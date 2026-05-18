import json
from datetime import UTC, datetime
from typing import Any

from passlib.context import CryptContext  # type: ignore[import-untyped]
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.errors import DomainError
from app.core.security import (
    create_access_token,
    decode_access_token,
    get_jwt_expire_minutes,
    get_jwt_secret,
)
from app.db.models import AppUser, AuditLog
from app.repositories.user_repo import UserRepository
from app.schemas.auth import LoginResponse


pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")


class AuthService:
    def __init__(self, user_repo: UserRepository | None = None) -> None:
        self.user_repo = user_repo or UserRepository()

    async def login(
        self,
        db: AsyncSession,
        username: str,
        password: str,
        ip_address: str | None = None,
    ) -> LoginResponse:
        user = await self.user_repo.get_by_username(db, username)
        if user is None:
            await self._write_login_audit(
                db,
                username=username,
                action="login",
                reason="user_not_found",
                result="failure",
                ip_address=ip_address,
            )
            raise DomainError(
                code="AUTH_REQUIRED", message="需要登录认证", http_status=401
            )

        if not pwd_context.verify(password, str(user.password_hash)):
            await self._write_login_audit(
                db,
                username=username,
                user_id=int(user.id),
                action="login",
                reason="invalid_password",
                result="failure",
                ip_address=ip_address,
            )
            raise DomainError(
                code="AUTH_REQUIRED", message="需要登录认证", http_status=401
            )

        if not bool(user.is_active):
            await self._write_login_audit(
                db,
                username=username,
                user_id=int(user.id),
                action="login",
                reason="user_disabled",
                result="failure",
                ip_address=ip_address,
            )
            raise DomainError(
                code="USER_DISABLED", message="用户已被禁用", http_status=403
            )

        await self._write_login_audit(
            db,
            username=username,
            user_id=int(user.id),
            action="login",
            reason="success",
            result="success",
            ip_address=ip_address,
        )
        user.last_login_at = datetime.now(UTC)
        db.add(user)
        await db.flush()
        await db.commit()
        token, expires_at = create_access_token(
            user_id=int(user.id),
            username=str(user.username),
            role=str(user.role),
            secret=get_jwt_secret(),
            expires_minutes=get_jwt_expire_minutes(),
        )
        return LoginResponse(
            user_id=str(user.id),
            username=str(user.username),
            display_name=str(user.display_name),
            role=str(user.role),
            token=token,
            expires_at=expires_at,
        )

    async def verify_token(self, db: AsyncSession, token: str) -> AppUser:
        payload = decode_access_token(token, get_jwt_secret())
        subject = payload.get("sub")
        username = payload.get("username")
        if not isinstance(subject, str) or not subject:
            raise DomainError(
                code="AUTH_REQUIRED", message="需要登录认证", http_status=401
            )
        if not isinstance(username, str) or not username:
            raise DomainError(
                code="AUTH_REQUIRED", message="需要登录认证", http_status=401
            )

        try:
            user_id = int(subject)
        except ValueError as exc:
            raise DomainError(
                code="AUTH_REQUIRED", message="需要登录认证", http_status=401
            ) from exc

        user = await self.user_repo.get_by_id(db, user_id)
        if user is None or user.username != username:
            raise DomainError(
                code="AUTH_REQUIRED", message="需要登录认证", http_status=401
            )
        if not bool(user.is_active):
            raise DomainError(
                code="USER_DISABLED", message="用户已被禁用", http_status=403
            )
        return user

    async def _write_login_audit(
        self,
        db: AsyncSession,
        username: str,
        action: str,
        reason: str,
        result: str,
        ip_address: str | None,
        user_id: int | None = None,
    ) -> None:
        detail: dict[str, Any] = {"reason": reason, "result": result}
        db.add(
            AuditLog(
                user_id=user_id,
                username=username,
                action=action,
                resource_type="auth",
                resource_id=username,
                detail_json=json.dumps(detail, ensure_ascii=False),
                ip_address=ip_address,
            )
        )
        await db.commit()


auth_service = AuthService()
