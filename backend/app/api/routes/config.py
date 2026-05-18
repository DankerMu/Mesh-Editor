from __future__ import annotations

from typing import Any

from fastapi import APIRouter, Body, Depends, Query, Request
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.dependencies import require_role
from app.core.error_registry import get_error
from app.core.errors import DomainError
from app.core.logging import get_trace_id
from app.db.models import AppUser, ConfigSnapshot
from app.db.session import get_db
from app.repositories.config_snapshot_repo import config_snapshot_repo
from app.schemas.common import ApiResponse
from app.schemas.m6 import (
    ConfigHistoryResponse,
    ConfigSnapshotResponse,
    ConfigSnapshotUpdateResponse,
)
from app.services.config_service import CONFIG_FILE_MAP, config_service


router = APIRouter(prefix="/config", tags=["Config"])


def _trace_id(request: Request) -> str:
    return get_trace_id() or str(getattr(request.state, "trace_id", ""))


def _domain_error(
    code: str, detail: dict[str, Any] | None = None, message: str | None = None
) -> DomainError:
    default_message, http_status = get_error(code)
    return DomainError(
        code=code,
        message=message or default_message,
        detail=detail or {},
        http_status=http_status,
    )


def _validate_config_type(config_type: str) -> None:
    if config_type not in CONFIG_FILE_MAP:
        raise _domain_error(
            "VALIDATION_ERROR",
            {"config_type": config_type},
            "不支持的配置类型",
        )


def _snapshot_response(snapshot: ConfigSnapshot) -> ConfigSnapshotResponse:
    return ConfigSnapshotResponse(
        snapshot_id=str(snapshot.snapshot_id),
        config_type=str(snapshot.config_type),
        changed_by=None if snapshot.changed_by is None else str(snapshot.changed_by),
        created_at=snapshot.created_at,  # type: ignore[arg-type]
    )


@router.get("/{config_type}", response_model=ApiResponse[dict[str, Any]])
async def get_config(
    config_type: str,
    request: Request,
    _: AppUser = Depends(require_role("admin")),
) -> ApiResponse[dict[str, Any]]:
    _validate_config_type(config_type)
    return ApiResponse(
        message="查询成功",
        data=config_service.get_config(config_type),
        trace_id=_trace_id(request),
    )


@router.put(
    "/{config_type}", response_model=ApiResponse[ConfigSnapshotUpdateResponse]
)
async def update_config(
    config_type: str,
    request: Request,
    payload: dict[str, Any] = Body(...),
    db: AsyncSession = Depends(get_db),
    current_user: AppUser = Depends(require_role("admin")),
) -> ApiResponse[ConfigSnapshotUpdateResponse]:
    _validate_config_type(config_type)
    snapshot = await config_service.update_config(
        db,
        config_type=config_type,
        config_json=payload,
        user_id=int(current_user.id),
        username=str(current_user.username),
        ip_address=request.client.host if request.client else None,
    )
    return ApiResponse(
        message="配置更新成功",
        data=ConfigSnapshotUpdateResponse(**_snapshot_response(snapshot).model_dump()),
        trace_id=_trace_id(request),
    )


@router.get(
    "/{config_type}/history", response_model=ApiResponse[ConfigHistoryResponse]
)
async def list_config_history(
    config_type: str,
    request: Request,
    limit: int = Query(20, ge=1, le=100),
    db: AsyncSession = Depends(get_db),
    _: AppUser = Depends(require_role("admin")),
) -> ApiResponse[ConfigHistoryResponse]:
    _validate_config_type(config_type)
    snapshots = await config_snapshot_repo.list_by_type(db, config_type, limit=limit)
    return ApiResponse(
        message="查询成功",
        data=ConfigHistoryResponse(
            items=[_snapshot_response(snapshot) for snapshot in snapshots],
            total=len(snapshots),
        ),
        trace_id=_trace_id(request),
    )
