from fastapi import APIRouter, Depends, Request
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.logging import get_trace_id
from app.db.models import AppUser
from app.db.session import get_db
from app.schemas.auth import LoginRequest, LoginResponse
from app.schemas.common import ApiResponse
from app.services.auth_service import auth_service


public_router = APIRouter()
protected_router = APIRouter()


@public_router.post("/auth/login", response_model=ApiResponse[LoginResponse])
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


@protected_router.get("/auth/me", response_model=ApiResponse[dict[str, object]])
async def me(request: Request) -> ApiResponse[dict[str, object]]:
    current_user: AppUser = request.state.current_user
    return ApiResponse(
        data={
            "username": current_user.username,
            "display_name": current_user.display_name,
            "role": current_user.role,
            "is_active": current_user.is_active,
        },
        trace_id=get_trace_id(),
    )
