from fastapi import APIRouter, Depends, Request
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.error_registry import get_error
from app.core.errors import DomainError
from app.db.session import get_db
from app.repositories.product_window_repo import product_window_repo
from app.schemas.common import ApiResponse
from app.schemas.window import WindowItem

router = APIRouter(tags=["windows"])


def _trace_id(request: Request) -> str:
    return str(getattr(request.state, "trace_id", ""))


def _validation_error(message: str) -> DomainError:
    _, http_status = get_error("VALIDATION_ERROR")
    return DomainError(
        code="VALIDATION_ERROR",
        message=message,
        http_status=http_status,
    )


@router.get("/windows", response_model=ApiResponse[list[WindowItem]])
async def list_windows(
    request: Request,
    case_id: str | None = None,
    accum_hours: int | None = None,
    status: str | None = None,
    db: AsyncSession = Depends(get_db),
) -> ApiResponse[list[WindowItem]]:
    if not case_id:
        raise _validation_error("case_id 为必填参数")
    if accum_hours is not None and accum_hours not in {24, 48, 168}:
        raise _validation_error("accum_hours 仅支持 24/48/168")

    windows = await product_window_repo.list_by_case_id(
        db,
        case_id=case_id,
        accum_hours=accum_hours,
        status=status,
    )
    return ApiResponse(
        data=[WindowItem.model_validate(window) for window in windows],
        trace_id=_trace_id(request),
    )
