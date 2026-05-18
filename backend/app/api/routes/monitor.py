from __future__ import annotations

from fastapi import APIRouter, Depends, Request
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.dependencies import require_role
from app.core.logging import get_trace_id
from app.db.models import AppUser
from app.db.session import get_db
from app.schemas.common import ApiResponse
from app.schemas.m6 import (
    StorageSummaryResponse,
    TaskRetryResponse,
    TaskSummaryResponse,
)
from app.services.storage_monitor_service import storage_monitor_service
from app.services.task_monitor_service import task_monitor_service

router = APIRouter(prefix="/monitor", tags=["Monitor"])


def _trace_id(request: Request) -> str:
    return get_trace_id() or str(getattr(request.state, "trace_id", ""))


@router.get("/storage", response_model=ApiResponse[StorageSummaryResponse])
async def get_storage_summary(
    request: Request,
    _: AppUser = Depends(require_role("admin")),
) -> ApiResponse[StorageSummaryResponse]:
    return ApiResponse(
        message="查询成功",
        data=storage_monitor_service.get_storage_summary(),
        trace_id=_trace_id(request),
    )


@router.get("/tasks", response_model=ApiResponse[TaskSummaryResponse])
async def get_task_summary(
    request: Request,
    db: AsyncSession = Depends(get_db),
    _: AppUser = Depends(require_role("admin", "reviewer")),
) -> ApiResponse[TaskSummaryResponse]:
    return ApiResponse(
        message="查询成功",
        data=await task_monitor_service.get_task_summary(db),
        trace_id=_trace_id(request),
    )


@router.post(
    "/tasks/{review_id}/retry",
    response_model=ApiResponse[TaskRetryResponse],
)
async def retry_task(
    review_id: str,
    request: Request,
    db: AsyncSession = Depends(get_db),
    current_user: AppUser = Depends(require_role("admin")),
) -> ApiResponse[TaskRetryResponse]:
    product = await task_monitor_service.manual_retry(
        db,
        review_id=review_id,
        user_id=int(current_user.id),
        username=str(current_user.username),
        ip_address=request.client.host if request.client else None,
    )
    return ApiResponse(
        message="任务已重试",
        data=TaskRetryResponse(
            review_id=str(product.review_id),
            plot_status=str(product.plot_status),
        ),
        trace_id=_trace_id(request),
    )
