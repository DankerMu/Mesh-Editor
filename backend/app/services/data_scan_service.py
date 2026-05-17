from __future__ import annotations

import logging
import traceback
from collections.abc import Callable
from datetime import UTC, datetime
from pathlib import Path
from typing import Any
from uuid import uuid4

from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker

from app.core.config import ProductConfig, settings
from app.core.error_registry import get_error
from app.core.errors import DomainError
from app.db.models import ProductWindow
from app.engines import ptype_builder, qpf_builder
from app.repositories.data_scan_log_repo import (
    DataScanLogRepository,
    data_scan_log_repo,
)
from app.repositories.forecast_case_repo import (
    ForecastCaseRepository,
    forecast_case_repo,
)
from app.repositories.product_window_repo import (
    ProductWindowRepository,
    product_window_repo,
)
from app.storage import archive_builder
from app.storage.path_builder import PathBuilder, path_builder as default_path_builder

logger = logging.getLogger(__name__)

SessionFactory = async_sessionmaker[AsyncSession] | Callable[[], AsyncSession]


def _domain_error(code: str, detail: dict[str, Any] | None = None) -> DomainError:
    message, http_status = get_error(code)
    return DomainError(
        code=code,
        message=message,
        detail=detail,
        http_status=http_status,
    )


def _window_id(case_id: str, accum_hours: int, start_lead: int, end_lead: int) -> str:
    return f"{case_id}_ACC{accum_hours}_{start_lead:03d}_{end_lead:03d}"


def validate_case_id(case_id: str, config: ProductConfig) -> datetime:
    if len(case_id) != 10 or not case_id.isdigit():
        raise _domain_error("INVALID_CASE_ID", {"case_id": case_id})

    init_hour = case_id[-2:]
    init_times = getattr(config, "init_times", ["08Z", "20Z"])
    allowed_hours = {str(item).replace("Z", "").zfill(2) for item in init_times}
    if init_hour not in allowed_hours:
        raise _domain_error(
            "INVALID_CASE_ID",
            {"case_id": case_id, "allowed_hours": sorted(allowed_hours)},
        )

    try:
        parsed = datetime.strptime(case_id, "%Y%m%d%H").replace(tzinfo=UTC)
    except ValueError as exc:
        raise _domain_error("INVALID_CASE_ID", {"case_id": case_id}) from exc

    if getattr(config, "init_time_zone", "UTC") != "UTC":
        raise _domain_error(
            "INVALID_CASE_ID",
            {"case_id": case_id, "init_time_zone": config.init_time_zone},
        )
    return parsed


def enumerate_windows(config: ProductConfig) -> list[dict[str, int]]:
    accum_products = getattr(config, "accum_products", None)
    if not isinstance(accum_products, dict):
        accum_products = settings.product_config.get("accum_products", {})

    max_lead_hours = int(getattr(config, "max_lead_hours", 240))
    window_step_hours = int(getattr(config, "window_step_hours", 24))
    windows: list[dict[str, int]] = []
    for key in sorted(accum_products, key=lambda value: int(value)):
        product = accum_products[key]
        accum_hours = int(product["accum_hours"])
        for raw_start_lead in product["allowed_start_leads"]:
            start_lead = int(raw_start_lead)
            end_lead = start_lead + accum_hours
            if start_lead % window_step_hours != 0 or end_lead > max_lead_hours:
                raise ValueError(
                    "非法窗口配置: "
                    f"accum_hours={accum_hours}, start_lead={start_lead}, "
                    f"max_lead_hours={max_lead_hours}"
                )
            windows.append(
                {
                    "accum_hours": accum_hours,
                    "start_lead": start_lead,
                    "end_lead": end_lead,
                }
            )
    return windows


def _status_from_results(
    qpf_result: qpf_builder.QpfBuildResult,
    ptype_result: ptype_builder.PtypeBuildResult,
) -> tuple[str, str]:
    if qpf_result.qc_status == "fail":
        return "invalid", "fail"
    if ptype_result.effective_lead_count == 0:
        return "invalid", "fail"
    if ptype_result.qc_status == "warn":
        return "partial", "warn"
    if qpf_result.qc_status == "warn" and ptype_result.qc_status == "pass":
        return "available", "warn"
    return "available", "pass"


def _missing_count(
    qpf_result: qpf_builder.QpfBuildResult,
    ptype_result: ptype_builder.PtypeBuildResult,
) -> int:
    return (
        int(qpf_result.missing_count)
        + len(ptype_result.ptype_missing_leads)
        + len(ptype_result.tp_missing_leads)
    )


def build_single_window(
    case_id: str,
    window_def: dict[str, int],
    config: ProductConfig,
    path_builder: PathBuilder,
    expected_shape: tuple[int, int] | None = None,
) -> dict[str, Any]:
    accum_hours = int(window_def["accum_hours"])
    start_lead = int(window_def["start_lead"])
    end_lead = int(window_def["end_lead"])
    window_id = _window_id(case_id, accum_hours, start_lead, end_lead)

    qpf_result = qpf_builder.build_qpf_window(
        case_id,
        start_lead,
        end_lead,
        config,
        path_builder,
        expected_shape,
    )
    ptype_result = ptype_builder.build_ptype_window(
        case_id,
        start_lead,
        end_lead,
        config,
        path_builder,
        expected_shape,
    )
    status, qc_status = _status_from_results(qpf_result, ptype_result)
    saved_paths: dict[str, str] = {}
    if status != "invalid":
        saved_paths = archive_builder.save_window_original(
            case_id, window_id, qpf_result, ptype_result, path_builder
        )

    errors: list[dict[str, Any]] = []
    if qpf_result.qc_status == "fail":
        errors.append(
            {
                "window_id": window_id,
                "type": "qpf",
                "reason": qpf_result.manifest.get("failure_reason", "qpf_failed"),
            }
        )
    if ptype_result.effective_lead_count == 0:
        errors.append(
            {
                "window_id": window_id,
                "type": "ptype",
                "reason": "no_effective_ptype_leads",
                "ptype_missing_leads": ptype_result.ptype_missing_leads,
                "tp_missing_leads": ptype_result.tp_missing_leads,
            }
        )

    return {
        "window_id": window_id,
        "case_id": case_id,
        "accum_hours": accum_hours,
        "start_lead": start_lead,
        "end_lead": end_lead,
        "status": status,
        "qc_status": qc_status,
        "negative_count": qpf_result.negative_count,
        "negative_min_value": qpf_result.negative_min_value,
        "negative_abs_max": qpf_result.negative_abs_max,
        "missing_count": _missing_count(qpf_result, ptype_result),
        "ptype_missing_leads": ptype_result.ptype_missing_leads,
        "qpf_before_path": saved_paths.get("qpf_before_path"),
        "ptype_before_path": saved_paths.get("ptype_before_path"),
        "data_ready_at": datetime.now(UTC)
        if status in {"available", "partial"}
        else None,
        "tp_files_found": _count_existing_paths(
            [qpf_result.start_tp_path, qpf_result.end_tp_path]
        ),
        "ptype_files_found": len(ptype_result.used_leads),
        "errors": errors,
    }


def _count_existing_paths(paths: list[Path | None]) -> int:
    return len({path for path in paths if path is not None and path.exists()})


def _forecast_case_status(counts_by_status: dict[str, int]) -> str:
    if counts_by_status.get("available", 0) == 23:
        return "complete"
    if counts_by_status.get("available", 0) or counts_by_status.get("partial", 0):
        return "partial"
    return "pending"


def _traceback_error(exc: BaseException) -> dict[str, Any]:
    logger.error("Scan exception: %s", traceback.format_exc())
    return {
        "code": getattr(exc, "code", exc.__class__.__name__),
        "message": str(exc)[:500],
    }


class DataScanService:
    def __init__(
        self,
        config: ProductConfig | None = None,
        path_builder: PathBuilder | None = None,
        forecast_cases: ForecastCaseRepository | None = None,
        product_windows: ProductWindowRepository | None = None,
        scan_logs: DataScanLogRepository | None = None,
        expected_shape: tuple[int, int] | None = None,
    ) -> None:
        self.config = config or settings.product
        self.path_builder = path_builder or default_path_builder
        self.forecast_cases = forecast_cases or forecast_case_repo
        self.product_windows = product_windows or product_window_repo
        self.scan_logs = scan_logs or data_scan_log_repo
        self.expected_shape = expected_shape

    async def scan_case(
        self,
        case_id: str,
        session_factory: SessionFactory,
        scan_id: str | None = None,
    ) -> str:
        init_time = validate_case_id(case_id, self.config)
        scan_id = scan_id or str(uuid4())
        case_dir = self.path_builder.data_source_dir(case_id)
        windows_created = 0
        windows_updated = 0
        window_errors: list[dict[str, Any]] = []

        async with session_factory() as db:
            existing_scan = await self.scan_logs.get_by_scan_id(db, scan_id)
            running_scan_exists = await self.scan_logs.has_running_scan(db, case_id)
            if running_scan_exists and (
                existing_scan is None
                or existing_scan.case_id != case_id
                or existing_scan.status != "running"
            ):
                raise _domain_error("SCAN_ALREADY_RUNNING", {"case_id": case_id})

            await self.forecast_cases.create_or_update(db, case_id, init_time, case_dir)
            if existing_scan is None:
                await self.scan_logs.create(db, scan_id, case_id, datetime.now(UTC))
            await db.commit()

        if not case_dir.exists() or not case_dir.is_dir():
            async with session_factory() as db:
                await self._fail_scan(
                    db,
                    scan_id,
                    forecast_case_case_id=case_id,
                    error={
                        "code": "CASE_DIR_NOT_FOUND",
                        "message": get_error("CASE_DIR_NOT_FOUND")[0],
                        "path": str(case_dir),
                    },
                )
                await db.commit()
            return scan_id

        try:
            window_defs = enumerate_windows(self.config)
            for window_def in window_defs:
                window_data = build_single_window(
                    case_id,
                    window_def,
                    self.config,
                    self.path_builder,
                    self.expected_shape,
                )
                window_errors.extend(window_data.pop("errors"))
                window_data.pop("tp_files_found")
                window_data.pop("ptype_files_found")

                async with session_factory() as db:
                    existing = await db.get(ProductWindow, window_data["window_id"])
                    await self.product_windows.create_or_update(db, **window_data)
                    if existing is None:
                        windows_created += 1
                    else:
                        windows_updated += 1
                    await db.commit()

            async with session_factory() as db:
                counts = await self.product_windows.count_by_status(db, case_id)
                forecast_case = await self.forecast_cases.get_by_case_id(db, case_id)
                if forecast_case is not None:
                    setattr(forecast_case, "status", _forecast_case_status(counts))
                    db.add(forecast_case)
                    await db.flush()

                await self.forecast_cases.mark_scan_completed(db, case_id)
                await self.scan_logs.update_finished(
                    db,
                    scan_id,
                    "completed",
                    datetime.now(UTC),
                    _count_files(case_dir, "tp_*.txt"),
                    _count_files(case_dir, "ptype_*.txt"),
                    windows_created,
                    windows_updated,
                    window_errors or None,
                )
                await db.commit()
        except Exception as exc:
            async with session_factory() as db:
                await self._fail_scan(
                    db,
                    scan_id,
                    forecast_case_case_id=case_id,
                    error=_traceback_error(exc),
                    windows_created=windows_created,
                    windows_updated=windows_updated,
                )
                await db.commit()
            return scan_id

        return scan_id

    async def _fail_scan(
        self,
        db: AsyncSession,
        scan_id: str,
        forecast_case_case_id: str,
        error: dict[str, Any],
        windows_created: int = 0,
        windows_updated: int = 0,
    ) -> None:
        forecast_case = await self.forecast_cases.get_by_case_id(
            db, forecast_case_case_id
        )
        if forecast_case is not None:
            setattr(forecast_case, "status", "pending")
            db.add(forecast_case)
            await db.flush()

        await self.scan_logs.update_finished(
            db,
            scan_id,
            "failed",
            datetime.now(UTC),
            0,
            0,
            windows_created,
            windows_updated,
            [error],
        )


def _count_files(directory: Path, pattern: str) -> int:
    if not directory.exists():
        return 0
    return sum(1 for path in directory.glob(pattern) if path.is_file())


data_scan_service = DataScanService()


async def scan_case(
    case_id: str, session_factory: SessionFactory, scan_id: str | None = None
) -> str:
    return await data_scan_service.scan_case(case_id, session_factory, scan_id)
