from datetime import datetime
from uuid import uuid4

from fastapi import APIRouter, BackgroundTasks, Depends, Request
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker

from app.api.dependencies import get_session_factory
from app.core.config import settings
from app.core.error_registry import get_error
from app.core.errors import DomainError
from app.db.models import AppUser
from app.db.session import get_db
from app.repositories.data_scan_log_repo import data_scan_log_repo
from app.repositories.forecast_case_repo import forecast_case_repo
from app.repositories.product_window_repo import product_window_repo
from app.schemas.common import ApiResponse
from app.schemas.data_scan import ScanRequest, ScanResponse, ScanStatusResponse
from app.services.data_scan_service import validate_case_id
from app.storage.path_builder import path_builder
from app.workers.data_scan_worker import run_data_scan

router = APIRouter(tags=["data-scan"])

INVALID_CASE_ID_MESSAGE = "case_id 格式错误，要求 YYYYMMDDHH 且时次为 08 或 20"


def _trace_id(request: Request) -> str:
    return str(getattr(request.state, "trace_id", ""))


def _domain_error(
    code: str,
    *,
    message: str | None = None,
    detail: dict[str, object] | None = None,
) -> DomainError:
    default_message, http_status = get_error(code)
    return DomainError(
        code=code,
        message=message or default_message,
        detail=detail,
        http_status=http_status,
    )


def _validate_case_id(case_id: str) -> datetime:
    try:
        return validate_case_id(case_id, settings.product)
    except DomainError as exc:
        if exc.code == "INVALID_CASE_ID":
            raise _domain_error(
                "INVALID_CASE_ID",
                message=INVALID_CASE_ID_MESSAGE,
                detail={"case_id": case_id},
            ) from exc
        raise


def _require_scan_role(current_user: AppUser) -> None:
    if current_user.role not in {"admin", "reviewer"}:
        raise _domain_error("PERMISSION_DENIED")


@router.post("/data/scan", response_model=ApiResponse[ScanResponse])
async def post_scan(
    payload: ScanRequest,
    background_tasks: BackgroundTasks,
    request: Request,
    db: AsyncSession = Depends(get_db),
    session_factory: async_sessionmaker[AsyncSession] = Depends(get_session_factory),
) -> ApiResponse[ScanResponse]:
    current_user: AppUser = request.state.current_user
    _require_scan_role(current_user)
    init_time = _validate_case_id(payload.case_id)

    if await data_scan_log_repo.has_running_scan(db, payload.case_id):
        raise _domain_error("SCAN_ALREADY_RUNNING", detail={"case_id": payload.case_id})

    scan_id = str(uuid4())
    await forecast_case_repo.create_or_update(
        db,
        payload.case_id,
        init_time,
        path_builder.tp_source_dir(payload.case_id),
    )
    await data_scan_log_repo.create(
        db, scan_id, payload.case_id, datetime.now(init_time.tzinfo)
    )
    await db.commit()

    background_tasks.add_task(
        run_data_scan,
        case_id=payload.case_id,
        session_factory=session_factory,
        scan_id=scan_id,
    )
    return ApiResponse(
        message="扫描已启动",
        data=ScanResponse(scan_id=scan_id, status="running"),
        trace_id=_trace_id(request),
    )


@router.get("/data/status", response_model=ApiResponse[ScanStatusResponse])
async def get_scan_status(
    request: Request,
    scan_id: str | None = None,
    case_id: str | None = None,
    db: AsyncSession = Depends(get_db),
) -> ApiResponse[ScanStatusResponse]:
    if not scan_id and not case_id:
        raise _domain_error(
            "VALIDATION_ERROR", message="scan_id 或 case_id 至少提供一个"
        )

    scan_log = (
        await data_scan_log_repo.get_by_scan_id(db, scan_id)
        if scan_id
        else await data_scan_log_repo.get_latest_by_case_id(db, str(case_id))
    )
    if scan_log is None:
        raise _domain_error("SCAN_NOT_FOUND")

    counts = await product_window_repo.count_by_status(db, str(scan_log.case_id))
    total = sum(counts.values())
    response = ScanStatusResponse.model_validate(scan_log)
    response.total_windows = total
    response.available_count = counts.get("available", 0)
    response.partial_count = counts.get("partial", 0)
    response.invalid_count = counts.get("invalid", 0)

    return ApiResponse(
        data=response,
        trace_id=_trace_id(request),
    )
