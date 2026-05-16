from fastapi import APIRouter, Depends, Request
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.dependencies import get_current_user, require_role
from app.core.logging import get_trace_id
from app.db.models import AppUser
from app.db.session import get_db
from app.schemas.auth import LoginRequest, LoginResponse
from app.schemas.common import ApiResponse
from app.services.auth_service import auth_service


router = APIRouter()


@router.post("/auth/login", response_model=ApiResponse[LoginResponse])
async def login(
    payload: LoginRequest,
    request: Request,
    db: AsyncSession = Depends(get_db),
) -> ApiResponse[LoginResponse]:
    result = await auth_service.login(
        db,
        username=payload.username,
        password=payload.password,
        ip_address=request.client.host if request.client else None,
    )
    return ApiResponse(data=result, trace_id=get_trace_id())


@router.get("/auth/me", response_model=ApiResponse[dict[str, object]])
async def me(current_user: AppUser = Depends(get_current_user)) -> ApiResponse[dict[str, object]]:
    return ApiResponse(
        data={
            "username": current_user.username,
            "display_name": current_user.display_name,
            "role": current_user.role,
            "is_active": current_user.is_active,
        },
        trace_id=get_trace_id(),
    )


@router.get("/admin/test", response_model=ApiResponse[dict[str, bool]])
async def admin_test(_: AppUser = Depends(require_role("admin"))) -> ApiResponse[dict[str, bool]]:
    return ApiResponse(data={"admin": True}, trace_id=get_trace_id())
