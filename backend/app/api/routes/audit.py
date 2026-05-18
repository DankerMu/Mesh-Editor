from __future__ import annotations

from datetime import datetime

from fastapi import APIRouter, Depends, Query, Request
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.logging import get_trace_id
from app.db.session import get_db
from app.repositories.audit_log_repo import audit_log_repo
from app.schemas.common import ApiResponse
from app.schemas.m6 import AuditAction, AuditLogListResponse, AuditLogResponse


router = APIRouter(prefix="/audit", tags=["Audit"])


def _trace_id(request: Request) -> str:
    return get_trace_id() or str(getattr(request.state, "trace_id", ""))


@router.get("/logs", response_model=ApiResponse[AuditLogListResponse])
async def list_audit_logs(
    request: Request,
    user_id: int | None = Query(None),
    action: AuditAction | None = Query(None),
    resource_type: str | None = Query(None),
    start_date: datetime | None = Query(None),
    end_date: datetime | None = Query(None),
    page: int = Query(1, ge=1),
    page_size: int = Query(20, ge=1, le=100),
    db: AsyncSession = Depends(get_db),
) -> ApiResponse[AuditLogListResponse]:
    logs = await audit_log_repo.list_by_filters(
        db,
        user_id=user_id,
        action=action,
        resource_type=resource_type,
        start_date=start_date,
        end_date=end_date,
        page=page,
        page_size=page_size,
    )
    total = await audit_log_repo.count_by_filters(
        db,
        user_id=user_id,
        action=action,
        resource_type=resource_type,
        start_date=start_date,
        end_date=end_date,
    )
    return ApiResponse(
        message="查询成功",
        data=AuditLogListResponse(
            items=[AuditLogResponse.model_validate(log) for log in logs],
            total=total,
            page=page,
            page_size=page_size,
        ),
        trace_id=_trace_id(request),
    )
