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

from app.core.constants import NX, NY, REPO_ROOT
from app.core.security import create_access_token, get_jwt_secret
from app.db.models import AppUser, Base, EditSession, ForecastCase, ProductWindow
from app.db.session import get_db
from app.main import app
from app.services.edit_engine import preview_cache
from app.storage.path_builder import PathBuilder

CASE_ID = "2026051608"
WINDOW_ID = f"{CASE_ID}_ACC24_000_024"

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")


class EditApiClient(NamedTuple):
    client: TestClient
    session_factory: async_sessionmaker[AsyncSession]
    path_builder: PathBuilder


def _write_field_data(builder: PathBuilder) -> None:
    original_dir = builder.window_original_dir(CASE_ID, WINDOW_ID)
    original_dir.mkdir(parents=True, exist_ok=True)
    qpf = np.full((NY, NX), 1.0, dtype=np.float32)
    qpf[100:105, 100:105] = 0.0
    ptype = np.ones((NY, NX), dtype=np.uint8)
    ptype[qpf <= 0.1] = 0
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
        db.add(
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
            )
        )
        await db.commit()


@pytest.fixture(autouse=True)
def clear_previews() -> Iterator[None]:
    preview_cache._previews.clear()
    yield
    preview_cache._previews.clear()


@pytest.fixture()
def edit_api_client(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> Iterator[EditApiClient]:
    builder = PathBuilder(
        base_dir=tmp_path / "archive", data_source_root=tmp_path / "source"
    )
    _write_field_data(builder)
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
        yield EditApiClient(
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


def _start_session(client: TestClient) -> str:
    response = client.post(
        "/api/session/start", json={"window_id": WINDOW_ID}, headers=_headers()
    )
    assert response.status_code == 200
    return str(response.json()["data"]["session_id"])


def _polygon_payload(
    session_id: str,
    *,
    operation: str = "increase",
    variable: str = "qpf",
    delta_mm: float | None = 2.0,
    value: float | None = None,
    coordinates: list[list[float]] | None = None,
) -> dict[str, object]:
    parameters: dict[str, object] = {}
    if delta_mm is not None:
        parameters["delta_mm"] = delta_mm
    if value is not None:
        parameters["value"] = value
    return {
        "session_id": session_id,
        "tool": "polygon",
        "variable": variable,
        "operation": operation,
        "mask": {
            "coordinates": coordinates
            or [[80.0, 35.0], [80.2, 35.0], [80.2, 35.2], [80.0, 35.2]],
        },
        "parameters": parameters,
    }


def _preview(client: TestClient, session_id: str, payload: dict[str, object]) -> dict:
    response = client.post("/api/edit/preview", json=payload, headers=_headers())
    assert response.status_code == 200, response.text
    return dict(response.json()["data"])


def _apply(
    client: TestClient, session_id: str, preview_id: str, target_ptype: int | None = None
) -> dict:
    payload: dict[str, object] = {"session_id": session_id, "preview_id": preview_id}
    if target_ptype is not None:
        payload["target_ptype"] = target_ptype
    response = client.post("/api/edit/apply", json=payload, headers=_headers())
    assert response.status_code == 200, response.text
    return dict(response.json()["data"])


def _qpf(builder: PathBuilder, session_id: str) -> np.ndarray:
    return np.load(builder.session_root(session_id) / "qpf_after.npy")


def _ptype(builder: PathBuilder, session_id: str) -> np.ndarray:
    return np.load(builder.session_root(session_id) / "ptype_after.npy")


def test_preview_valid_polygon_qpf_increase_returns_stats(
    edit_api_client: EditApiClient,
) -> None:
    session_id = _start_session(edit_api_client.client)

    data = _preview(
        edit_api_client.client, session_id, _polygon_payload(session_id)
    )

    assert data["preview_id"]
    assert data["affected_grid_count"] > 0
    assert data["affected_area_km2"] > 0
    assert data["before_stats"]["count"] == data["affected_grid_count"]
    assert data["after_stats"]["mean"] > data["before_stats"]["mean"]
    assert data["op_ptype_transition"]["1_to_1"] > 0


def test_preview_valid_lasso_qpf_increase_returns_stats(
    edit_api_client: EditApiClient,
) -> None:
    session_id = _start_session(edit_api_client.client)
    payload = {
        "session_id": session_id,
        "tool": "lasso",
        "variable": "qpf",
        "operation": "increase",
        "mask": {
            "coordinates": [
                [80.0, 35.0],
                [80.05, 35.0],
                [80.1, 35.01],
                [80.15, 35.03],
                [80.2, 35.07],
                [80.22, 35.12],
                [80.21, 35.18],
                [80.17, 35.22],
                [80.1, 35.24],
                [80.03, 35.23],
                [79.98, 35.2],
                [79.95, 35.15],
                [79.94, 35.08],
                [79.96, 35.03],
                [80.0, 35.0],
            ]
        },
        "parameters": {"delta_mm": 2.0},
    }

    data = _preview(edit_api_client.client, session_id, payload)

    assert data["preview_id"]
    assert data["affected_grid_count"] > 0
    assert data["affected_area_km2"] > 0
    assert data["after_stats"]["mean"] > data["before_stats"]["mean"]
    assert data["op_ptype_transition"]["1_to_1"] > 0


def test_apply_updates_session_state_and_is_bit_exact_with_preview(
    edit_api_client: EditApiClient,
) -> None:
    session_id = _start_session(edit_api_client.client)
    preview_data = _preview(
        edit_api_client.client, session_id, _polygon_payload(session_id)
    )
    preview_id = preview_data["preview_id"]
    cached = preview_cache.load_preview(
        preview_id, session_id, REPO_ROOT / "tmp" / "previews" / session_id
    )

    applied = _apply(edit_api_client.client, session_id, preview_id)

    assert applied["sequence_no"] == 1
    assert applied["applied"] is True
    assert applied["can_undo"] is True
    assert applied["can_redo"] is False
    assert np.array_equal(_qpf(edit_api_client.path_builder, session_id), cached["qpf_after"])


def test_new_precip_needs_ptype_error_then_apply_with_target_ptype(
    edit_api_client: EditApiClient,
) -> None:
    session_id = _start_session(edit_api_client.client)
    payload = _polygon_payload(
        session_id,
        operation="set_value",
        delta_mm=None,
        value=3.0,
        coordinates=[[75.0, 30.0], [75.3, 30.0], [75.3, 30.3], [75.0, 30.3]],
    )
    data = _preview(edit_api_client.client, session_id, payload)

    failed = edit_api_client.client.post(
        "/api/edit/apply",
        json={"session_id": session_id, "preview_id": data["preview_id"]},
        headers=_headers(),
    )
    assert failed.status_code == 422
    assert failed.json()["code"] == "NEW_PRECIP_NEEDS_PTYPE"

    applied = _apply(
        edit_api_client.client, session_id, data["preview_id"], target_ptype=2
    )
    ptype_after = np.load(
        edit_api_client.path_builder.session_root(session_id) / "ptype_after.npy"
    )
    assert applied["sequence_no"] == 1
    assert np.count_nonzero(ptype_after == 2) > 0


def test_redo_preserves_target_ptype_for_new_precip(
    edit_api_client: EditApiClient,
) -> None:
    session_id = _start_session(edit_api_client.client)
    payload = _polygon_payload(
        session_id,
        operation="set_value",
        delta_mm=None,
        value=3.0,
        coordinates=[[75.0, 30.0], [75.3, 30.0], [75.3, 30.3], [75.0, 30.3]],
    )
    data = _preview(edit_api_client.client, session_id, payload)
    assert data["new_precip_needs_ptype"] is True

    _apply(edit_api_client.client, session_id, data["preview_id"], target_ptype=2)
    applied_ptype = _ptype(edit_api_client.path_builder, session_id).copy()
    assert np.count_nonzero(applied_ptype == 2) > 0

    undo = edit_api_client.client.post(
        "/api/edit/undo", json={"session_id": session_id}, headers=_headers()
    )
    assert undo.status_code == 200
    redo = edit_api_client.client.post(
        "/api/edit/redo", json={"session_id": session_id}, headers=_headers()
    )

    assert redo.status_code == 200
    assert np.array_equal(_ptype(edit_api_client.path_builder, session_id), applied_ptype)


def test_undo_redo_and_undo_all_restore_expected_states(
    edit_api_client: EditApiClient,
) -> None:
    session_id = _start_session(edit_api_client.client)
    base = _qpf(edit_api_client.path_builder, session_id).copy()

    p1 = _preview(
        edit_api_client.client,
        session_id,
        _polygon_payload(session_id, delta_mm=2.0),
    )
    _apply(edit_api_client.client, session_id, p1["preview_id"])
    op1_state = _qpf(edit_api_client.path_builder, session_id).copy()
    p2 = _preview(
        edit_api_client.client,
        session_id,
        _polygon_payload(
            session_id,
            delta_mm=4.0,
            coordinates=[[76.0, 31.0], [76.2, 31.0], [76.2, 31.2], [76.0, 31.2]],
        ),
    )
    _apply(edit_api_client.client, session_id, p2["preview_id"])
    op2_state = _qpf(edit_api_client.path_builder, session_id).copy()

    undo = edit_api_client.client.post(
        "/api/edit/undo", json={"session_id": session_id}, headers=_headers()
    )
    assert undo.status_code == 200
    assert undo.json()["data"]["can_redo"] is True
    assert np.array_equal(_qpf(edit_api_client.path_builder, session_id), op1_state)

    redo = edit_api_client.client.post(
        "/api/edit/redo", json={"session_id": session_id}, headers=_headers()
    )
    assert redo.status_code == 200
    assert redo.json()["data"]["can_redo"] is False
    assert np.array_equal(_qpf(edit_api_client.path_builder, session_id), op2_state)

    edit_api_client.client.post(
        "/api/edit/undo", json={"session_id": session_id}, headers=_headers()
    )
    edit_api_client.client.post(
        "/api/edit/undo", json={"session_id": session_id}, headers=_headers()
    )
    assert np.array_equal(_qpf(edit_api_client.path_builder, session_id), base)


def test_operations_list_returns_metadata(edit_api_client: EditApiClient) -> None:
    session_id = _start_session(edit_api_client.client)
    p1 = _preview(edit_api_client.client, session_id, _polygon_payload(session_id))
    _apply(edit_api_client.client, session_id, p1["preview_id"])

    response = edit_api_client.client.get(
        f"/api/edit/operations?session_id={session_id}", headers=_headers()
    )

    assert response.status_code == 200
    operation = response.json()["data"]["operations"][0]
    assert operation["sequence_no"] == 1
    assert operation["tool_name"] == "polygon"
    assert operation["operation_type"] == "increase"
    assert operation["variable_name"] == "qpf"
    assert operation["affected_grid_count"] > 0
    assert operation["is_undone"] == 0
    assert operation["created_at"]


def test_permission_viewer_gets_permission_denied(
    edit_api_client: EditApiClient,
) -> None:
    session_id = _start_session(edit_api_client.client)

    response = edit_api_client.client.post(
        "/api/edit/preview",
        json=_polygon_payload(session_id),
        headers=_headers(3, "viewer", "viewer"),
    )

    assert response.status_code == 403
    assert response.json()["code"] == "PERMISSION_DENIED"


def test_preview_rejects_non_finite_geometry_coordinate(
    edit_api_client: EditApiClient,
) -> None:
    session_id = _start_session(edit_api_client.client)

    response = edit_api_client.client.post(
        "/api/edit/preview",
        json=_polygon_payload(
            session_id,
            coordinates=[["nan", 35.0], [80.2, 35.0], [80.2, 35.2], [80.0, 35.2]],
        ),
        headers=_headers(),
    )

    assert response.status_code == 422
    assert response.json()["code"] == "MASK_INVALID_GEOMETRY"


def test_preview_expired_and_conflict_errors(edit_api_client: EditApiClient) -> None:
    session_id = _start_session(edit_api_client.client)
    first = _preview(edit_api_client.client, session_id, _polygon_payload(session_id))
    second = _preview(edit_api_client.client, session_id, _polygon_payload(session_id))

    expired = edit_api_client.client.post(
        "/api/edit/apply",
        json={"session_id": session_id, "preview_id": first["preview_id"]},
        headers=_headers(),
    )
    assert expired.status_code == 409
    assert expired.json()["code"] == "PREVIEW_EXPIRED"

    _apply(edit_api_client.client, session_id, second["preview_id"])
    conflict = edit_api_client.client.post(
        "/api/edit/apply",
        json={"session_id": session_id, "preview_id": second["preview_id"]},
        headers=_headers(),
    )
    assert conflict.status_code == 409
    assert conflict.json()["code"] == "PREVIEW_CONFLICT"


def test_session_not_editing_error(edit_api_client: EditApiClient) -> None:
    session_id = _start_session(edit_api_client.client)

    async def close_session() -> None:
        async with edit_api_client.session_factory() as db:
            stored = await db.get(EditSession, session_id)
            assert stored is not None
            stored.status = "saved"
            await db.commit()

    asyncio.run(close_session())
    response = edit_api_client.client.post(
        "/api/edit/preview", json=_polygon_payload(session_id), headers=_headers()
    )

    assert response.status_code == 409
    assert response.json()["code"] == "SESSION_NOT_EDITING"


def test_nothing_to_undo_redo_errors(edit_api_client: EditApiClient) -> None:
    session_id = _start_session(edit_api_client.client)

    undo = edit_api_client.client.post(
        "/api/edit/undo", json={"session_id": session_id}, headers=_headers()
    )
    redo = edit_api_client.client.post(
        "/api/edit/redo", json={"session_id": session_id}, headers=_headers()
    )

    assert undo.status_code == 409
    assert undo.json()["code"] == "NOTHING_TO_UNDO"
    assert redo.status_code == 409
    assert redo.json()["code"] == "NOTHING_TO_REDO"
