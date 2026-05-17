from __future__ import annotations

import asyncio
from collections.abc import AsyncIterator, Iterator
from datetime import UTC, datetime
from pathlib import Path
from typing import NamedTuple

import numpy as np
import pytest
import sqlalchemy as sa
from fastapi.testclient import TestClient
from passlib.context import CryptContext
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine
from sqlalchemy.pool import StaticPool

from app.core.constants import NX, NY
from app.core.security import create_access_token, get_jwt_secret
from app.db.models import (
    AppUser,
    AuditLog,
    Base,
    EditSession,
    ForecastCase,
    ProductWindow,
)
from app.db.session import get_db
from app.main import app
from app.repositories.session_repository import SessionRepository
from app.services.session_service import EditSessionService
from app.storage.path_builder import PathBuilder

CASE_ID = "2026051608"
WINDOW_ID = f"{CASE_ID}_ACC24_000_024"
PENDING_WINDOW_ID = f"{CASE_ID}_ACC48_000_048"
GRID_BYTES_QPF = NY * NX * 4
GRID_BYTES_UINT8 = NY * NX

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")


class SessionApiClient(NamedTuple):
    client: TestClient
    session_factory: async_sessionmaker[AsyncSession]
    path_builder: PathBuilder


def _write_field_data(builder: PathBuilder, case_id: str, window_id: str) -> None:
    original_dir = builder.window_original_dir(case_id, window_id)
    original_dir.mkdir(parents=True, exist_ok=True)
    qpf = np.full((NY, NX), 1.25, dtype=np.float32)
    ptype = np.full((NY, NX), 2, dtype=np.uint8)
    invalid = np.zeros((NY, NX), dtype=np.uint8)
    invalid[0, 0] = 1
    np.savez(original_dir / "qpf_before.npz", qpf=qpf)
    np.savez(original_dir / "ptype_before.npz", ptype=ptype)
    np.savez(original_dir / "ptype_invalid_mask.npz", mask=invalid)


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
                    username="viewer",
                    password_hash=pwd_context.hash("viewer123"),
                    display_name="查看员",
                    role="viewer",
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
                    window_id=PENDING_WINDOW_ID,
                    case_id=CASE_ID,
                    accum_hours=48,
                    start_lead=0,
                    end_lead=48,
                    status="pending",
                    qc_status="unchecked",
                    negative_count=0,
                    missing_count=0,
                ),
            ]
        )
        await db.commit()


@pytest.fixture()
def session_api_client(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> Iterator[SessionApiClient]:
    builder = PathBuilder(
        base_dir=tmp_path / "archive", data_source_root=tmp_path / "source"
    )
    _write_field_data(builder, CASE_ID, WINDOW_ID)
    monkeypatch.setattr("app.services.session_service.default_path_builder", builder)
    monkeypatch.setattr(
        "app.services.session_service.session_service.path_builder", builder
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
        yield SessionApiClient(
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


async def _get_user(
    session_factory: async_sessionmaker[AsyncSession], user_id: int
) -> AppUser:
    async with session_factory() as db:
        user = await db.get(AppUser, user_id)
        assert user is not None
        return user


async def _create_session(
    session_factory: async_sessionmaker[AsyncSession],
    builder: PathBuilder,
    user_id: int = 1,
    window_id: str = WINDOW_ID,
) -> EditSession:
    service = EditSessionService(path_builder=builder)
    async with session_factory() as db:
        user = await db.get(AppUser, user_id)
        assert user is not None
        return await service.create_session(db, window_id, user)


def _assert_binary_headers(response, expected_dtype: str, expected_length: int) -> None:
    assert response.status_code == 200
    assert response.headers["x-grid-rows"] == str(NY)
    assert response.headers["x-grid-cols"] == str(NX)
    assert response.headers["x-grid-dtype"] == expected_dtype
    assert response.headers["x-grid-order"] == "C"
    assert response.headers["x-grid-byte-length"] == str(expected_length)
    assert len(response.content) == expected_length


def test_session_service_create_normal_and_audit_log(
    session_api_client: SessionApiClient,
) -> None:
    session = asyncio.run(
        _create_session(
            session_api_client.session_factory, session_api_client.path_builder
        )
    )

    async def inspect_db() -> tuple[int, str | None]:
        async with session_api_client.session_factory() as db:
            count = await db.scalar(
                select(sa.func.count(AuditLog.id)).where(
                    AuditLog.action == "session_start",
                    AuditLog.resource_id == session.session_id,
                    AuditLog.user_id == 1,
                )
            )
            stored = await db.get(EditSession, session.session_id)
            assert stored is not None
            return int(count or 0), stored.status

    audit_count, status = asyncio.run(inspect_db())
    assert status == "editing"
    assert audit_count == 1
    assert (
        session_api_client.path_builder.session_root(session.session_id)
        / "qpf_after.npy"
    ).exists()


def test_session_service_errors_and_idempotency(
    session_api_client: SessionApiClient,
) -> None:
    async def run_cases() -> dict[str, str]:
        service = EditSessionService(path_builder=session_api_client.path_builder)
        async with session_api_client.session_factory() as db:
            admin = await db.get(AppUser, 1)
            forecaster = await db.get(AppUser, 2)
            viewer = await db.get(AppUser, 3)
            assert admin is not None and forecaster is not None and viewer is not None

            errors: dict[str, str] = {}
            for key, args in {
                "missing": ("missing-window", admin),
                "not_editable": (PENDING_WINDOW_ID, admin),
                "permission": (WINDOW_ID, viewer),
            }.items():
                try:
                    await service.create_session(db, args[0], args[1])
                except Exception as exc:
                    errors[key] = getattr(exc, "code", "")

            first = await service.create_session(db, WINDOW_ID, admin)
            second = await service.create_session(db, WINDOW_ID, admin)
            assert first.session_id == second.session_id
            try:
                await service.create_session(db, WINDOW_ID, forecaster)
            except Exception as exc:
                errors["locked"] = getattr(exc, "code", "")
            return errors

    errors = asyncio.run(run_cases())
    assert errors == {
        "missing": "WINDOW_NOT_FOUND",
        "not_editable": "WINDOW_NOT_EDITABLE",
        "permission": "PERMISSION_DENIED",
        "locked": "WINDOW_LOCKED",
    }


def test_session_service_integrity_error_returns_window_locked(
    session_api_client: SessionApiClient,
) -> None:
    class RacingRepository(SessionRepository):
        def __init__(self) -> None:
            self.active_lookup_count = 0

        async def get_active_by_window(
            self, db: AsyncSession, window_id: str
        ) -> EditSession | None:
            self.active_lookup_count += 1
            if self.active_lookup_count == 1:
                return None
            return await super().get_active_by_window(db, window_id)

    async def run_case() -> dict[str, object]:
        async with session_api_client.session_factory() as db:
            db.add(
                EditSession(
                    session_id="00000000-0000-0000-0000-000000000029",
                    window_id=WINDOW_ID,
                    user_id=1,
                    status="editing",
                )
            )
            await db.commit()

        repo = RacingRepository()
        service = EditSessionService(
            sessions=repo,
            path_builder=session_api_client.path_builder,
        )
        async with session_api_client.session_factory() as db:
            forecaster = await db.get(AppUser, 2)
            assert forecaster is not None
            try:
                await service.create_session(db, WINDOW_ID, forecaster)
            except Exception as exc:
                return {
                    "code": getattr(exc, "code", ""),
                    "detail": getattr(exc, "detail", {}),
                    "lookups": repo.active_lookup_count,
                }
            return {"code": "", "detail": {}, "lookups": repo.active_lookup_count}

    result = asyncio.run(run_case())
    assert result == {
        "code": "WINDOW_LOCKED",
        "detail": {"window_id": WINDOW_ID},
        "lookups": 2,
    }


def test_session_service_with_mocked_repository(
    session_api_client: SessionApiClient,
) -> None:
    class RecordingRepository(SessionRepository):
        def __init__(self) -> None:
            self.created: list[dict[str, object]] = []

        async def create(self, db: AsyncSession, **kwargs: object) -> EditSession:
            self.created.append(kwargs)
            return await super().create(db, **kwargs)

    async def run_case() -> int:
        repo = RecordingRepository()
        service = EditSessionService(
            sessions=repo,
            path_builder=session_api_client.path_builder,
        )
        async with session_api_client.session_factory() as db:
            user = await db.get(AppUser, 1)
            assert user is not None
            await service.create_session(db, WINDOW_ID, user)
        return len(repo.created)

    assert asyncio.run(run_case()) == 1


def test_post_session_start_success_and_concurrent_lock(
    session_api_client: SessionApiClient,
) -> None:
    response = session_api_client.client.post(
        "/api/session/start",
        json={"window_id": WINDOW_ID},
        headers=_headers(),
    )
    assert response.status_code == 200
    session_id = response.json()["data"]["session_id"]

    lock_response = session_api_client.client.post(
        "/api/session/start",
        json={"window_id": WINDOW_ID},
        headers=_headers(2, "forecaster", "forecaster"),
    )
    assert lock_response.status_code == 409
    assert lock_response.json()["code"] == "WINDOW_LOCKED"
    assert lock_response.json()["detail"] == {"window_id": WINDOW_ID}
    assert session_id


def test_post_session_start_viewer_forbidden(
    session_api_client: SessionApiClient,
) -> None:
    response = session_api_client.client.post(
        "/api/session/start",
        json={"window_id": WINDOW_ID},
        headers=_headers(3, "viewer", "viewer"),
    )
    assert response.status_code == 403
    assert response.json()["code"] == "PERMISSION_DENIED"


def test_load_session_response(session_api_client: SessionApiClient) -> None:
    session = asyncio.run(
        _create_session(
            session_api_client.session_factory, session_api_client.path_builder
        )
    )
    response = session_api_client.client.get(
        f"/api/session/{session.session_id}/load",
        headers=_headers(),
    )
    assert response.status_code == 200
    data = response.json()["data"]
    assert data["session_id"] == session.session_id
    assert data["grid_rows"] == NY
    assert data["grid_cols"] == NX
    assert data["operation_count"] == 0
    assert data["can_undo"] is False
    assert data["can_redo"] is False
    assert set(data["field_urls"]) == {
        "qpf_before",
        "qpf_after",
        "ptype_before",
        "ptype_after",
        "touched_mask",
        "changed_mask",
        "invalid_mask",
    }


@pytest.mark.parametrize(
    ("field", "dtype", "length"),
    [
        ("qpf_before", "float32", GRID_BYTES_QPF),
        ("qpf_after", "float32", GRID_BYTES_QPF),
        ("ptype_before", "uint8", GRID_BYTES_UINT8),
        ("ptype_after", "uint8", GRID_BYTES_UINT8),
        ("touched_mask", "uint8", GRID_BYTES_UINT8),
        ("changed_mask", "uint8", GRID_BYTES_UINT8),
        ("invalid_mask", "uint8", GRID_BYTES_UINT8),
    ],
)
def test_get_session_field_all_fields(
    session_api_client: SessionApiClient, field: str, dtype: str, length: int
) -> None:
    session = asyncio.run(
        _create_session(
            session_api_client.session_factory, session_api_client.path_builder
        )
    )
    response = session_api_client.client.get(
        f"/api/session/{session.session_id}/field/{field}",
        headers=_headers(),
    )
    _assert_binary_headers(response, dtype, length)
    assert response.headers["x-grid-variable"] == field


def test_gzip_content_encoding(session_api_client: SessionApiClient) -> None:
    session = asyncio.run(
        _create_session(
            session_api_client.session_factory, session_api_client.path_builder
        )
    )
    response = session_api_client.client.get(
        f"/api/session/{session.session_id}/field/qpf_before",
        headers={**_headers(), "Accept-Encoding": "gzip"},
    )
    assert response.status_code == 200
    assert response.headers["content-encoding"] == "gzip"
    assert response.headers["x-grid-byte-length"] == str(GRID_BYTES_QPF)


@pytest.mark.parametrize(
    ("field", "dtype", "length"),
    [
        ("qpf_before", "float32", GRID_BYTES_QPF),
        ("ptype_before", "uint8", GRID_BYTES_UINT8),
        ("invalid_mask", "uint8", GRID_BYTES_UINT8),
    ],
)
def test_get_window_field_all_fields(
    session_api_client: SessionApiClient, field: str, dtype: str, length: int
) -> None:
    response = session_api_client.client.get(
        f"/api/window/{WINDOW_ID}/field/{field}",
        headers=_headers(),
    )
    _assert_binary_headers(response, dtype, length)


def test_window_field_error_scenarios(session_api_client: SessionApiClient) -> None:
    missing = session_api_client.client.get(
        "/api/window/missing/field/qpf_before",
        headers=_headers(),
    )
    assert missing.status_code == 404
    assert missing.json()["code"] == "WINDOW_NOT_FOUND"

    not_editable = session_api_client.client.get(
        f"/api/window/{PENDING_WINDOW_ID}/field/qpf_before",
        headers=_headers(),
    )
    assert not_editable.status_code == 409
    assert not_editable.json()["code"] == "WINDOW_NOT_EDITABLE"

    invalid_field = session_api_client.client.get(
        f"/api/window/{WINDOW_ID}/field/qpf_after",
        headers=_headers(),
    )
    assert invalid_field.status_code == 422
    assert invalid_field.json()["code"] == "VALIDATION_ERROR"


def test_session_expired_returns_410(session_api_client: SessionApiClient) -> None:
    session = asyncio.run(
        _create_session(
            session_api_client.session_factory, session_api_client.path_builder
        )
    )

    async def expire() -> None:
        async with session_api_client.session_factory() as db:
            stored = await db.get(EditSession, session.session_id)
            assert stored is not None
            stored.status = "expired"
            await db.commit()

    asyncio.run(expire())
    response = session_api_client.client.get(
        f"/api/session/{session.session_id}/load",
        headers=_headers(),
    )
    assert response.status_code == 410
    assert response.json()["code"] == "SESSION_EXPIRED"


def test_expired_session_field_returns_410(
    session_api_client: SessionApiClient,
) -> None:
    session = asyncio.run(
        _create_session(
            session_api_client.session_factory, session_api_client.path_builder
        )
    )

    async def expire() -> None:
        async with session_api_client.session_factory() as db:
            stored = await db.get(EditSession, session.session_id)
            assert stored is not None
            stored.status = "expired"
            await db.commit()

    asyncio.run(expire())
    response = session_api_client.client.get(
        f"/api/session/{session.session_id}/field/qpf_before",
        headers=_headers(),
    )
    assert response.status_code == 410
    assert response.json()["code"] == "SESSION_EXPIRED"


def test_field_not_available_returns_404(session_api_client: SessionApiClient) -> None:
    session = asyncio.run(
        _create_session(
            session_api_client.session_factory, session_api_client.path_builder
        )
    )
    missing_file = (
        session_api_client.path_builder.session_root(session.session_id)
        / "changed_mask.npy"
    )
    missing_file.unlink()

    response = session_api_client.client.get(
        f"/api/session/{session.session_id}/field/changed_mask",
        headers=_headers(),
    )
    assert response.status_code == 404
    assert response.json()["code"] == "FIELD_NOT_AVAILABLE"


def test_edit_session_model_can_be_created(
    session_api_client: SessionApiClient,
) -> None:
    async def create_directly() -> str | None:
        async with session_api_client.session_factory() as db:
            db.add(
                EditSession(
                    session_id="00000000-0000-0000-0000-000000000024",
                    window_id=WINDOW_ID,
                    user_id=1,
                    status="editing",
                )
            )
            await db.commit()
            stored = await db.get(EditSession, "00000000-0000-0000-0000-000000000024")
            return None if stored is None else stored.status

    assert asyncio.run(create_directly()) == "editing"
