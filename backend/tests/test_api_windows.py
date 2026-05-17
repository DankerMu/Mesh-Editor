from __future__ import annotations

import asyncio
from collections.abc import AsyncIterator, Iterator
from datetime import UTC, datetime
from typing import NamedTuple

import pytest
from fastapi.testclient import TestClient
from passlib.context import CryptContext
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine
from sqlalchemy.pool import StaticPool

from app.db.models import AppUser, Base, ForecastCase, ProductWindow
from app.db.session import get_db
from app.main import app

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")
CASE_ID = "2026051608"


class ApiClient(NamedTuple):
    client: TestClient
    session_factory: async_sessionmaker[AsyncSession]


async def _seed_database(session_factory: async_sessionmaker[AsyncSession]) -> None:
    async with session_factory() as db:
        db.add(
            AppUser(
                username="viewer",
                password_hash=pwd_context.hash("viewer123"),
                display_name="查看员",
                role="viewer",
                is_active=True,
            )
        )
        db.add(
            ForecastCase(
                case_id=CASE_ID,
                init_time=datetime(2026, 5, 16, 8, tzinfo=UTC),
                data_source_path="/tmp/source/2026051608",
                scan_count=1,
                status="complete",
            )
        )
        db.add_all(
            [
                ProductWindow(
                    window_id=f"{CASE_ID}_ACC24_000_024",
                    case_id=CASE_ID,
                    accum_hours=24,
                    start_lead=0,
                    end_lead=24,
                    status="available",
                    qc_status="pass",
                    negative_count=0,
                    missing_count=0,
                    ptype_missing_leads=[],
                    qpf_before_path="/archive/qpf.npz",
                    ptype_before_path="/archive/ptype.npz",
                    data_ready_at=datetime(2026, 5, 16, 9, tzinfo=UTC),
                ),
                ProductWindow(
                    window_id=f"{CASE_ID}_ACC48_000_048",
                    case_id=CASE_ID,
                    accum_hours=48,
                    start_lead=0,
                    end_lead=48,
                    status="partial",
                    qc_status="warn",
                    negative_count=1,
                    negative_min_value=-0.2,
                    negative_abs_max=0.2,
                    missing_count=1,
                    ptype_missing_leads=[24],
                    data_ready_at=datetime(2026, 5, 16, 10, tzinfo=UTC),
                ),
                ProductWindow(
                    window_id=f"{CASE_ID}_ACC168_000_168",
                    case_id=CASE_ID,
                    accum_hours=168,
                    start_lead=0,
                    end_lead=168,
                    status="available",
                    qc_status="pass",
                    negative_count=0,
                    missing_count=0,
                ),
            ]
        )
        await db.commit()


@pytest.fixture()
def api_client() -> Iterator[ApiClient]:
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
        await _seed_database(session_factory)

    asyncio.run(setup_database())

    async def override_get_db() -> AsyncIterator[AsyncSession]:
        async with session_factory() as session:
            yield session

    app.dependency_overrides[get_db] = override_get_db

    with TestClient(app, raise_server_exceptions=False) as client:
        yield ApiClient(client=client, session_factory=session_factory)

    app.dependency_overrides.pop(get_db, None)
    asyncio.run(engine.dispose())


def _token(client: TestClient) -> str:
    response = client.post(
        "/api/auth/login", json={"username": "viewer", "password": "viewer123"}
    )
    assert response.status_code == 200
    return str(response.json()["data"]["token"])


def _headers(client: TestClient) -> dict[str, str]:
    return {"Authorization": f"Bearer {_token(client)}"}


def test_list_windows(api_client: ApiClient) -> None:
    response = api_client.client.get(
        "/api/windows",
        params={"case_id": CASE_ID},
        headers=_headers(api_client.client),
    )

    body = response.json()
    assert response.status_code == 200
    assert len(body["data"]) == 3
    assert body["data"][0]["window_id"] == f"{CASE_ID}_ACC24_000_024"


def test_filter_accum(api_client: ApiClient) -> None:
    response = api_client.client.get(
        "/api/windows",
        params={"case_id": CASE_ID, "accum_hours": 24},
        headers=_headers(api_client.client),
    )

    assert response.status_code == 200
    assert [item["accum_hours"] for item in response.json()["data"]] == [24]


def test_filter_invalid_accum(api_client: ApiClient) -> None:
    response = api_client.client.get(
        "/api/windows",
        params={"case_id": CASE_ID, "accum_hours": 12},
        headers=_headers(api_client.client),
    )

    assert response.status_code == 422
    assert response.json()["code"] == "VALIDATION_ERROR"


def test_filter_status(api_client: ApiClient) -> None:
    response = api_client.client.get(
        "/api/windows",
        params={"case_id": CASE_ID, "status": "available"},
        headers=_headers(api_client.client),
    )

    assert response.status_code == 200
    assert {item["status"] for item in response.json()["data"]} == {"available"}
    assert len(response.json()["data"]) == 2


def test_missing_case_id(api_client: ApiClient) -> None:
    response = api_client.client.get(
        "/api/windows",
        headers=_headers(api_client.client),
    )

    assert response.status_code == 422
    assert response.json()["code"] == "VALIDATION_ERROR"


def test_empty_result(api_client: ApiClient) -> None:
    response = api_client.client.get(
        "/api/windows",
        params={"case_id": "2026051620"},
        headers=_headers(api_client.client),
    )

    assert response.status_code == 200
    assert response.json()["data"] == []


def test_windows_without_token_returns_401(api_client: ApiClient) -> None:
    response = api_client.client.get(
        "/api/windows",
        params={"case_id": CASE_ID},
    )

    assert response.status_code == 401
    assert response.json()["code"] == "AUTH_REQUIRED"
