from __future__ import annotations

import json
from typing import Any

from fastapi import APIRouter, Depends, Query, Request, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.exc import IntegrityError

from app.api.dependencies import require_role
from app.core.error_registry import get_error
from app.core.errors import DomainError
from app.core.logging import get_trace_id
from app.db.models import AppUser, AuditLog
from app.db.session import get_db
from app.repositories.user_repo import UserRepository
from app.schemas.common import ApiResponse
from app.schemas.m6 import (
    UserCreateRequest,
    UserListResponse,
    UserResponse,
    UserRole,
    UserUpdateRequest,
)
from app.services.auth_service import pwd_context


router = APIRouter(prefix="/users", tags=["Users"])
user_repo = UserRepository()


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


def _user_response(user: AppUser) -> UserResponse:
    return UserResponse(
        id=int(user.id),
        username=str(user.username),
        display_name=str(user.display_name),
        role=str(user.role),
        is_active=bool(user.is_active),
        created_at=user.created_at,
        updated_at=user.updated_at,
        last_login_at=user.last_login_at,
    )


async def _write_user_audit(
    db: AsyncSession,
    *,
    actor: AppUser,
    target_user: AppUser,
    operation: str,
    changes: dict[str, object] | None = None,
    request: Request,
) -> None:
    detail: dict[str, object] = {
        "operation": operation,
        "target_user_id": int(target_user.id),
        "target_username": str(target_user.username),
    }
    if changes:
        detail["changes"] = changes
    db.add(
        AuditLog(
            user_id=int(actor.id),
            username=str(actor.username),
            action="user_manage",
            resource_type="user",
            resource_id=str(target_user.id),
            detail_json=json.dumps(detail, ensure_ascii=False),
            ip_address=request.client.host if request.client else None,
        )
    )
    await db.flush()


@router.get("", response_model=ApiResponse[UserListResponse])
async def list_users(
    request: Request,
    role: UserRole | None = Query(None),
    is_active: bool | None = Query(None),
    page: int = Query(1, ge=1),
    page_size: int = Query(20, ge=1, le=100),
    db: AsyncSession = Depends(get_db),
    _: AppUser = Depends(require_role("admin")),
) -> ApiResponse[UserListResponse]:
    users = await user_repo.list_all(
        db,
        role=role,
        is_active=is_active,
        page=page,
        page_size=page_size,
    )
    total = await user_repo.count(db, role=role, is_active=is_active)
    return ApiResponse(
        message="查询成功",
        data=UserListResponse(
            items=[_user_response(user) for user in users],
            total=total,
            page=page,
            page_size=page_size,
        ),
        trace_id=_trace_id(request),
    )


@router.post(
    "",
    response_model=ApiResponse[UserResponse],
    status_code=status.HTTP_201_CREATED,
)
async def create_user(
    payload: UserCreateRequest,
    request: Request,
    db: AsyncSession = Depends(get_db),
    current_user: AppUser = Depends(require_role("admin")),
) -> ApiResponse[UserResponse]:
    existing = await user_repo.get_by_username(db, payload.username)
    if existing is not None:
        raise _domain_error("USER_ALREADY_EXISTS", {"username": payload.username})

    try:
        user = await user_repo.create(
            db,
            username=payload.username,
            password_hash=pwd_context.hash(payload.password),
            display_name=payload.display_name,
            role=payload.role,
        )
        await _write_user_audit(
            db,
            actor=current_user,
            target_user=user,
            operation="create",
            changes={
                "username": payload.username,
                "display_name": payload.display_name,
                "role": payload.role,
                "is_active": True,
            },
            request=request,
        )
        await db.commit()
    except IntegrityError as exc:
        await db.rollback()
        raise _domain_error(
            "USER_ALREADY_EXISTS", {"username": payload.username}
        ) from exc
    await db.refresh(user)
    return ApiResponse(
        message="用户创建成功",
        data=_user_response(user),
        trace_id=_trace_id(request),
    )


@router.put("/{user_id}", response_model=ApiResponse[UserResponse])
async def update_user(
    user_id: int,
    payload: UserUpdateRequest,
    request: Request,
    db: AsyncSession = Depends(get_db),
    current_user: AppUser = Depends(require_role("admin")),
) -> ApiResponse[UserResponse]:
    user = await user_repo.get_by_id(db, user_id)
    if user is None:
        raise _domain_error("USER_NOT_FOUND", {"user_id": user_id})

    changes = payload.model_dump(exclude_unset=True)
    if changes:
        user = await user_repo.update(db, user, **changes)

    operation = "disable" if changes.get("is_active") is False else "update"
    await _write_user_audit(
        db,
        actor=current_user,
        target_user=user,
        operation=operation,
        changes=changes,
        request=request,
    )
    await db.commit()
    await db.refresh(user)
    return ApiResponse(
        message="用户更新成功",
        data=_user_response(user),
        trace_id=_trace_id(request),
    )
