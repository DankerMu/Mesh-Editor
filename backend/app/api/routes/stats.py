from __future__ import annotations

from datetime import date
from typing import Any

from fastapi import APIRouter, Depends, Query, Request
from fastapi.responses import StreamingResponse
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.error_registry import get_error
from app.core.errors import DomainError
from app.core.logging import get_trace_id
from app.db.session import get_db
from app.schemas.common import ApiResponse
from app.schemas.m6 import (
    OperationStatsResponse,
    PtypeTransitionStatsResponse,
    StatsExportRequest,
)
from app.services.stats_service import stats_service

router = APIRouter(prefix="/stats", tags=["Stats"])


def _trace_id(request: Request) -> str:
    return get_trace_id() or str(getattr(request.state, "trace_id", ""))


def _domain_error(code: str, detail: dict[str, Any] | None = None) -> DomainError:
    message, http_status = get_error(code)
    return DomainError(
        code=code,
        message=message,
        detail=detail or {},
        http_status=http_status,
    )


def _validate_date_range(start_date: date, end_date: date) -> None:
    days = (end_date - start_date).days
    if days < 0:
        raise _domain_error(
            "VALIDATION_ERROR",
            {"start_date": start_date.isoformat(), "end_date": end_date.isoformat()},
        )
    if days > 365:
        raise _domain_error(
            "STATS_DATE_RANGE_EXCEEDED",
            {"max_days": 365, "actual_days": days},
        )


@router.get("/operations", response_model=ApiResponse[OperationStatsResponse])
async def get_operation_stats(
    request: Request,
    start_date: date = Query(...),
    end_date: date = Query(...),
    user_id: int | None = Query(None),
    window_id: str | None = Query(None),
    accum_hours: int | None = Query(None),
    db: AsyncSession = Depends(get_db),
) -> ApiResponse[OperationStatsResponse]:
    _validate_date_range(start_date, end_date)
    return ApiResponse(
        message="查询成功",
        data=await stats_service.get_operation_stats(
            db,
            start_date=start_date,
            end_date=end_date,
            user_id=user_id,
            window_id=window_id,
            accum_hours=accum_hours,
        ),
        trace_id=_trace_id(request),
    )


@router.get(
    "/ptype-transitions",
    response_model=ApiResponse[PtypeTransitionStatsResponse],
)
async def get_ptype_transition_stats(
    request: Request,
    start_date: date = Query(...),
    end_date: date = Query(...),
    user_id: int | None = Query(None),
    window_id: str | None = Query(None),
    db: AsyncSession = Depends(get_db),
) -> ApiResponse[PtypeTransitionStatsResponse]:
    _validate_date_range(start_date, end_date)
    return ApiResponse(
        message="查询成功",
        data=await stats_service.get_ptype_transition_stats(
            db,
            start_date=start_date,
            end_date=end_date,
            user_id=user_id,
            window_id=window_id,
        ),
        trace_id=_trace_id(request),
    )


@router.post("/export")
async def export_stats(
    payload: StatsExportRequest,
    db: AsyncSession = Depends(get_db),
) -> StreamingResponse:
    _validate_date_range(payload.start_date, payload.end_date)
    result = await stats_service.export_stats_csv(
        db,
        start_date=payload.start_date,
        end_date=payload.end_date,
        include=list(payload.include),
    )
    return StreamingResponse(
        iter([result.content]),
        media_type=result.media_type,
        headers={
            "Content-Disposition": f'attachment; filename="{result.filename}"',
        },
    )
