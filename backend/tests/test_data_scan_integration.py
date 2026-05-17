from __future__ import annotations

from datetime import UTC, datetime
from pathlib import Path

import numpy as np
import pytest
from sqlalchemy.ext.asyncio import async_sessionmaker, create_async_engine

from app.core.config import ProductConfig
from app.core.errors import DomainError
from app.db.models import Base, DataScanLog, ForecastCase
from app.repositories.data_scan_log_repo import data_scan_log_repo
from app.repositories.forecast_case_repo import forecast_case_repo
from app.repositories.product_window_repo import product_window_repo
from app.services.data_scan_service import (
    DataScanService,
    enumerate_windows,
    validate_case_id,
)
from app.storage.path_builder import PathBuilder
from tests.fixtures.conftest import TEST_SHAPE, write_grid

CASE_ID = "2026051608"


@pytest.fixture()
def scan_config() -> ProductConfig:
    return ProductConfig.model_validate(
        {
            "max_lead_hours": 240,
            "lead_step_hours": 3,
            "window_step_hours": 24,
            "init_times": ["08Z", "20Z"],
            "init_time_zone": "UTC",
            "ptype_qpf_threshold_mm": 0.1,
            "allow_zero_start_lead_fallback": True,
            "accum_products": {
                "24": {
                    "accum_hours": 24,
                    "allowed_start_leads": [
                        0,
                        24,
                        48,
                        72,
                        96,
                        120,
                        144,
                        168,
                        192,
                        216,
                    ],
                },
                "48": {
                    "accum_hours": 48,
                    "allowed_start_leads": [0, 24, 48, 72, 96, 120, 144, 168, 192],
                },
                "168": {
                    "accum_hours": 168,
                    "allowed_start_leads": [0, 24, 48, 72],
                },
            },
        }
    )


@pytest.fixture()
async def session_factory(tmp_path: Path):
    engine = create_async_engine(f"sqlite+aiosqlite:///{tmp_path / 'scan.db'}")
    async with engine.begin() as connection:
        await connection.run_sync(Base.metadata.create_all)
    try:
        yield async_sessionmaker(engine, expire_on_commit=False)
    finally:
        await engine.dispose()


@pytest.fixture()
def path_builder(tmp_path: Path) -> PathBuilder:
    return PathBuilder(
        base_dir=tmp_path / "archive",
        data_source_root=tmp_path / "source",
    )


def _service(config: ProductConfig, builder: PathBuilder) -> DataScanService:
    return DataScanService(
        config=config, path_builder=builder, expected_shape=TEST_SHAPE
    )


def _write_full_case(
    builder: PathBuilder,
    *,
    case_id: str = CASE_ID,
    missing_tp: set[int] | None = None,
    missing_ptype: set[int] | None = None,
    negative_end_lead: int | None = None,
) -> None:
    missing_tp = missing_tp or set()
    missing_ptype = missing_ptype or set()
    running = np.zeros(TEST_SHAPE, dtype=float)
    for lead in range(0, 241, 3):
        if lead == 0:
            running = np.zeros(TEST_SHAPE, dtype=float)
        else:
            running = running + 0.2
        if negative_end_lead is not None and lead == negative_end_lead:
            running = running.copy()
            running[0, 0] = -1.0
        if lead not in missing_tp:
            write_grid(builder.tp_file_path(case_id, lead), running)
        if lead > 0 and lead not in missing_ptype:
            write_grid(
                builder.ptype_file_path(case_id, lead),
                np.ones(TEST_SHAPE, dtype=float),
            )


async def test_scan_create_windows(
    session_factory, scan_config: ProductConfig, path_builder: PathBuilder
) -> None:
    _write_full_case(path_builder)

    scan_id = await _service(scan_config, path_builder).scan_case(
        CASE_ID, session_factory
    )

    async with session_factory() as db:
        latest = await data_scan_log_repo.get_by_scan_id(db, scan_id)
        windows = await product_window_repo.list_by_case_id(db, CASE_ID)
        forecast_case = await forecast_case_repo.get_by_case_id(db, CASE_ID)

    assert latest is not None
    assert latest.status == "completed"
    assert len(windows) == 23
    assert forecast_case is not None
    assert forecast_case.status == "complete"


def test_23_windows_exact(scan_config: ProductConfig) -> None:
    init_time = validate_case_id(CASE_ID, scan_config)
    window_ids = [
        f"{CASE_ID}_ACC{item['accum_hours']}_{item['start_lead']:03d}_{item['end_lead']:03d}"
        for item in enumerate_windows(scan_config)
    ]

    assert init_time == datetime(2026, 5, 16, 8, tzinfo=UTC)
    assert len(window_ids) == 23
    assert window_ids == [
        "2026051608_ACC24_000_024",
        "2026051608_ACC24_024_048",
        "2026051608_ACC24_048_072",
        "2026051608_ACC24_072_096",
        "2026051608_ACC24_096_120",
        "2026051608_ACC24_120_144",
        "2026051608_ACC24_144_168",
        "2026051608_ACC24_168_192",
        "2026051608_ACC24_192_216",
        "2026051608_ACC24_216_240",
        "2026051608_ACC48_000_048",
        "2026051608_ACC48_024_072",
        "2026051608_ACC48_048_096",
        "2026051608_ACC48_072_120",
        "2026051608_ACC48_096_144",
        "2026051608_ACC48_120_168",
        "2026051608_ACC48_144_192",
        "2026051608_ACC48_168_216",
        "2026051608_ACC48_192_240",
        "2026051608_ACC168_000_168",
        "2026051608_ACC168_024_192",
        "2026051608_ACC168_048_216",
        "2026051608_ACC168_072_240",
    ]


async def test_scan_idempotent(
    session_factory, scan_config: ProductConfig, path_builder: PathBuilder
) -> None:
    _write_full_case(path_builder)
    service = _service(scan_config, path_builder)

    await service.scan_case(CASE_ID, session_factory)
    await service.scan_case(CASE_ID, session_factory)

    async with session_factory() as db:
        windows = await product_window_repo.list_by_case_id(db, CASE_ID)
        forecast_case = await forecast_case_repo.get_by_case_id(db, CASE_ID)
        latest = await data_scan_log_repo.get_latest_by_case_id(db, CASE_ID)

    assert len(windows) == 23
    assert forecast_case is not None
    assert forecast_case.scan_count == 2
    assert latest is not None
    assert latest.windows_created == 0
    assert latest.windows_updated == 23


async def test_concurrent_scan_rejected(
    session_factory, scan_config: ProductConfig, path_builder: PathBuilder
) -> None:
    async with session_factory() as db:
        db.add(
            ForecastCase(
                case_id=CASE_ID,
                init_time=datetime(2026, 5, 16, 8, tzinfo=UTC),
                data_source_path=str(path_builder.data_source_dir(CASE_ID)),
                scan_count=1,
                last_scan_at=datetime.now(UTC),
            )
        )
        db.add(
            DataScanLog(
                scan_id="running-scan",
                case_id=CASE_ID,
                status="running",
                scan_started_at=datetime.now(UTC),
            )
        )
        await db.commit()

    with pytest.raises(DomainError) as exc_info:
        await _service(scan_config, path_builder).scan_case(CASE_ID, session_factory)

    assert exc_info.value.code == "SCAN_ALREADY_RUNNING"


async def test_case_dir_not_found(
    session_factory, scan_config: ProductConfig, path_builder: PathBuilder
) -> None:
    scan_id = await _service(scan_config, path_builder).scan_case(
        CASE_ID, session_factory
    )

    async with session_factory() as db:
        latest = await data_scan_log_repo.get_by_scan_id(db, scan_id)
        windows = await product_window_repo.list_by_case_id(db, CASE_ID)

    assert latest is not None
    assert latest.status == "failed"
    assert latest.errors_json[0]["code"] == "CASE_DIR_NOT_FOUND"
    assert windows == []


async def test_status_available(
    session_factory, scan_config: ProductConfig, path_builder: PathBuilder
) -> None:
    _write_full_case(path_builder)

    await _service(scan_config, path_builder).scan_case(CASE_ID, session_factory)

    async with session_factory() as db:
        counts = await product_window_repo.count_by_status(db, CASE_ID)
        first = (await product_window_repo.list_by_case_id(db, CASE_ID))[0]

    assert counts == {"available": 23}
    assert first.qc_status == "pass"


async def test_status_partial(
    session_factory, scan_config: ProductConfig, path_builder: PathBuilder
) -> None:
    _write_full_case(path_builder, missing_ptype={24})

    await _service(scan_config, path_builder).scan_case(CASE_ID, session_factory)

    async with session_factory() as db:
        counts = await product_window_repo.count_by_status(db, CASE_ID)
        window = (
            await product_window_repo.list_by_case_id(
                db, CASE_ID, accum_hours=24, status="partial"
            )
        )[0]

    assert counts["partial"] >= 1
    assert window.qc_status == "warn"
    assert 24 in window.ptype_missing_leads


async def test_status_invalid(
    session_factory, scan_config: ProductConfig, path_builder: PathBuilder
) -> None:
    _write_full_case(path_builder, missing_tp={24})

    await _service(scan_config, path_builder).scan_case(CASE_ID, session_factory)

    async with session_factory() as db:
        invalid = await product_window_repo.list_by_case_id(
            db, CASE_ID, accum_hours=24, status="invalid"
        )
        latest = await data_scan_log_repo.get_latest_by_case_id(db, CASE_ID)

    assert invalid
    assert invalid[0].window_id == "2026051608_ACC24_000_024"
    assert invalid[0].qc_status == "fail"
    assert latest is not None
    assert latest.status == "completed"
