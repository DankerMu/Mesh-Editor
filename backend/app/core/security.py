import os
from datetime import UTC, datetime, timedelta
from typing import Any

import jwt

from app.core.errors import DomainError


DEFAULT_JWT_SECRET = "mesh-editor-dev-secret"
DEFAULT_JWT_EXPIRE_MINUTES = 480
JWT_ALGORITHM = "HS256"


def get_jwt_secret() -> str:
    return os.getenv("JWT_SECRET", DEFAULT_JWT_SECRET)


def get_jwt_expire_minutes() -> int:
    raw_value = os.getenv("JWT_EXPIRE_MINUTES")
    if raw_value is None:
        return DEFAULT_JWT_EXPIRE_MINUTES
    try:
        return int(raw_value)
    except ValueError:
        return DEFAULT_JWT_EXPIRE_MINUTES


def create_access_token(
    user_id: int,
    username: str,
    role: str,
    secret: str,
    expires_minutes: int = DEFAULT_JWT_EXPIRE_MINUTES,
) -> tuple[str, datetime]:
    expires_at = datetime.now(UTC) + timedelta(minutes=expires_minutes)
    payload: dict[str, Any] = {
        "sub": str(user_id),
        "username": username,
        "role": role,
        "exp": expires_at,
    }
    token = jwt.encode(payload, secret, algorithm=JWT_ALGORITHM)
    return token, expires_at


def decode_access_token(token: str, secret: str) -> dict[str, Any]:
    try:
        payload = jwt.decode(token, secret, algorithms=[JWT_ALGORITHM])
    except jwt.ExpiredSignatureError as exc:
        raise DomainError(
            code="TOKEN_EXPIRED",
            message="登录已过期",
            http_status=401,
        ) from exc
    except jwt.PyJWTError as exc:
        raise DomainError(
            code="AUTH_REQUIRED",
            message="需要登录认证",
            http_status=401,
        ) from exc
    return payload
