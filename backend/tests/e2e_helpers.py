from __future__ import annotations

import asyncio
import json
from collections.abc import AsyncIterator, Iterator
from contextlib import contextmanager
from datetime import UTC, datetime
from pathlib import Path
from typing import Any, NamedTuple
from uuid import uuid4

import numpy as np
import pytest
from fastapi.testclient import TestClient
from passlib.context import CryptContext
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine
from sqlalchemy.pool import StaticPool

from app.core.config import ProductConfig
from app.core.constants import NX, NY
from app.db.models import (
    AppUser,
    AuditLog,
    Base,
    EditSession,
    EditVersion,
    ForecastCase,
    ProductWindow,
    ReviewProduct,
)
from app.db.session import get_db
from app.main import app
from app.repositories.review_product_repo import review_product_repo
from app.storage.path_builder import PathBuilder

CASE_ID = "2026051808"
WINDOW_ID = f"{CASE_ID}_ACC24_000_024"
TEMPLATE_ID = "snow_phase_review_v1"

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")


class E2EClient(NamedTuple):
    client: TestClient
    session_factory: async_sessionmaker[AsyncSession]
    path_builder: PathBuilder
    tokens: dict[str, str]


async def _seed_users(session_factory: async_sessionmaker[AsyncSession]) -> None:
    async with session_factory() as db:
        db.add_all(
            [
                AppUser(
                    id=1,
                    username="admin",
                    password_hash=pwd_context.hash("admin123"),
                    display_name="系统管理员",
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
        await db.commit()


def write_window_originals(
    builder: PathBuilder,
    *,
    qpf_value: float = 1.0,
    ptype_value: int = 1,
    partial: bool = False,
) -> None:
    original_dir = builder.window_original_dir(CASE_ID, WINDOW_ID)
    original_dir.mkdir(parents=True, exist_ok=True)
    qpf = np.full((NY, NX), qpf_value, dtype=np.float32)
    ptype = np.full((NY, NX), ptype_value, dtype=np.uint8)
    invalid = np.zeros((NY, NX), dtype=np.uint8)
    np.savez_compressed(original_dir / "qpf_before.npz", qpf=qpf)
    np.savez_compressed(original_dir / "ptype_before.npz", ptype=ptype)
    np.savez_compressed(original_dir / "ptype_invalid_mask.npz", mask=invalid)

    write_source_tp_files(builder, include_next=not partial)
    if not partial:
        write_source_ifs_files(builder)


def write_source_tp_files(builder: PathBuilder, *, include_next: bool) -> None:
    case_dir = builder.data_source_dir(CASE_ID)
    case_dir.mkdir(parents=True, exist_ok=True)
    shape = (NY, NX)
    for lead, value in [(0, 0.0), (24, 1.0)]:
        np.savetxt(builder.tp_file_path(CASE_ID, lead), np.full(shape, value), delimiter=",")
    if include_next:
        np.savetxt(builder.tp_file_path(CASE_ID, 48), np.full(shape, 2.0), delimiter=",")


def write_source_ifs_files(builder: PathBuilder) -> None:
    ifs_dir = builder.data_source_dir(CASE_ID) / "ifs"
    for variable_name in ["z500", "t850", "rh700", "u850", "v850"]:
        path = ifs_dir / variable_name / f"{CASE_ID[2:]}.012.npz"
        path.parent.mkdir(parents=True, exist_ok=True)
        np.savez_compressed(path, data=np.ones((NY, NX), dtype=np.float32))


async def seed_window(
    session_factory: async_sessionmaker[AsyncSession],
    builder: PathBuilder,
    *,
    status: str = "available",
    qc_status: str = "pass",
    missing_count: int = 0,
    ptype_missing_leads: list[int] | None = None,
) -> None:
    init_time = datetime(2026, 5, 18, 8, tzinfo=UTC)
    original_dir = builder.window_original_dir(CASE_ID, WINDOW_ID)
    async with session_factory() as db:
        db.add(
            ForecastCase(
                case_id=CASE_ID,
                init_time=init_time,
                data_source_path=str(builder.data_source_dir(CASE_ID)),
                scan_count=1,
                status="partial" if status == "partial" else "complete",
            )
        )
        db.add(
            ProductWindow(
                window_id=WINDOW_ID,
                case_id=CASE_ID,
                accum_hours=24,
                start_lead=0,
                end_lead=24,
                status=status,
                qc_status=qc_status,
                negative_count=0,
                missing_count=missing_count,
                ptype_missing_leads=ptype_missing_leads or [],
                qpf_before_path=str(original_dir / "qpf_before.npz"),
                ptype_before_path=str(original_dir / "ptype_before.npz"),
                data_ready_at=init_time,
            )
        )
        await db.commit()


def patch_services(monkeypatch: pytest.MonkeyPatch, builder: PathBuilder) -> None:
    monkeypatch.setattr("app.services.session_service.default_path_builder", builder)
    monkeypatch.setattr("app.services.session_service.session_service.path_builder", builder)
    for service_path in [
        "app.services.version_service.version_service.path_builder",
        "app.services.release_service.release_service.path_builder",
        "app.api.routes.versions.version_service.path_builder",
        "app.api.routes.versions.release_service.path_builder",
        "app.api.routes.reviews.path_builder",
        "app.services.review_service.default_path_builder",
    ]:
        monkeypatch.setattr(service_path, builder)

    for plotter_path in [
        "app.services.version_service.version_service.precip_plotter",
        "app.services.version_service.version_service.delta_qpf_plotter",
        "app.services.version_service.version_service.change_ptype_plotter",
        "app.services.version_service.version_service.mask_plotter",
    ]:
        monkeypatch.setattr(plotter_path, lambda *_args, **_kwargs: b"\x89PNG\r\n\x1a\n")


@contextmanager
def e2e_client(
    tmp_path: Path,
    monkeypatch: pytest.MonkeyPatch,
    *,
    partial: bool = False,
) -> Iterator[E2EClient]:
    builder = PathBuilder(base_dir=tmp_path / "archive", data_source_root=tmp_path / "source")
    write_window_originals(builder, partial=partial)
    patch_services(monkeypatch, builder)

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
        await seed_window(
            session_factory,
            builder,
            status="partial" if partial else "available",
            qc_status="warn" if partial else "pass",
            missing_count=1 if partial else 0,
            ptype_missing_leads=[24] if partial else [],
        )

    asyncio.run(setup_database())

    async def override_get_db() -> AsyncIterator[AsyncSession]:
        async with session_factory() as session:
            yield session

    app.dependency_overrides[get_db] = override_get_db

    try:
        with TestClient(app, raise_server_exceptions=False) as client:
            tokens = {
                username: login(client, username, f"{username}123")
                for username in ["admin", "forecaster", "reviewer"]
            }
            yield E2EClient(
                client=client,
                session_factory=session_factory,
                path_builder=builder,
                tokens=tokens,
            )
    finally:
        app.dependency_overrides.pop(get_db, None)
        asyncio.run(engine.dispose())


def auth(token: str) -> dict[str, str]:
    return {"Authorization": f"Bearer {token}"}


def login(client: TestClient, username: str, password: str) -> str:
    response = client.post(
        "/api/auth/login", json={"username": username, "password": password}
    )
    assert response.status_code == 200, response.text
    return str(response.json()["data"]["token"])


def start_session(env: E2EClient) -> str:
    response = env.client.post(
        "/api/session/start",
        json={"window_id": WINDOW_ID},
        headers=auth(env.tokens["forecaster"]),
    )
    assert response.status_code == 200, response.text
    return str(response.json()["data"]["session_id"])


async def set_session_base_version(
    session_factory: async_sessionmaker[AsyncSession],
    session_id: str,
    base_version_id: str,
) -> None:
    async with session_factory() as db:
        session = await db.get(EditSession, session_id)
        assert session is not None
        session.base_version_id = base_version_id  # type: ignore[assignment]
        db.add(session)
        await db.commit()


def apply_qpf_edit(env: E2EClient, session_id: str, *, delta_mm: float = 0.5) -> None:
    payload = {
        "session_id": session_id,
        "tool": "polygon",
        "variable": "qpf",
        "operation": "increase",
        "mask": {
            "coordinates": [
                [80.0, 35.0],
                [80.2, 35.0],
                [80.2, 35.2],
                [80.0, 35.2],
            ]
        },
        "parameters": {"delta_mm": delta_mm},
    }
    preview = env.client.post(
        "/api/edit/preview", json=payload, headers=auth(env.tokens["forecaster"])
    )
    assert preview.status_code == 200, preview.text
    preview_data = preview.json()["data"]
    assert preview_data["affected_grid_count"] > 0

    apply = env.client.post(
        "/api/edit/apply",
        json={"session_id": session_id, "preview_id": preview_data["preview_id"]},
        headers=auth(env.tokens["forecaster"]),
    )
    assert apply.status_code == 200, apply.text
    assert apply.json()["data"]["sequence_no"] >= 1


def save_submit_approve_release(env: E2EClient, session_id: str) -> str:
    version_id = save_version(env, session_id)
    submit_version(env, version_id)
    review_version(env, version_id, action="approve", comment="通过")
    release_version(env, version_id)
    return version_id


def save_version(env: E2EClient, session_id: str) -> str:
    response = env.client.post(
        "/api/version/save",
        json={"session_id": session_id, "generate_review": True},
        headers=auth(env.tokens["forecaster"]),
    )
    assert response.status_code == 200, response.text
    data = response.json()["data"]
    assert data["version_id"]
    return str(data["version_id"])


def submit_version(env: E2EClient, version_id: str) -> None:
    response = env.client.post(
        "/api/version/submit",
        json={"version_id": version_id},
        headers=auth(env.tokens["forecaster"]),
    )
    assert response.status_code == 200, response.text
    assert response.json()["data"]["status"] == "submitted"


def review_version(env: E2EClient, version_id: str, *, action: str, comment: str) -> None:
    response = env.client.post(
        "/api/version/review",
        json={"version_id": version_id, "action": action, "comment": comment},
        headers=auth(env.tokens["reviewer"]),
    )
    assert response.status_code == 200, response.text
    expected_status = "approved" if action == "approve" else "rejected"
    assert response.json()["data"]["version_status"] == expected_status


def release_version(env: E2EClient, version_id: str) -> None:
    response = env.client.post(
        "/api/version/release",
        json={"version_id": version_id},
        headers=auth(env.tokens["reviewer"]),
    )
    assert response.status_code == 200, response.text
    assert response.json()["data"]["release_status"] == "active"


async def fake_generate_review(
    db: AsyncSession,
    window_id: str,
    version_id: str,
    template_id: str,
    user_id: str,
    path_builder: PathBuilder | None = None,
    *,
    plot_status: str = "success",
    missing_fields: list[dict[str, Any]] | None = None,
) -> Any:
    from app.schemas.review import ReviewGenerateResponse

    assert path_builder is not None
    await review_product_repo.supersede_existing(
        db, window_id=window_id, version_id=version_id, template_id=template_id
    )
    review_id = str(uuid4())
    images_dir = path_builder.review_images_dir(window_id, review_id)
    images_dir.mkdir(parents=True, exist_ok=True)
    payload_path = path_builder.review_payload_path(window_id, review_id)
    log_path = path_builder.review_log_path(window_id, review_id)
    image_path = images_dir / "review_composite.png"
    missing = missing_fields or []
    payload_path.write_text(
        json.dumps(
            {
                "review_id": review_id,
                "window_id": window_id,
                "version_id": version_id,
                "missing_fields": missing,
            },
            ensure_ascii=False,
            indent=2,
        ),
        encoding="utf-8",
    )
    log_path.write_text(plot_status, encoding="utf-8")
    image_path.write_bytes(b"\x89PNG\r\n\x1a\n")
    await review_product_repo.create(
        db,
        review_id=review_id,
        window_id=window_id,
        version_id=version_id,
        template_id=template_id,
        plot_status=plot_status,
        plot_config_path=str(payload_path),
        plot_input_manifest_path=str(payload_path),
        image_path=str(image_path),
        error_log_path=str(log_path),
        total_panels=3,
        success_panels=3 if plot_status == "success" else 2,
        skipped_panels=0 if plot_status == "success" else 1,
        missing_fields_json=json.dumps(missing, ensure_ascii=False),
    )
    return ReviewGenerateResponse(
        review_id=review_id,
        plot_status=plot_status,
        message="复盘绘图任务已完成",
    )


def generate_review(env: E2EClient, version_id: str, *, expected_status: str) -> str:
    response = env.client.post(
        "/api/review/generate",
        json={
            "window_id": WINDOW_ID,
            "version_id": version_id,
            "template_id": TEMPLATE_ID,
        },
        headers=auth(env.tokens["forecaster"]),
    )
    assert response.status_code == 200, response.text
    data = response.json()["data"]
    assert data["plot_status"] == expected_status
    return str(data["review_id"])


def export_review(env: E2EClient, review_id: str) -> None:
    response = env.client.post(
        "/api/review/export",
        json={"review_id": review_id},
        headers=auth(env.tokens["admin"]),
    )
    assert response.status_code == 200, response.text
    assert response.headers["content-type"] == "application/zip"


async def audit_actions(
    session_factory: async_sessionmaker[AsyncSession],
) -> list[str]:
    async with session_factory() as db:
        result = await db.execute(select(AuditLog).order_by(AuditLog.id.asc()))
        return [str(item.action) for item in result.scalars().all()]


async def audit_logs(
    session_factory: async_sessionmaker[AsyncSession],
    action: str | None = None,
) -> list[AuditLog]:
    async with session_factory() as db:
        statement = select(AuditLog)
        if action is not None:
            statement = statement.where(AuditLog.action == action)
        result = await db.execute(statement.order_by(AuditLog.id.asc()))
        return list(result.scalars().all())


async def versions_by_no(
    session_factory: async_sessionmaker[AsyncSession],
) -> list[EditVersion]:
    async with session_factory() as db:
        result = await db.execute(
            select(EditVersion)
            .where(EditVersion.window_id == WINDOW_ID)
            .order_by(EditVersion.version_no.asc())
        )
        return list(result.scalars().all())


async def review_product(
    session_factory: async_sessionmaker[AsyncSession],
    review_id: str,
) -> ReviewProduct:
    async with session_factory() as db:
        product = await db.get(ReviewProduct, review_id)
        assert product is not None
        return product


def compact_product_config() -> ProductConfig:
    return ProductConfig.model_validate(
        {
            "init_time_zone": "UTC",
            "init_times": ["08Z", "20Z"],
            "max_lead_hours": 48,
            "window_step_hours": 24,
            "lead_step_hours": 3,
            "ptype_qpf_threshold_mm": 0.1,
            "allow_zero_start_lead_fallback": True,
            "accum_products": {
                "24": {"accum_hours": 24, "allowed_start_leads": [0, 24]}
            },
        }
    )
