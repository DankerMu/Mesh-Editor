from __future__ import annotations

import json
from collections.abc import AsyncIterator
from datetime import UTC, datetime, timedelta
from pathlib import Path

import numpy as np
import pytest
from sqlalchemy.ext.asyncio import AsyncSession, create_async_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from app.db.models import AppUser, Base, EditVersion, ForecastCase, ProductWindow
from app.repositories.review_product_repo import review_product_repo
from app.services.plot_task_service import PlotTaskService
from app.storage.path_builder import PathBuilder

CASE_ID = "2026051608"
WINDOW_ID = f"{CASE_ID}_ACC24_024_048"
VERSION_ID = f"{WINDOW_ID}_v001"
TEMPLATE_ID = "snow_phase_review_v1"

PNG_1X1 = (
    b"\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00\x01"
    b"\x00\x00\x00\x01\x08\x02\x00\x00\x00\x90wS\xde\x00"
    b"\x00\x00\x0cIDATx\x9cc\xf8\xff\xff?\x00\x05\xfe"
    b"\x02\xfeA\xe2!\xbc\x00\x00\x00\x00IEND\xaeB`\x82"
)


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
    db.add(
        ForecastCase(
            case_id=CASE_ID,
            init_time=datetime(2026, 5, 16, 8, tzinfo=UTC),
            status="complete",
        )
    )
    db.add(
        ProductWindow(
            window_id=WINDOW_ID,
            case_id=CASE_ID,
            accum_hours=24,
            start_lead=24,
            end_lead=48,
            status="available",
            qc_status="pass",
            negative_count=0,
            missing_count=0,
        )
    )
    db.add(
        EditVersion(
            version_id=VERSION_ID,
            window_id=WINDOW_ID,
            version_no=1,
            base_version_id=None,
            session_id=None,
            status="approved",
            qpf_after_path="/tmp/qpf_after.npz",
            ptype_after_path="/tmp/ptype_after.npz",
            delta_qpf_path="/tmp/delta_qpf.npz",
            change_ptype_path="/tmp/change_ptype.npz",
            touched_mask_path="/tmp/touched_mask.npz",
            changed_mask_path="/tmp/changed_mask.npz",
            created_by="forecaster",
        )
    )
    await db.commit()


def _builder(tmp_path: Path) -> PathBuilder:
    return PathBuilder(base_dir=tmp_path / "archive", data_source_root=tmp_path / "src")


def _write_npz(path: Path, value: float = 1.0, dtype=np.float32) -> Path:
    path.parent.mkdir(parents=True, exist_ok=True)
    np.savez_compressed(path, data=np.full((4, 5), value, dtype=dtype))
    return path


async def _create_product(
    db: AsyncSession,
    *,
    review_id: str = "review-1",
    status: str = "pending",
    attempt: int = 0,
    locked_by: str | None = None,
    locked_at: datetime | None = None,
    next_retry_at: datetime | None = None,
    plot_config_path: str | None = None,
) -> None:
    await review_product_repo.create(
        db,
        review_id=review_id,
        window_id=WINDOW_ID,
        version_id=VERSION_ID,
        template_id=TEMPLATE_ID,
        plot_status=status,
        attempt=attempt,
        locked_by=locked_by,
        locked_at=locked_at,
        next_retry_at=next_retry_at,
        plot_config_path=plot_config_path,
        plot_input_manifest_path=plot_config_path,
    )


def _payload(
    builder: PathBuilder,
    review_id: str,
    *,
    include_optional: bool = True,
    missing_required: bool = False,
) -> dict:
    root = builder.review_root(WINDOW_ID, review_id)
    root.mkdir(parents=True, exist_ok=True)
    data_dir = root / "data"
    edit_fields = {
        "qpf_before": str(_write_npz(data_dir / "qpf_before.npz", 1.0)),
        "ptype_before": str(_write_npz(data_dir / "ptype_before.npz", 1.0, np.uint8)),
        "qpf_after": str(_write_npz(data_dir / "qpf_after.npz", 2.0)),
        "ptype_after": str(_write_npz(data_dir / "ptype_after.npz", 2.0, np.uint8)),
        "delta_qpf": str(_write_npz(data_dir / "delta_qpf.npz", 1.0)),
        "change_ptype": str(_write_npz(data_dir / "change_ptype.npz", 1.0, np.int8)),
    }
    if missing_required:
        Path(edit_fields["delta_qpf"]).unlink()
    ifs_fields = []
    if include_optional:
        for name in ["z500", "u850", "v850"]:
            ifs_fields.append(
                {
                    "variable_name": name,
                    "source_model": "ifs",
                    "level_type": "pressure",
                    "level_value": 500 if name == "z500" else 850,
                    "lead_hour": 36,
                    "unit": "x",
                    "path": str(_write_npz(data_dir / f"{name}.npz", 1.0)),
                }
            )
    payload = {
        "metadata": {
            "case_id": CASE_ID,
            "window_id": WINDOW_ID,
            "version_id": VERSION_ID,
            "start_lead": 24,
            "end_lead": 48,
            "accum_hours": 24,
        },
        "edit_fields": edit_fields,
        "review_windows": {},
        "ifs_fields": ifs_fields,
        "missing_fields": []
        if include_optional
        else [
            {
                "variable_name": "z500",
                "level_type": "pressure",
                "level_value": 500,
                "lead_hour": 36,
                "reason": "file_not_found",
            }
        ],
        "template": {
            "template_id": TEMPLATE_ID,
            "required_fields": list(edit_fields),
            "optional_fields": ["z500", "u850", "v850"],
            "allow_partial_success": True,
            "panels": [
                {"id": "before", "type": "precip_phase", "fields": ["qpf_before", "ptype_before"]},
                {"id": "delta", "type": "delta_qpf", "fields": ["delta_qpf"]},
                {"id": "circulation", "type": "circulation", "fields": ["z500", "u850", "v850"]},
            ],
        },
        "output": {
            "review_root": str(root),
            "images_dir": str(builder.review_images_dir(WINDOW_ID, review_id)),
            "composite_image_path": str(
                builder.review_images_dir(WINDOW_ID, review_id) / "review_composite.png"
            ),
            "plot_log_path": str(builder.review_log_path(WINDOW_ID, review_id)),
            "grid_def": {"extent": [70.0, 111.0, 25.0, 50.0]},
        },
        "plot_task": {"review_id": review_id},
    }
    builder.review_payload_path(WINDOW_ID, review_id).write_text(
        json.dumps(payload, ensure_ascii=False), encoding="utf-8"
    )
    return payload


def _patch_plotters(monkeypatch: pytest.MonkeyPatch, fail_delta: bool = False) -> None:
    def write_png(*args, **kwargs):
        output_path = Path(kwargs["output_path"])
        output_path.parent.mkdir(parents=True, exist_ok=True)
        output_path.write_bytes(PNG_1X1)
        return str(output_path)

    def delta(*args, **kwargs):
        if fail_delta:
            raise RuntimeError("plotter boom")
        return PNG_1X1

    from app.services import plot_task_service

    monkeypatch.setitem(plot_task_service.PANEL_DISPATCH, "precip_phase", write_png)
    monkeypatch.setitem(plot_task_service.PANEL_DISPATCH, "circulation", write_png)
    monkeypatch.setitem(plot_task_service.PANEL_DISPATCH, "delta_qpf", delta)


async def test_claim_task_pending_updates_running(
    db_session: AsyncSession, tmp_path: Path
) -> None:
    await _create_product(db_session)
    service = PlotTaskService(_builder(tmp_path))

    task = await service.claim_task(db_session, "worker-1")

    assert task is not None
    assert task.plot_status == "running"
    assert task.locked_by == "worker-1"
    assert task.attempt == 1


async def test_claim_task_no_pending_returns_none(
    db_session: AsyncSession, tmp_path: Path
) -> None:
    service = PlotTaskService(_builder(tmp_path))

    assert await service.claim_task(db_session, "worker-1") is None


async def test_claim_task_max_concurrent_reached(
    db_session: AsyncSession, tmp_path: Path
) -> None:
    await _create_product(db_session, review_id="running-1", status="running")
    await _create_product(db_session, review_id="running-2", status="running")
    await _create_product(db_session, review_id="pending-1", status="pending")
    service = PlotTaskService(_builder(tmp_path))

    assert await service.claim_task(db_session, "worker-1") is None


async def test_complete_task_success_updates_results(
    db_session: AsyncSession, tmp_path: Path
) -> None:
    await _create_product(db_session, status="running", locked_by="worker")
    service = PlotTaskService(_builder(tmp_path))

    product = await service.complete_task(
        db_session,
        "review-1",
        "success",
        image_path="/tmp/review.png",
        total_panels=3,
        success_panels=3,
        skipped_panels=0,
    )

    assert product is not None
    assert product.plot_status == "success"
    assert product.locked_by is None
    assert product.image_path == "/tmp/review.png"


async def test_complete_task_failed_retry_available(
    db_session: AsyncSession, tmp_path: Path
) -> None:
    await _create_product(db_session, status="running", attempt=1)
    service = PlotTaskService(_builder(tmp_path))

    product = await service.complete_task(db_session, "review-1", "failed")

    assert product is not None
    assert product.plot_status == "failed"
    assert product.next_retry_at is not None


async def test_complete_task_failed_max_retries(
    db_session: AsyncSession, tmp_path: Path
) -> None:
    await _create_product(db_session, status="running", attempt=3)
    service = PlotTaskService(_builder(tmp_path))

    product = await service.complete_task(db_session, "review-1", "failed")

    assert product is not None
    assert product.plot_status == "permanently_failed"


async def test_recover_stale_tasks(db_session: AsyncSession, tmp_path: Path) -> None:
    await _create_product(
        db_session,
        status="running",
        attempt=1,
        locked_by="worker",
        locked_at=datetime.now(UTC) - timedelta(seconds=600),
    )
    service = PlotTaskService(_builder(tmp_path))

    await service.recover_stale_tasks(db_session)
    product = await review_product_repo.get_by_id(db_session, "review-1")

    assert product is not None
    assert product.plot_status == "failed"
    assert product.locked_by is None


async def test_recover_retryable_failed_tasks(
    db_session: AsyncSession, tmp_path: Path
) -> None:
    await _create_product(
        db_session,
        status="failed",
        next_retry_at=datetime.now(UTC) - timedelta(seconds=1),
    )
    service = PlotTaskService(_builder(tmp_path))

    await service.recover_retryable_failed_tasks(db_session)
    product = await review_product_repo.get_by_id(db_session, "review-1")

    assert product is not None
    assert product.plot_status == "pending"
    assert product.next_retry_at is None


async def test_execute_task_happy_path(
    db_session: AsyncSession, tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> None:
    builder = _builder(tmp_path)
    _payload(builder, "review-1")
    await _create_product(db_session, status="running", attempt=1)
    _patch_plotters(monkeypatch)
    service = PlotTaskService(builder)

    product = await service.execute_task(db_session, "review-1")

    assert product is not None
    assert product.plot_status == "success"
    assert product.image_path is not None
    assert Path(product.image_path).exists()


async def test_execute_task_missing_optional_partial_success(
    db_session: AsyncSession, tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> None:
    builder = _builder(tmp_path)
    _payload(builder, "review-1", include_optional=False)
    await _create_product(db_session, status="running", attempt=1)
    _patch_plotters(monkeypatch)
    service = PlotTaskService(builder)

    product = await service.execute_task(db_session, "review-1")

    assert product is not None
    assert product.plot_status == "partial_success"
    assert product.skipped_panels == 1


async def test_execute_task_required_missing_failed(
    db_session: AsyncSession, tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> None:
    builder = _builder(tmp_path)
    _payload(builder, "review-1", missing_required=True)
    await _create_product(db_session, status="running", attempt=1)
    _patch_plotters(monkeypatch)
    service = PlotTaskService(builder)

    product = await service.execute_task(db_session, "review-1")

    assert product is not None
    assert product.plot_status == "failed"
    assert product.error_log_path is not None
    assert "必需复盘字段缺失" in Path(product.error_log_path).read_text(encoding="utf-8")


async def test_execute_task_plotter_exception_failed(
    db_session: AsyncSession, tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> None:
    builder = _builder(tmp_path)
    _payload(builder, "review-1")
    await _create_product(db_session, status="running", attempt=1)
    _patch_plotters(monkeypatch, fail_delta=True)
    service = PlotTaskService(builder)

    product = await service.execute_task(db_session, "review-1")

    assert product is not None
    assert product.plot_status == "failed"
    assert product.error_log_path is not None
    assert "plotter boom" in Path(product.error_log_path).read_text(encoding="utf-8")
