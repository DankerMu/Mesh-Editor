from __future__ import annotations

import asyncio
from collections.abc import AsyncIterator, Iterator
from datetime import UTC, datetime
from pathlib import Path
from typing import NamedTuple

import numpy as np
import pytest
from fastapi.testclient import TestClient
from passlib.context import CryptContext
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine
from sqlalchemy.pool import StaticPool

from app.core.constants import NX, NY
from app.core.security import create_access_token, get_jwt_secret
from app.db.models import AppUser, Base, EditSession, ForecastCase, ProductWindow
from app.db.session import get_db
from app.main import app
from app.repositories.edit_version_repo import edit_version_repo
from app.storage.path_builder import PathBuilder

CASE_ID = "2026051608"
WINDOW_ID = f"{CASE_ID}_ACC24_000_024"
OTHER_WINDOW_ID = f"{CASE_ID}_ACC48_000_048"
SESSION_ID = "00000000-0000-0000-0000-000000000100"
FORECASTER_SESSION_ID = "00000000-0000-0000-0000-000000000200"
GRID_BYTES_QPF = NY * NX * 4
GRID_BYTES_UINT8 = NY * NX

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")


class VersionApiClient(NamedTuple):
    client: TestClient
    session_factory: async_sessionmaker[AsyncSession]
    path_builder: PathBuilder


@pytest.fixture()
def version_api_client(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> Iterator[VersionApiClient]:
    builder = PathBuilder(
        base_dir=tmp_path / "archive", data_source_root=tmp_path / "source"
    )
    _write_original_fields(builder, WINDOW_ID)
    _write_original_fields(builder, OTHER_WINDOW_ID)
    _write_session_fields(builder, SESSION_ID)
    _write_session_fields(builder, FORECASTER_SESSION_ID)

    for service_path in [
        "app.services.version_service.version_service.path_builder",
        "app.services.release_service.release_service.path_builder",
        "app.api.routes.versions.version_service.path_builder",
        "app.api.routes.versions.release_service.path_builder",
    ]:
        monkeypatch.setattr(service_path, builder)

    monkeypatch.setattr(
        "app.services.version_service.version_service.precip_plotter",
        lambda *_args, **_kwargs: b"\x89PNG\r\n\x1a\nprecip",
    )
    monkeypatch.setattr(
        "app.services.version_service.version_service.delta_qpf_plotter",
        lambda *_args, **_kwargs: b"\x89PNG\r\n\x1a\ndelta",
    )
    monkeypatch.setattr(
        "app.services.version_service.version_service.change_ptype_plotter",
        lambda *_args, **_kwargs: b"\x89PNG\r\n\x1a\nptype",
    )
    monkeypatch.setattr(
        "app.services.version_service.version_service.mask_plotter",
        lambda *_args, **_kwargs: b"\x89PNG\r\n\x1a\nmask",
    )

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
        yield VersionApiClient(
            client=client, session_factory=session_factory, path_builder=builder
        )

    app.dependency_overrides.pop(get_db, None)
    asyncio.run(engine.dispose())


def _headers(
    user_id: int = 1, username: str = "admin", role: str = "admin"
) -> dict[str, str]:
    token, _ = create_access_token(
        user_id=user_id,
        username=username,
        role=role,
        secret=get_jwt_secret(),
    )
    return {"Authorization": f"Bearer {token}"}


def _write_original_fields(builder: PathBuilder, window_id: str) -> None:
    original_dir = builder.window_original_dir(CASE_ID, window_id)
    original_dir.mkdir(parents=True, exist_ok=True)
    qpf = np.zeros((NY, NX), dtype=np.float32)
    ptype = np.zeros((NY, NX), dtype=np.uint8)
    np.savez_compressed(original_dir / "qpf_before.npz", qpf=qpf)
    np.savez_compressed(original_dir / "ptype_before.npz", ptype=ptype)


def _write_session_fields(builder: PathBuilder, session_id: str) -> None:
    session_dir = builder.session_root(session_id)
    session_dir.mkdir(parents=True, exist_ok=True)
    qpf_before = np.zeros((NY, NX), dtype=np.float32)
    qpf_after = qpf_before.copy()
    qpf_after[0, 0] = 1.25
    ptype_before = np.zeros((NY, NX), dtype=np.uint8)
    ptype_after = ptype_before.copy()
    ptype_after[0, 0] = 1
    touched_mask = np.zeros((NY, NX), dtype=np.uint8)
    touched_mask[0, 0] = 1
    for name, data in [
        ("qpf_before", qpf_before),
        ("qpf_after", qpf_after),
        ("ptype_before", ptype_before),
        ("ptype_after", ptype_after),
        ("touched_mask", touched_mask),
    ]:
        np.savez_compressed(session_dir / f"{name}.npz", data=data)


async def _seed_database(
    session_factory: async_sessionmaker[AsyncSession],
) -> None:
    async with session_factory() as db:
        db.add_all(
            [
                AppUser(
                    id=1,
                    username="admin",
                    password_hash=pwd_context.hash("admin123"),
                    display_name="管理员",
                    role="admin",
                    is_active=True,
                ),
                AppUser(
                    id=2,
                    username="forecaster",
                    password_hash=pwd_context.hash("forecaster123"),
                    display_name="预报员",
                    role="forecaster",
                    is_active=True,
                ),
                AppUser(
                    id=3,
                    username="reviewer",
                    password_hash=pwd_context.hash("reviewer123"),
                    display_name="审核员",
                    role="reviewer",
                    is_active=True,
                ),
            ]
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
                    window_id=WINDOW_ID,
                    case_id=CASE_ID,
                    accum_hours=24,
                    start_lead=0,
                    end_lead=24,
                    status="available",
                    qc_status="pass",
                    negative_count=0,
                    missing_count=0,
                    ptype_missing_leads=[],
                    data_ready_at=datetime(2026, 5, 16, 9, tzinfo=UTC),
                ),
                ProductWindow(
                    window_id=OTHER_WINDOW_ID,
                    case_id=CASE_ID,
                    accum_hours=48,
                    start_lead=0,
                    end_lead=48,
                    status="available",
                    qc_status="pass",
                    negative_count=0,
                    missing_count=0,
                    ptype_missing_leads=[],
                    data_ready_at=datetime(2026, 5, 16, 9, tzinfo=UTC),
                ),
            ]
        )
        db.add_all(
            [
                EditSession(
                    session_id=SESSION_ID,
                    window_id=WINDOW_ID,
                    user_id=1,
                    base_version_id=None,
                    status="editing",
                ),
                EditSession(
                    session_id=FORECASTER_SESSION_ID,
                    window_id=OTHER_WINDOW_ID,
                    user_id=2,
                    base_version_id=None,
                    status="editing",
                ),
            ]
        )
        await db.commit()


def _save(client: TestClient, session_id: str, headers: dict[str, str]) -> str:
    response = client.post(
        "/api/version/save",
        json={"session_id": session_id, "generate_review": True},
        headers=headers,
    )
    assert response.status_code == 200, response.text
    return str(response.json()["data"]["version_id"])


def _submit(client: TestClient, version_id: str, headers: dict[str, str]) -> None:
    response = client.post(
        "/api/version/submit",
        json={"version_id": version_id},
        headers=headers,
    )
    assert response.status_code == 200, response.text
    assert response.json()["data"]["status"] == "submitted"


def _approve(client: TestClient, version_id: str) -> None:
    response = client.post(
        "/api/version/review",
        json={"version_id": version_id, "action": "approve", "comment": "通过"},
        headers=_headers(3, "reviewer", "reviewer"),
    )
    assert response.status_code == 200, response.text
    assert response.json()["data"]["version_status"] == "approved"


def test_save_submit_review_release_full_cycle(
    version_api_client: VersionApiClient,
) -> None:
    version_id = _save(version_api_client.client, SESSION_ID, _headers())
    _submit(version_api_client.client, version_id, _headers())
    _approve(version_api_client.client, version_id)

    release = version_api_client.client.post(
        "/api/version/release",
        json={"version_id": version_id},
        headers=_headers(3, "reviewer", "reviewer"),
    )
    assert release.status_code == 200, release.text
    assert release.json()["data"]["release_status"] == "active"

    detail = version_api_client.client.get(
        f"/api/versions/{version_id}", headers=_headers()
    )
    assert detail.status_code == 200, detail.text
    data = detail.json()["data"]
    assert data["status"] == "released"
    assert data["operation_summary"] == {
        "operation_count": 0,
        "affected_grid_count": 0,
    }
    assert data["approval_history"][0]["action"] == "approve"
    assert set(data["field_urls"]) == {
        "qpf_before",
        "ptype_before",
        "qpf_after",
        "ptype_after",
        "delta_qpf",
        "change_ptype",
        "touched_mask",
        "changed_mask",
    }


def test_save_requires_auth(version_api_client: VersionApiClient) -> None:
    response = version_api_client.client.post(
        "/api/version/save",
        json={"session_id": SESSION_ID, "generate_review": True},
    )
    assert response.status_code == 401
    assert response.json()["code"] == "AUTH_REQUIRED"


def test_review_requires_reviewer_role(version_api_client: VersionApiClient) -> None:
    response = version_api_client.client.post(
        "/api/version/review",
        json={"version_id": "missing", "action": "approve", "comment": "通过"},
        headers=_headers(2, "forecaster", "forecaster"),
    )
    assert response.status_code == 403
    assert response.json()["code"] == "PERMISSION_DENIED"


def test_release_requires_reviewer_role(version_api_client: VersionApiClient) -> None:
    response = version_api_client.client.post(
        "/api/version/release",
        json={"version_id": "missing"},
        headers=_headers(2, "forecaster", "forecaster"),
    )
    assert response.status_code == 403
    assert response.json()["code"] == "PERMISSION_DENIED"


def test_list_versions_forecaster_scoped(
    version_api_client: VersionApiClient,
) -> None:
    admin_version = _save(version_api_client.client, SESSION_ID, _headers())
    forecaster_version = _save(
        version_api_client.client,
        FORECASTER_SESSION_ID,
        _headers(2, "forecaster", "forecaster"),
    )

    response = version_api_client.client.get(
        "/api/versions",
        headers=_headers(2, "forecaster", "forecaster"),
    )
    assert response.status_code == 200, response.text
    version_ids = {item["version_id"] for item in response.json()["data"]}
    assert forecaster_version in version_ids
    assert admin_version not in version_ids


def test_version_not_found(version_api_client: VersionApiClient) -> None:
    response = version_api_client.client.get("/api/versions/missing", headers=_headers())
    assert response.status_code == 404
    assert response.json()["code"] == "VERSION_NOT_FOUND"


def test_version_field_binary(version_api_client: VersionApiClient) -> None:
    version_id = _save(version_api_client.client, SESSION_ID, _headers())

    qpf_response = version_api_client.client.get(
        f"/api/version/{version_id}/field/qpf_after",
        headers=_headers(),
    )
    assert qpf_response.status_code == 200
    assert qpf_response.headers["x-grid-rows"] == str(NY)
    assert qpf_response.headers["x-grid-cols"] == str(NX)
    assert qpf_response.headers["x-grid-dtype"] == "float32"
    assert qpf_response.headers["x-grid-byte-length"] == str(GRID_BYTES_QPF)
    assert len(qpf_response.content) == GRID_BYTES_QPF

    ptype_response = version_api_client.client.get(
        f"/api/version/{version_id}/field/ptype_before",
        headers=_headers(),
    )
    assert ptype_response.status_code == 200
    assert ptype_response.headers["x-grid-dtype"] == "uint8"
    assert ptype_response.headers["x-grid-byte-length"] == str(GRID_BYTES_UINT8)
    assert len(ptype_response.content) == GRID_BYTES_UINT8


def test_detail_operation_summary_from_operations_jsonl(
    version_api_client: VersionApiClient,
) -> None:
    async def seed_version() -> str:
        async with version_api_client.session_factory() as db:
            version_id = f"{WINDOW_ID}_v099"
            version_dir = version_api_client.path_builder.version_root(
                WINDOW_ID, version_id
            )
            version_dir.mkdir(parents=True, exist_ok=True)
            for name, data in [
                ("qpf_after", np.zeros((NY, NX), dtype=np.float32)),
                ("ptype_after", np.zeros((NY, NX), dtype=np.uint8)),
                ("delta_qpf", np.zeros((NY, NX), dtype=np.float32)),
                ("change_ptype", np.zeros((NY, NX), dtype=np.int8)),
                ("touched_mask", np.zeros((NY, NX), dtype=np.uint8)),
                ("changed_mask", np.zeros((NY, NX), dtype=np.uint8)),
            ]:
                np.savez_compressed(version_dir / f"{name}.npz", data=data)
            (version_dir / "operations.jsonl").write_text(
                '{"after_stats_json":"{\\"count\\": 3}"}\n'
                '{"after_stats_json":"{\\"count\\": 5}"}\n',
                encoding="utf-8",
            )
            await edit_version_repo.create(
                db,
                version_id=version_id,
                window_id=WINDOW_ID,
                version_no=99,
                base_version_id=None,
                session_id=SESSION_ID,
                status="draft",
                created_by="1",
                qpf_after_path=str(version_dir / "qpf_after.npz"),
                ptype_after_path=str(version_dir / "ptype_after.npz"),
                delta_qpf_path=str(version_dir / "delta_qpf.npz"),
                change_ptype_path=str(version_dir / "change_ptype.npz"),
                touched_mask_path=str(version_dir / "touched_mask.npz"),
                changed_mask_path=str(version_dir / "changed_mask.npz"),
            )
            await db.commit()
            return version_id

    version_id = asyncio.run(seed_version())
    response = version_api_client.client.get(
        f"/api/versions/{version_id}", headers=_headers()
    )
    assert response.status_code == 200, response.text
    assert response.json()["data"]["operation_summary"] == {
        "operation_count": 2,
        "affected_grid_count": 8,
    }
