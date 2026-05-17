from __future__ import annotations

import asyncio
from collections.abc import AsyncIterator, Iterator
from datetime import UTC, datetime
from typing import NamedTuple

import pytest
from fastapi.testclient import TestClient
from passlib.context import CryptContext
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine
from sqlalchemy.pool import StaticPool

from app.api.dependencies import get_session_factory
from app.db.models import AppUser, Base, DataScanLog, ForecastCase
from app.db.session import get_db
from app.main import app

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")
CASE_ID = "2026051608"


class ApiClient(NamedTuple):
    client: TestClient
    session_factory: async_sessionmaker[AsyncSession]


async def _seed_users(session_factory: async_sessionmaker[AsyncSession]) -> None:
    async with session_factory() as db:
        db.add_all(
            [
                AppUser(
                    username="admin",
                    password_hash=pwd_context.hash("admin123"),
                    display_name="系统管理员",
                    role="admin",
                    is_active=True,
                ),
                AppUser(
                    username="reviewer",
                    password_hash=pwd_context.hash("reviewer123"),
                    display_name="审核员",
                    role="reviewer",
                    is_active=True,
                ),
                AppUser(
                    username="forecaster",
                    password_hash=pwd_context.hash("forecaster123"),
                    display_name="预报员",
                    role="forecaster",
                    is_active=True,
                ),
                AppUser(
                    username="viewer",
                    password_hash=pwd_context.hash("viewer123"),
                    display_name="查看员",
                    role="viewer",
                    is_active=True,
                ),
            ]
        )
        await db.commit()


@pytest.fixture()
def api_client(monkeypatch: pytest.MonkeyPatch) -> Iterator[ApiClient]:
    engine = create_async_engine(
        "sqlite+aiosqlite://",
        connect_args={"check_same_thread": False},
        poolclass=StaticPool,
    )
    session_factory = async_sessionmaker(
        engine, class_=AsyncSession, expire_on_commit=False
    )

    async def setup_database() -> None:
        async with engine.begin() as connection:
            await connection.run_sync(Base.metadata.create_all)
        await _seed_users(session_factory)

    asyncio.run(setup_database())

    async def override_get_db() -> AsyncIterator[AsyncSession]:
        async with session_factory() as session:
            yield session

    def override_session_factory() -> async_sessionmaker[AsyncSession]:
        return session_factory

    async def noop_scan(**_: object) -> None:
        return None

    app.dependency_overrides[get_db] = override_get_db
    app.dependency_overrides[get_session_factory] = override_session_factory
    monkeypatch.setattr("app.api.routes.data_scan.run_data_scan", noop_scan)

    with TestClient(app, raise_server_exceptions=False) as client:
        yield ApiClient(client=client, session_factory=session_factory)

    app.dependency_overrides.pop(get_db, None)
    app.dependency_overrides.pop(get_session_factory, None)
    asyncio.run(engine.dispose())


def _login(client: TestClient, username: str, password: str) -> str:
    response = client.post(
        "/api/auth/login", json={"username": username, "password": password}
    )
    assert response.status_code == 200
    return str(response.json()["data"]["token"])


def _bearer(token: str) -> dict[str, str]:
    return {"Authorization": f"Bearer {token}"}


async def _create_running_scan(
    session_factory: async_sessionmaker[AsyncSession],
    scan_id: str = "running-scan",
) -> None:
    async with session_factory() as db:
        db.add(
            ForecastCase(
                case_id=CASE_ID,
                init_time=datetime(2026, 5, 16, 8, tzinfo=UTC),
                data_source_path="/tmp/source/2026051608",
                scan_count=0,
                status="pending",
            )
        )
        db.add(
            DataScanLog(
                scan_id=scan_id,
                case_id=CASE_ID,
                status="running",
                scan_started_at=datetime(2026, 5, 16, 9, tzinfo=UTC),
            )
        )
        await db.commit()


def test_post_scan_success(api_client: ApiClient) -> None:
    token = _login(api_client.client, "admin", "admin123")

    response = api_client.client.post(
        "/api/data/scan",
        json={"case_id": CASE_ID},
        headers=_bearer(token),
    )

    body = response.json()
    assert response.status_code == 200
    assert body["code"] == "OK"
    assert body["message"] == "扫描已启动"
    assert body["data"]["status"] == "running"
    assert body["data"]["scan_id"]
    assert body["trace_id"]


def test_invalid_case_id(api_client: ApiClient) -> None:
    token = _login(api_client.client, "admin", "admin123")

    response = api_client.client.post(
        "/api/data/scan",
        json={"case_id": "bad"},
        headers=_bearer(token),
    )

    assert response.status_code == 422
    assert response.json()["code"] == "INVALID_CASE_ID"


def test_invalid_init_hour(api_client: ApiClient) -> None:
    token = _login(api_client.client, "admin", "admin123")

    response = api_client.client.post(
        "/api/data/scan",
        json={"case_id": "2026051610"},
        headers=_bearer(token),
    )

    assert response.status_code == 422
    assert response.json()["code"] == "INVALID_CASE_ID"


def test_viewer_denied(api_client: ApiClient) -> None:
    token = _login(api_client.client, "viewer", "viewer123")

    response = api_client.client.post(
        "/api/data/scan",
        json={"case_id": CASE_ID},
        headers=_bearer(token),
    )

    assert response.status_code == 403
    assert response.json()["code"] == "PERMISSION_DENIED"


def test_forecaster_denied(api_client: ApiClient) -> None:
    token = _login(api_client.client, "forecaster", "forecaster123")

    response = api_client.client.post(
        "/api/data/scan",
        json={"case_id": CASE_ID},
        headers=_bearer(token),
    )

    assert response.status_code == 403
    assert response.json()["code"] == "PERMISSION_DENIED"


def test_scan_already_running(api_client: ApiClient) -> None:
    asyncio.run(_create_running_scan(api_client.session_factory))
    token = _login(api_client.client, "reviewer", "reviewer123")

    response = api_client.client.post(
        "/api/data/scan",
        json={"case_id": CASE_ID},
        headers=_bearer(token),
    )

    assert response.status_code == 409
    assert response.json()["code"] == "SCAN_ALREADY_RUNNING"


def test_get_status_by_scan_id(api_client: ApiClient) -> None:
    asyncio.run(_create_running_scan(api_client.session_factory))
    token = _login(api_client.client, "viewer", "viewer123")

    response = api_client.client.get(
        "/api/data/status",
        params={"scan_id": "running-scan"},
        headers=_bearer(token),
    )

    body = response.json()
    assert response.status_code == 200
    assert body["code"] == "OK"
    assert body["data"]["scan_id"] == "running-scan"
    assert body["data"]["case_id"] == CASE_ID
    assert body["data"]["status"] == "running"


def test_get_status_neither_param(api_client: ApiClient) -> None:
    token = _login(api_client.client, "viewer", "viewer123")

    response = api_client.client.get(
        "/api/data/status",
        headers=_bearer(token),
    )

    assert response.status_code == 422
    assert response.json()["code"] == "VALIDATION_ERROR"


def test_post_scan_without_token_returns_401(api_client: ApiClient) -> None:
    response = api_client.client.post(
        "/api/data/scan",
        json={"case_id": CASE_ID},
    )

    assert response.status_code == 401
    assert response.json()["code"] == "AUTH_REQUIRED"


async def test_post_scan_persists_running_log(api_client: ApiClient) -> None:
    token = _login(api_client.client, "admin", "admin123")

    response = api_client.client.post(
        "/api/data/scan",
        json={"case_id": CASE_ID},
        headers=_bearer(token),
    )
    scan_id = response.json()["data"]["scan_id"]

    async with api_client.session_factory() as db:
        result = await db.execute(
            select(DataScanLog).where(DataScanLog.scan_id == scan_id)
        )
        scan_log = result.scalar_one()

    assert scan_log.case_id == CASE_ID
    assert scan_log.status == "running"
