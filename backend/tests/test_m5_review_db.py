from __future__ import annotations

from collections.abc import AsyncIterator
from datetime import UTC, datetime, timedelta
from importlib import import_module
from pathlib import Path

import pytest
import sqlalchemy as sa
from alembic import command
from alembic.config import Config
from pydantic import ValidationError
from sqlalchemy.ext.asyncio import AsyncSession, create_async_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from app.core.error_registry import ERROR_CODES, get_error
from app.db.models import (
    AppUser,
    Base,
    EditSession,
    ForecastCase,
    ProductWindow,
    ReviewField,
    ReviewProduct,
)
from app.repositories.edit_version_repo import edit_version_repo
from app.repositories.review_field_repo import review_field_repo
from app.repositories.review_product_repo import review_product_repo
from app.schemas.review import MissingField, PlotTaskStatus, ReviewGenerateRequest

CASE_ID = "2026051608"
OTHER_CASE_ID = "2026051620"
WINDOW_ID = f"{CASE_ID}_ACC24_000_024"
OTHER_WINDOW_ID = f"{CASE_ID}_ACC24_024_048"
OTHER_CASE_WINDOW_ID = f"{OTHER_CASE_ID}_ACC24_000_024"
SESSION_ID = "00000000-0000-0000-0000-000000000200"


@pytest.fixture()
async def db_session() -> AsyncIterator[AsyncSession]:
    engine = create_async_engine(
        "sqlite+aiosqlite://",
        connect_args={"check_same_thread": False},
        poolclass=StaticPool,
    )
    async with engine.begin() as connection:
        await connection.run_sync(Base.metadata.create_all)

    async_session = sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)
    async with async_session() as session:
        await _seed_parent_rows(session)
        yield session

    await engine.dispose()


async def _seed_parent_rows(db: AsyncSession) -> None:
    db.add(
        AppUser(
            id=1,
            username="forecaster",
            password_hash="hash",
            display_name="预报员",
            role="forecaster",
            is_active=True,
        )
    )
    for case_id, init_hour in [(CASE_ID, 8), (OTHER_CASE_ID, 20)]:
        db.add(
            ForecastCase(
                case_id=case_id,
                init_time=datetime(2026, 5, 16, init_hour, tzinfo=UTC),
                status="complete",
            )
        )
    for window_id, case_id, start_lead, end_lead in [
        (WINDOW_ID, CASE_ID, 0, 24),
        (OTHER_WINDOW_ID, CASE_ID, 24, 48),
        (OTHER_CASE_WINDOW_ID, OTHER_CASE_ID, 0, 24),
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
    db.add(
        EditSession(
            session_id=SESSION_ID,
            window_id=WINDOW_ID,
            user_id=1,
            base_version_id=f"{WINDOW_ID}_v000",
            status="editing",
        )
    )
    await db.commit()


def _version_kwargs(version_no: int = 1, **overrides: object) -> dict[str, object]:
    window_id = str(overrides.get("window_id", WINDOW_ID))
    data: dict[str, object] = {
        "version_id": f"{window_id}_v{version_no:03d}",
        "window_id": window_id,
        "version_no": version_no,
        "base_version_id": f"{window_id}_v000",
        "session_id": SESSION_ID if window_id == WINDOW_ID else None,
        "status": "draft",
        "qpf_after_path": f"/data/{window_id}/v{version_no:03d}/qpf_after.npz",
        "ptype_after_path": f"/data/{window_id}/v{version_no:03d}/ptype_after.npz",
        "delta_qpf_path": f"/data/{window_id}/v{version_no:03d}/delta_qpf.npz",
        "change_ptype_path": f"/data/{window_id}/v{version_no:03d}/change_ptype.npz",
        "touched_mask_path": f"/data/{window_id}/v{version_no:03d}/touched_mask.npz",
        "changed_mask_path": f"/data/{window_id}/v{version_no:03d}/changed_mask.npz",
        "created_by": "forecaster",
    }
    data.update(overrides)
    return data


def _review_field_kwargs(index: int = 1, **overrides: object) -> dict[str, object]:
    data: dict[str, object] = {
        "field_id": f"field-{index}",
        "window_id": WINDOW_ID,
        "version_id": f"{WINDOW_ID}_v001",
        "source_model": "ifs",
        "variable_name": "tp",
        "level_type": "surface",
        "level_value": None,
        "lead_hour": index * 3,
        "valid_time": datetime(2026, 5, 16, 8 + index, tzinfo=UTC),
        "unit": "mm",
        "file_path": f"/review/field-{index}.npz",
    }
    data.update(overrides)
    return data


def _review_product_kwargs(index: int = 1, **overrides: object) -> dict[str, object]:
    window_id = str(overrides.get("window_id", WINDOW_ID))
    version_id = str(overrides.get("version_id", f"{window_id}_v001"))
    data: dict[str, object] = {
        "review_id": f"review-{index}",
        "window_id": window_id,
        "version_id": version_id,
        "template_id": "template-main",
        "image_path": None,
        "plot_config_path": f"/review/{index}/plot_config.json",
        "plot_input_manifest_path": f"/review/{index}/manifest.json",
        "plot_code_version": "abc123",
    }
    data.update(overrides)
    return data


def _migration_config(db_path: Path) -> Config:
    config = Config("alembic.ini")
    config.set_main_option("sqlalchemy.url", f"sqlite+aiosqlite:///{db_path}")
    return config


async def _create_versions(db_session: AsyncSession) -> None:
    await edit_version_repo.create(db_session, **_version_kwargs(version_no=1))
    await edit_version_repo.create(db_session, **_version_kwargs(version_no=2))
    await edit_version_repo.create(
        db_session,
        **_version_kwargs(
            version_no=1,
            window_id=OTHER_WINDOW_ID,
            version_id=f"{OTHER_WINDOW_ID}_v001",
        ),
    )
    await edit_version_repo.create(
        db_session,
        **_version_kwargs(
            version_no=1,
            window_id=OTHER_CASE_WINDOW_ID,
            version_id=f"{OTHER_CASE_WINDOW_ID}_v001",
        ),
    )


def test_m5_model_table_names() -> None:
    assert ReviewField.__tablename__ == "review_field"
    assert ReviewProduct.__tablename__ == "review_product"


def test_m5_migration_v008_creates_review_tables(tmp_path: Path) -> None:
    migration = import_module("app.db.migrations.versions.v008_m5_review_tables")
    assert migration.revision == "v008"
    assert migration.down_revision == "v007"

    db_path = tmp_path / "migration.db"
    command.upgrade(_migration_config(db_path), "head")

    engine = sa.create_engine(f"sqlite:///{db_path}")
    inspector = sa.inspect(engine)
    assert "review_field" in inspector.get_table_names()
    assert "review_product" in inspector.get_table_names()

    field_columns = {
        column["name"]: column for column in inspector.get_columns("review_field")
    }
    product_columns = {
        column["name"]: column for column in inspector.get_columns("review_product")
    }

    assert set(field_columns) == {
        "field_id",
        "window_id",
        "version_id",
        "source_model",
        "variable_name",
        "level_type",
        "level_value",
        "lead_hour",
        "valid_time",
        "unit",
        "file_path",
        "created_at",
    }
    assert field_columns["field_id"]["primary_key"] == 1
    for column_name in [
        "window_id",
        "source_model",
        "variable_name",
        "file_path",
        "created_at",
    ]:
        assert field_columns[column_name]["nullable"] is False

    assert set(product_columns) == {
        "review_id",
        "window_id",
        "version_id",
        "template_id",
        "image_path",
        "plot_config_path",
        "plot_input_manifest_path",
        "plot_code_version",
        "plot_status",
        "attempt",
        "max_retries",
        "locked_by",
        "locked_at",
        "next_retry_at",
        "plot_started_at",
        "plot_finished_at",
        "total_panels",
        "success_panels",
        "skipped_panels",
        "missing_fields_json",
        "error_log_path",
        "created_at",
    }
    assert product_columns["review_id"]["primary_key"] == 1
    for column_name in [
        "window_id",
        "version_id",
        "template_id",
        "plot_status",
        "attempt",
        "max_retries",
        "created_at",
    ]:
        assert product_columns[column_name]["nullable"] is False
    assert product_columns["plot_status"]["default"] == "'pending'"
    assert product_columns["attempt"]["default"] == "'0'"
    assert product_columns["max_retries"]["default"] == "'3'"

    product_indexes = {
        index["name"]: index for index in inspector.get_indexes("review_product")
    }
    assert product_indexes["idx_review_product_window_status"]["column_names"] == [
        "window_id",
        "plot_status",
    ]
    assert product_indexes["idx_review_product_supersede"]["column_names"] == [
        "window_id",
        "version_id",
        "template_id",
    ]
    engine.dispose()


async def test_review_field_repo_create_and_list_by_version(
    db_session: AsyncSession,
) -> None:
    await _create_versions(db_session)

    field = await review_field_repo.create(
        db_session, **_review_field_kwargs(index=1)
    )

    assert field.field_id == "field-1"
    assert field.window_id == WINDOW_ID
    assert field.version_id == f"{WINDOW_ID}_v001"
    assert field.source_model == "ifs"
    assert field.variable_name == "tp"
    assert field.file_path == "/review/field-1.npz"
    assert field.created_at is not None

    fields = await review_field_repo.list_by_version(db_session, f"{WINDOW_ID}_v001")
    assert [item.field_id for item in fields] == ["field-1"]


async def test_review_field_repo_list_by_window_filters(
    db_session: AsyncSession,
) -> None:
    await _create_versions(db_session)
    await review_field_repo.create(db_session, **_review_field_kwargs(index=1))
    await review_field_repo.create(db_session, **_review_field_kwargs(index=2))
    await review_field_repo.create(
        db_session,
        **_review_field_kwargs(
            index=3,
            field_id="field-other",
            window_id=OTHER_WINDOW_ID,
            version_id=f"{OTHER_WINDOW_ID}_v001",
        ),
    )

    fields = await review_field_repo.list_by_window(db_session, WINDOW_ID)

    assert [field.field_id for field in fields] == ["field-1", "field-2"]
    assert await review_field_repo.list_by_window(db_session, "missing") == []


async def test_review_product_repo_create_and_get_by_id(
    db_session: AsyncSession,
) -> None:
    await _create_versions(db_session)

    product = await review_product_repo.create(
        db_session, **_review_product_kwargs(index=1)
    )

    assert product.review_id == "review-1"
    assert product.window_id == WINDOW_ID
    assert product.version_id == f"{WINDOW_ID}_v001"
    assert product.template_id == "template-main"
    assert product.plot_status == "pending"
    assert product.attempt == 0
    assert product.max_retries == 3
    assert product.created_at is not None

    fetched = await review_product_repo.get_by_id(db_session, "review-1")
    assert fetched is not None
    assert fetched.review_id == "review-1"
    assert await review_product_repo.get_by_id(db_session, "missing") is None


async def test_review_product_repo_list_by_case(
    db_session: AsyncSession,
) -> None:
    await _create_versions(db_session)
    await review_product_repo.create(db_session, **_review_product_kwargs(index=1))
    await review_product_repo.create(
        db_session,
        **_review_product_kwargs(
            index=2,
            review_id="review-other-case",
            window_id=OTHER_CASE_WINDOW_ID,
            version_id=f"{OTHER_CASE_WINDOW_ID}_v001",
        ),
    )

    products = await review_product_repo.list_by_case(db_session, CASE_ID)

    assert [product.review_id for product in products] == ["review-1"]


async def test_review_product_repo_list_all_filters(
    db_session: AsyncSession,
) -> None:
    await _create_versions(db_session)
    await review_product_repo.create(
        db_session, **_review_product_kwargs(index=1, plot_status="pending")
    )
    await review_product_repo.create(
        db_session,
        **_review_product_kwargs(
            index=2,
            review_id="review-success",
            version_id=f"{WINDOW_ID}_v002",
            plot_status="success",
        ),
    )
    await review_product_repo.create(
        db_session,
        **_review_product_kwargs(
            index=3,
            review_id="review-other-window",
            window_id=OTHER_WINDOW_ID,
            version_id=f"{OTHER_WINDOW_ID}_v001",
            plot_status="success",
        ),
    )

    success_products = await review_product_repo.list_all(
        db_session, plot_status="success"
    )
    window_products = await review_product_repo.list_all(
        db_session, window_id=WINDOW_ID
    )
    case_products = await review_product_repo.list_all(db_session, case_id=CASE_ID)

    assert {product.review_id for product in success_products} == {
        "review-success",
        "review-other-window",
    }
    assert {product.review_id for product in window_products} == {
        "review-1",
        "review-success",
    }
    assert {product.review_id for product in case_products} == {
        "review-1",
        "review-success",
        "review-other-window",
    }


async def test_review_product_repo_claim_task_marks_running(
    db_session: AsyncSession,
) -> None:
    await _create_versions(db_session)
    await review_product_repo.create(db_session, **_review_product_kwargs(index=1))

    claimed = await review_product_repo.claim_task(db_session, "worker-1")

    assert claimed is not None
    assert claimed.review_id == "review-1"
    assert claimed.plot_status == "running"
    assert claimed.locked_by == "worker-1"
    assert claimed.locked_at is not None
    assert claimed.attempt == 1


async def test_review_product_repo_claim_task_no_pending_returns_none(
    db_session: AsyncSession,
) -> None:
    await _create_versions(db_session)

    assert await review_product_repo.claim_task(db_session, "worker-1") is None


async def test_review_product_repo_claim_task_respects_max_concurrent(
    db_session: AsyncSession,
) -> None:
    await _create_versions(db_session)
    await review_product_repo.create(
        db_session, **_review_product_kwargs(index=1, plot_status="running")
    )
    await review_product_repo.create(
        db_session,
        **_review_product_kwargs(
            index=2,
            review_id="review-running-2",
            version_id=f"{WINDOW_ID}_v002",
            plot_status="running",
        ),
    )
    await review_product_repo.create(
        db_session,
        **_review_product_kwargs(
            index=3,
            review_id="review-pending",
            window_id=OTHER_WINDOW_ID,
            version_id=f"{OTHER_WINDOW_ID}_v001",
        ),
    )

    assert (
        await review_product_repo.claim_task(
            db_session, "worker-1", max_concurrent=2
        )
        is None
    )


async def test_review_product_repo_claim_task_skips_future_retry(
    db_session: AsyncSession,
) -> None:
    await _create_versions(db_session)
    await review_product_repo.create(
        db_session,
        **_review_product_kwargs(
            index=1,
            next_retry_at=datetime.now(UTC) + timedelta(hours=1),
        ),
    )
    await review_product_repo.create(
        db_session,
        **_review_product_kwargs(
            index=2,
            review_id="review-ready",
            version_id=f"{WINDOW_ID}_v002",
            next_retry_at=None,
        ),
    )

    claimed = await review_product_repo.claim_task(db_session, "worker-1")

    assert claimed is not None
    assert claimed.review_id == "review-ready"


async def test_review_product_repo_count_running(
    db_session: AsyncSession,
) -> None:
    await _create_versions(db_session)
    await review_product_repo.create(
        db_session, **_review_product_kwargs(index=1, plot_status="running")
    )
    await review_product_repo.create(
        db_session,
        **_review_product_kwargs(
            index=2,
            review_id="review-pending",
            version_id=f"{WINDOW_ID}_v002",
            plot_status="pending",
        ),
    )

    assert await review_product_repo.count_running(db_session) == 1


async def test_review_product_repo_list_stale_tasks(
    db_session: AsyncSession,
) -> None:
    await _create_versions(db_session)
    old_locked_at = datetime.now(UTC) - timedelta(seconds=600)
    recent_locked_at = datetime.now(UTC)
    await review_product_repo.create(
        db_session,
        **_review_product_kwargs(
            index=1,
            plot_status="running",
            locked_by="worker-1",
            locked_at=old_locked_at,
        ),
    )
    await review_product_repo.create(
        db_session,
        **_review_product_kwargs(
            index=2,
            review_id="review-recent",
            version_id=f"{WINDOW_ID}_v002",
            plot_status="running",
            locked_by="worker-2",
            locked_at=recent_locked_at,
        ),
    )

    stale_tasks = await review_product_repo.list_stale_tasks(
        db_session, timeout_seconds=300
    )

    assert [task.review_id for task in stale_tasks] == ["review-1"]


async def test_review_product_repo_supersede_existing(
    db_session: AsyncSession,
) -> None:
    await _create_versions(db_session)
    await review_product_repo.create(
        db_session, **_review_product_kwargs(index=1, plot_status="pending")
    )
    await review_product_repo.create(
        db_session,
        **_review_product_kwargs(
            index=2,
            review_id="review-running",
            plot_status="running",
        ),
    )
    await review_product_repo.create(
        db_session,
        **_review_product_kwargs(
            index=3,
            review_id="review-success",
            plot_status="success",
        ),
    )

    count = await review_product_repo.supersede_existing(
        db_session, WINDOW_ID, f"{WINDOW_ID}_v001", "template-main"
    )

    assert count == 2
    products = await review_product_repo.list_by_window(db_session, WINDOW_ID)
    statuses = {product.review_id: product.plot_status for product in products}
    assert statuses["review-1"] == "superseded"
    assert statuses["review-running"] == "superseded"
    assert statuses["review-success"] == "success"


async def test_review_product_repo_list_by_window_with_versions(
    db_session: AsyncSession,
) -> None:
    await _create_versions(db_session)
    await review_product_repo.create(db_session, **_review_product_kwargs(index=1))

    products = await review_product_repo.list_by_window_with_versions(
        db_session, WINDOW_ID
    )

    assert [product.review_id for product in products] == ["review-1"]
    assert products[0].version_no == 1
    assert products[0].version_status == "draft"
    assert products[0].version_created_by == "forecaster"
    assert products[0].version_created_at is not None


def test_review_schema_validation() -> None:
    with pytest.raises(ValidationError):
        ReviewGenerateRequest.model_validate(
            {"window_id": WINDOW_ID, "version_id": f"{WINDOW_ID}_v001"}
        )

    request = ReviewGenerateRequest(
        window_id=WINDOW_ID,
        version_id=f"{WINDOW_ID}_v001",
        template_id="template-main",
    )
    assert request.template_id == "template-main"

    with pytest.raises(ValidationError):
        MissingField(variable_name="tp", reason="bad_reason")

    missing = MissingField(
        variable_name="tp",
        level_type=None,
        level_value=None,
        lead_hour=24,
        reason="file_not_found",
    )
    assert missing.reason == "file_not_found"
    assert PlotTaskStatus.SUPERSEDED.value == "superseded"


def test_review_error_code_registration() -> None:
    assert "REVIEW_NOT_READY" in ERROR_CODES
    assert get_error("REVIEW_NOT_READY") == ("复盘产品尚未就绪", 409)
    assert "TEMPLATE_NOT_FOUND" in ERROR_CODES
    assert get_error("TEMPLATE_NOT_FOUND") == ("复盘模板不存在", 404)
