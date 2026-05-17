from collections.abc import Callable

from fastapi import Depends, Request
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker

from app.core.errors import DomainError
from app.db.models import AppUser
from app.db.session import async_session_factory, get_db
from app.services.auth_service import auth_service


async def get_current_user(
    request: Request, db: AsyncSession = Depends(get_db)
) -> AppUser:
    authorization = request.headers.get("Authorization")
    if authorization is None:
        raise DomainError(code="AUTH_REQUIRED", message="需要登录认证", http_status=401)

    scheme, _, token = authorization.partition(" ")
    if scheme != "Bearer" or not token:
        raise DomainError(code="AUTH_REQUIRED", message="需要登录认证", http_status=401)

    user = await auth_service.verify_token(db, token)
    request.state.current_user = user
    return user


def require_role(*roles: str) -> Callable[..., object]:
    async def role_dependency(
        current_user: AppUser = Depends(get_current_user),
    ) -> AppUser:
        if current_user.role not in roles:
            raise DomainError(
                code="PERMISSION_DENIED", message="权限不足", http_status=403
            )
        return current_user

    return role_dependency


def get_session_factory() -> async_sessionmaker[AsyncSession]:
    return async_session_factory
