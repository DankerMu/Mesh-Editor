from __future__ import annotations

import asyncio
import io
import json
import zipfile
from collections.abc import AsyncIterator, Iterator
from datetime import UTC, datetime, timedelta
from pathlib import Path
from typing import NamedTuple

import numpy as np
import pytest
from fastapi.testclient import TestClient
from passlib.context import CryptContext
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine
from sqlalchemy.pool import StaticPool

from app.core.security import create_access_token, get_jwt_secret
from app.db.models import AppUser, AuditLog, Base, ForecastCase, ProductWindow
from app.db.session import get_db
from app.main import app
from app.repositories.edit_version_repo import edit_version_repo
from app.repositories.review_product_repo import review_product_repo
from app.storage.path_builder import PathBuilder

CASE_ID = "2026051608"
OTHER_CASE_ID = "2026051620"
WINDOW_ID = f"{CASE_ID}_ACC24_024_048"
OTHER_WINDOW_ID = f"{CASE_ID}_ACC24_048_072"
OTHER_CASE_WINDOW_ID = f"{OTHER_CASE_ID}_ACC24_024_048"
TEMPLATE_ID = "snow_phase_review_v1"

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")


class ReviewApiClient(NamedTuple):
    client: TestClient
    session_factory: async_sessionmaker[AsyncSession]
    path_builder: PathBuilder


@pytest.fixture()
def review_api_client(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> Iterator[ReviewApiClient]:
    builder = PathBuilder(base_dir=tmp_path / "archive", data_source_root=tmp_path / "src")
    for service_path in [
        "app.api.routes.reviews.path_builder",
        "app.services.review_service.default_path_builder",
    ]:
        monkeypatch.setattr(service_path, builder)

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
        yield ReviewApiClient(client, session_factory, builder)

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
                AppUser(
                    id=4,
                    username="viewer",
                    password_hash=pwd_context.hash("viewer123"),
                    display_name="查看员",
                    role="viewer",
                    is_active=True,
                ),
            ]
        )
        db.add_all(
            [
                ForecastCase(
                    case_id=CASE_ID,
                    init_time=datetime(2026, 5, 16, 8, tzinfo=UTC),
                    status="complete",
                ),
                ForecastCase(
                    case_id=OTHER_CASE_ID,
                    init_time=datetime(2026, 5, 16, 20, tzinfo=UTC),
                    status="complete",
                ),
            ]
        )
        for window_id, case_id, start_lead, end_lead in [
            (WINDOW_ID, CASE_ID, 24, 48),
            (OTHER_WINDOW_ID, CASE_ID, 48, 72),
            (OTHER_CASE_WINDOW_ID, OTHER_CASE_ID, 24, 48),
        ]:
            db.add(
                ProductWindow(
                    window_id=window_id,
                    case_id=case_id,
                    accum_hours=24,
                    start_lead=start_lead,
                    end_lead=end_lead,
                    status="available",
                    qc_status="pass",
                    negative_count=0,
                    missing_count=0,
                )
            )
        await db.commit()


def _write_npz(path: Path, value: float = 1.0) -> Path:
    path.parent.mkdir(parents=True, exist_ok=True)
    np.savez_compressed(path, data=np.full((4, 5), value, dtype=np.float32))
    return path


async def _create_version(
    session_factory: async_sessionmaker[AsyncSession],
    builder: PathBuilder,
    *,
    window_id: str = WINDOW_ID,
    version_no: int = 1,
    status: str = "approved",
) -> str:
    version_id = f"{window_id}_v{version_no:03d}"
    version_root = builder.version_root(window_id, version_id)
    original_root = builder.window_root(window_id) / "original"
    field_paths = {
        "qpf_before": _write_npz(original_root / "qpf_before.npz", 1.0),
        "ptype_before": _write_npz(original_root / "ptype_before.npz", 1.0),
        "qpf_after": _write_npz(version_root / "qpf_after.npz", 2.0),
        "ptype_after": _write_npz(version_root / "ptype_after.npz", 2.0),
        "delta_qpf": _write_npz(version_root / "delta_qpf.npz", 1.0),
        "change_ptype": _write_npz(version_root / "change_ptype.npz", 1.0),
        "touched_mask": _write_npz(version_root / "touched_mask.npz", 1.0),
        "changed_mask": _write_npz(version_root / "changed_mask.npz", 1.0),
    }
    async with session_factory() as db:
        window = await db.get(ProductWindow, window_id)
        assert window is not None
        window.qpf_before_path = str(field_paths["qpf_before"])  # type: ignore[assignment]
        window.ptype_before_path = str(field_paths["ptype_before"])  # type: ignore[assignment]
        await edit_version_repo.create(
            db,
            version_id=version_id,
            window_id=window_id,
            version_no=version_no,
            base_version_id=None,
            session_id=None,
            status=status,
            qpf_after_path=str(field_paths["qpf_after"]),
            ptype_after_path=str(field_paths["ptype_after"]),
            delta_qpf_path=str(field_paths["delta_qpf"]),
            change_ptype_path=str(field_paths["change_ptype"]),
            touched_mask_path=str(field_paths["touched_mask"]),
            changed_mask_path=str(field_paths["changed_mask"]),
            created_by="forecaster",
        )
        await db.commit()
    return version_id


async def _create_review(
    session_factory: async_sessionmaker[AsyncSession],
    builder: PathBuilder,
    *,
    review_id: str,
    window_id: str = WINDOW_ID,
    version_id: str | None = None,
    version_no: int = 1,
    plot_status: str = "pending",
    created_offset_seconds: int = 0,
) -> str:
    if version_id is None:
        version_id = f"{window_id}_v{version_no:03d}"
    async with session_factory() as db:
        if await edit_version_repo.get(db, version_id) is None:
            await edit_version_repo.create(
                db,
                version_id=version_id,
                window_id=window_id,
                version_no=version_no,
                base_version_id=None,
                session_id=None,
                status="approved",
                qpf_after_path=f"/tmp/{version_id}/qpf_after.npz",
                ptype_after_path=f"/tmp/{version_id}/ptype_after.npz",
                delta_qpf_path=f"/tmp/{version_id}/delta_qpf.npz",
                change_ptype_path=f"/tmp/{version_id}/change_ptype.npz",
                touched_mask_path=f"/tmp/{version_id}/touched_mask.npz",
                changed_mask_path=f"/tmp/{version_id}/changed_mask.npz",
                created_by="forecaster",
            )
        root = builder.review_root(window_id, review_id)
        images = builder.review_images_dir(window_id, review_id)
        root.mkdir(parents=True, exist_ok=True)
        images.mkdir(parents=True, exist_ok=True)
        payload_path = builder.review_payload_path(window_id, review_id)
        payload_path.write_text(
            json.dumps({"review_id": review_id, "window_id": window_id}),
            encoding="utf-8",
        )
        product = await review_product_repo.create(
            db,
            review_id=review_id,
            window_id=window_id,
            version_id=version_id,
            template_id=TEMPLATE_ID,
            plot_status=plot_status,
            plot_config_path=str(payload_path),
            plot_input_manifest_path=str(payload_path),
            image_path=str(images / "review_composite.png"),
            error_log_path=str(builder.review_log_path(window_id, review_id)),
            total_panels=3,
            success_panels=2 if plot_status == "partial_success" else 3,
            skipped_panels=1 if plot_status == "partial_success" else 0,
            missing_fields_json="[]",
        )
        product.created_at = datetime(2026, 5, 16, 8, 0, tzinfo=UTC) + timedelta(
            seconds=created_offset_seconds
        )
        await db.commit()
    return review_id


def _generate(
    client: TestClient, version_id: str, headers: dict[str, str] | None = None
):
    return client.post(
        "/api/review/generate",
        json={
            "window_id": WINDOW_ID,
            "version_id": version_id,
            "template_id": TEMPLATE_ID,
        },
        headers=headers,
    )


def test_generate_auth(review_api_client: ReviewApiClient) -> None:
    version_id = asyncio.run(
        _create_version(review_api_client.session_factory, review_api_client.path_builder)
    )

    no_token = _generate(review_api_client.client, version_id)
    assert no_token.status_code == 401

    viewer = _generate(review_api_client.client, version_id, _headers(4, "viewer", "viewer"))
    assert viewer.status_code == 403
    assert viewer.json()["code"] == "PERMISSION_DENIED"

    forecaster = _generate(
        review_api_client.client, version_id, _headers(2, "forecaster", "forecaster")
    )
    assert forecaster.status_code == 200, forecaster.text


def test_generate_version_not_released(review_api_client: ReviewApiClient) -> None:
    version_id = asyncio.run(
        _create_version(
            review_api_client.session_factory,
            review_api_client.path_builder,
            status="draft",
        )
    )

    response = _generate(review_api_client.client, version_id, _headers())

    assert response.status_code == 409
    assert response.json()["code"] == "VERSION_STATUS_CONFLICT"


def test_generate_success_creates_audit_log(review_api_client: ReviewApiClient) -> None:
    version_id = asyncio.run(
        _create_version(review_api_client.session_factory, review_api_client.path_builder)
    )

    response = _generate(review_api_client.client, version_id, _headers())

    assert response.status_code == 200, response.text
    data = response.json()["data"]
    assert data["plot_status"] == "pending"

    async def count_audit() -> int:
        async with review_api_client.session_factory() as db:
            result = await db.execute(
                select(AuditLog).where(
                    AuditLog.action == "review_generate",
                    AuditLog.resource_id == data["review_id"],
                )
            )
            return len(result.scalars().all())

    assert asyncio.run(count_audit()) == 1


def test_get_plot_task(review_api_client: ReviewApiClient) -> None:
    asyncio.run(
        _create_review(
            review_api_client.session_factory,
            review_api_client.path_builder,
            review_id="review-task",
            plot_status="success",
        )
    )

    response = review_api_client.client.get(
        "/api/tasks/plot/review-task", headers=_headers(4, "viewer", "viewer")
    )

    assert response.status_code == 200, response.text
    data = response.json()["data"]
    assert data["review_id"] == "review-task"
    assert data["plot_status"] == "success"
    assert data["total_panels"] == 3


def test_get_plot_task_not_found(review_api_client: ReviewApiClient) -> None:
    response = review_api_client.client.get("/api/tasks/plot/nonexistent", headers=_headers())

    assert response.status_code == 404
    assert response.json()["code"] == "REVIEW_NOT_FOUND"


def test_reviews_list_and_filter(review_api_client: ReviewApiClient) -> None:
    asyncio.run(
        _create_review(
            review_api_client.session_factory,
            review_api_client.path_builder,
            review_id="review-old",
            plot_status="pending",
            created_offset_seconds=1,
        )
    )
    asyncio.run(
        _create_review(
            review_api_client.session_factory,
            review_api_client.path_builder,
            review_id="review-new",
            version_no=2,
            plot_status="success",
            created_offset_seconds=2,
        )
    )

    response = review_api_client.client.get("/api/reviews", headers=_headers())
    assert response.status_code == 200, response.text
    payload = response.json()["data"]
    assert payload["total"] == 2
    assert [item["review_id"] for item in payload["items"]] == [
        "review-new",
        "review-old",
    ]

    filtered = review_api_client.client.get(
        "/api/reviews?plot_status=success", headers=_headers()
    )
    assert filtered.status_code == 200, filtered.text
    assert [item["review_id"] for item in filtered.json()["data"]["items"]] == [
        "review-new"
    ]


def test_reviews_by_case(review_api_client: ReviewApiClient) -> None:
    asyncio.run(
        _create_review(
            review_api_client.session_factory,
            review_api_client.path_builder,
            review_id="review-case-main",
        )
    )
    asyncio.run(
        _create_review(
            review_api_client.session_factory,
            review_api_client.path_builder,
            review_id="review-other-case",
            window_id=OTHER_CASE_WINDOW_ID,
        )
    )

    response = review_api_client.client.get(f"/api/review/case/{CASE_ID}", headers=_headers())
    assert response.status_code == 200, response.text
    assert [item["review_id"] for item in response.json()["data"]] == ["review-case-main"]

    empty = review_api_client.client.get("/api/review/case/2027010108", headers=_headers())
    assert empty.status_code == 200
    assert empty.json()["data"] == []


def test_reviews_by_window_versions(review_api_client: ReviewApiClient) -> None:
    asyncio.run(
        _create_review(
            review_api_client.session_factory,
            review_api_client.path_builder,
            review_id="review-v1",
            version_no=1,
        )
    )
    asyncio.run(
        _create_review(
            review_api_client.session_factory,
            review_api_client.path_builder,
            review_id="review-v2",
            version_no=2,
        )
    )

    response = review_api_client.client.get(
        f"/api/review/window/{WINDOW_ID}/versions", headers=_headers()
    )

    assert response.status_code == 200, response.text
    data = response.json()["data"]
    assert [item["review_id"] for item in data] == ["review-v2", "review-v1"]
    assert [item["version_no"] for item in data] == [2, 1]


def test_export_success(review_api_client: ReviewApiClient) -> None:
    review_id = asyncio.run(
        _create_review(
            review_api_client.session_factory,
            review_api_client.path_builder,
            review_id="review-export",
            plot_status="success",
        )
    )
    root = review_api_client.path_builder.review_root(WINDOW_ID, review_id)
    (root / "images" / "panel.png").write_bytes(b"png")
    (root / "plot_log.txt").write_text("ok", encoding="utf-8")
    _write_npz(root / "field.npz", 1.0)

    response = review_api_client.client.post(
        "/api/review/export", json={"review_id": review_id}, headers=_headers()
    )

    assert response.status_code == 200, response.text
    assert response.headers["content-type"] == "application/zip"
    with zipfile.ZipFile(io.BytesIO(response.content)) as archive:
        assert set(archive.namelist()) == {
            "review_payload.json",
            "images/panel.png",
            "plot_log.txt",
            "field.npz",
        }


def test_export_pending_not_ready(review_api_client: ReviewApiClient) -> None:
    review_id = asyncio.run(
        _create_review(
            review_api_client.session_factory,
            review_api_client.path_builder,
            review_id="review-pending",
            plot_status="pending",
        )
    )

    response = review_api_client.client.post(
        "/api/review/export", json={"review_id": review_id}, headers=_headers()
    )

    assert response.status_code == 409
    assert response.json()["code"] == "REVIEW_NOT_READY"


def test_regenerate_supersedes_existing(review_api_client: ReviewApiClient) -> None:
    version_id = asyncio.run(
        _create_version(review_api_client.session_factory, review_api_client.path_builder)
    )

    first = _generate(review_api_client.client, version_id, _headers())
    second = _generate(review_api_client.client, version_id, _headers())

    assert first.status_code == 200, first.text
    assert second.status_code == 200, second.text
    first_id = first.json()["data"]["review_id"]
    second_id = second.json()["data"]["review_id"]

    async def statuses() -> dict[str, str]:
        async with review_api_client.session_factory() as db:
            first_product = await review_product_repo.get_by_id(db, first_id)
            second_product = await review_product_repo.get_by_id(db, second_id)
            assert first_product is not None
            assert second_product is not None
            return {
                str(first_product.review_id): str(first_product.plot_status),
                str(second_product.review_id): str(second_product.plot_status),
            }

    assert asyncio.run(statuses()) == {first_id: "superseded", second_id: "pending"}
