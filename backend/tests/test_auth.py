import asyncio
import json
from collections.abc import AsyncIterator, Iterator
from datetime import UTC, datetime, timedelta
from pathlib import Path
from typing import NamedTuple

import jwt
import pytest
import sqlalchemy as sa
from alembic import command
from alembic.config import Config
from fastapi import APIRouter, Depends, Request
from fastapi.testclient import TestClient
from passlib.context import CryptContext
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine
from sqlalchemy.pool import StaticPool

from app.api.dependencies import require_role
from app.core.logging import get_trace_id
from app.core.security import create_access_token, get_jwt_secret
from app.db.models import AppUser, AuditLog, Base
from app.db.session import get_db
from app.main import api_router, app
from app.schemas.common import ApiResponse


BACKEND_ROOT = Path(__file__).resolve().parents[1]
pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")


class AuthClient(NamedTuple):
    client: TestClient
    session_factory: async_sessionmaker[AsyncSession]


async def seed_auth_users(session_factory: async_sessionmaker[AsyncSession]) -> None:
    async with session_factory() as db:
        db.add_all(
            [
                AppUser(
                    username="admin",
                    password_hash=pwd_context.hash("admin123"),
                    display_name="系统管理员",
                    role="admin",
                    is_active=True,
                ),
                AppUser(
                    username="viewer",
                    password_hash=pwd_context.hash("viewer123"),
                    display_name="查看员",
                    role="viewer",
                    is_active=True,
                ),
                AppUser(
                    username="disabled",
                    password_hash=pwd_context.hash("disabled123"),
                    display_name="禁用预报员",
                    role="forecaster",
                    is_active=False,
                ),
            ]
        )
        await db.commit()


@pytest.fixture()
def auth_client() -> Iterator[AuthClient]:
    engine = create_async_engine(
        "sqlite+aiosqlite://",
        connect_args={"check_same_thread": False},
        poolclass=StaticPool,
    )
    session_factory = async_sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)

    async def setup_database() -> None:
        async with engine.begin() as connection:
            await connection.run_sync(Base.metadata.create_all)
        await seed_auth_users(session_factory)

    asyncio.run(setup_database())

    async def override_get_db() -> AsyncIterator[AsyncSession]:
        async with session_factory() as session:
            yield session

    app.dependency_overrides[get_db] = override_get_db

    async def admin_test_route(_: AppUser = Depends(require_role("admin"))) -> ApiResponse[dict[str, bool]]:
        return ApiResponse(data={"admin": True}, trace_id=get_trace_id())

    include_test_route("/admin/test", admin_test_route)
    with TestClient(app, raise_server_exceptions=False) as client:
        yield AuthClient(client=client, session_factory=session_factory)

    remove_test_routes()
    app.dependency_overrides.pop(get_db, None)
    asyncio.run(engine.dispose())


def login(client: TestClient, username: str, password: str) -> dict:
    response = client.post("/api/auth/login", json={"username": username, "password": password})
    return response.json()


async def get_user_id(session_factory: async_sessionmaker[AsyncSession], username: str) -> int:
    async with session_factory() as db:
        result = await db.execute(select(AppUser.id).where(AppUser.username == username))
        user_id = result.scalar_one()
        return int(user_id)


async def count_audit_logs(
    session_factory: async_sessionmaker[AsyncSession],
    username: str,
    action: str,
) -> int:
    async with session_factory() as db:
        result = await db.execute(
            select(sa.func.count(AuditLog.id)).where(
                AuditLog.username == username,
                AuditLog.action == action,
            )
        )
        return int(result.scalar_one())


async def get_audit_detail(
    session_factory: async_sessionmaker[AsyncSession],
    username: str,
    action: str,
) -> dict[str, object]:
    async with session_factory() as db:
        result = await db.execute(
            select(AuditLog.detail_json)
            .where(
                AuditLog.username == username,
                AuditLog.action == action,
            )
            .order_by(AuditLog.id.desc())
        )
        detail_json = result.scalar_one()
        return json.loads(str(detail_json))


def bearer(token: str) -> dict[str, str]:
    return {"Authorization": f"Bearer {token}"}


def include_test_route(path: str, endpoint: object) -> None:
    router = APIRouter()
    router.add_api_route(path, endpoint, methods=["GET"])
    before = list(app.router.routes)
    app.include_router(router, prefix=api_router.prefix, dependencies=api_router.dependencies)
    app.state.test_routes = getattr(app.state, "test_routes", []) + [
        route for route in app.router.routes if route not in before
    ]
    app.openapi_schema = None


def remove_test_routes() -> None:
    for route in getattr(app.state, "test_routes", []):
        if route in app.router.routes:
            app.router.routes.remove(route)
    app.state.test_routes = []
    app.openapi_schema = None


def test_t4_1_login_success_returns_token(auth_client: AuthClient) -> None:
    response = auth_client.client.post(
        "/api/auth/login",
        json={"username": "admin", "password": "admin123"},
    )

    assert response.status_code == 200
    body = response.json()
    assert body["code"] == "OK"
    assert body["data"]["user_id"] == "1"
    assert body["data"]["username"] == "admin"
    assert body["data"]["display_name"] == "系统管理员"
    assert body["data"]["role"] == "admin"
    assert body["data"]["token"]
    assert body["data"]["expires_at"]


def test_t4_2_login_wrong_password_returns_auth_required(auth_client: AuthClient) -> None:
    response = auth_client.client.post(
        "/api/auth/login",
        json={"username": "admin", "password": "wrong"},
    )

    assert response.status_code == 401
    assert response.json()["code"] == "AUTH_REQUIRED"


def test_t4_3_login_unknown_user_returns_auth_required(auth_client: AuthClient) -> None:
    response = auth_client.client.post(
        "/api/auth/login",
        json={"username": "missing", "password": "admin123"},
    )

    assert response.status_code == 401
    assert response.json()["code"] == "AUTH_REQUIRED"


def test_t4_4_me_with_valid_token_returns_current_user(auth_client: AuthClient) -> None:
    token = login(auth_client.client, "viewer", "viewer123")["data"]["token"]

    response = auth_client.client.get("/api/auth/me", headers=bearer(token))

    assert response.status_code == 200
    assert response.json()["data"] == {
        "username": "viewer",
        "display_name": "查看员",
        "role": "viewer",
        "is_active": True,
    }


def test_t4_5_me_without_token_returns_auth_required(auth_client: AuthClient) -> None:
    response = auth_client.client.get("/api/auth/me")

    assert response.status_code == 401
    assert response.json()["code"] == "AUTH_REQUIRED"


def test_t4_6_me_with_expired_token_returns_token_expired(auth_client: AuthClient) -> None:
    user_id = asyncio.run(get_user_id(auth_client.session_factory, "admin"))
    token, _ = create_access_token(
        user_id=user_id,
        username="admin",
        role="admin",
        secret=get_jwt_secret(),
        expires_minutes=-1,
    )

    response = auth_client.client.get("/api/auth/me", headers=bearer(token))

    assert response.status_code == 401
    assert response.json()["code"] == "TOKEN_EXPIRED"


def test_t4_7_protected_route_without_token_returns_auth_required(auth_client: AuthClient) -> None:
    response = auth_client.client.get("/api/auth/me")

    assert response.status_code == 401
    assert response.json()["code"] == "AUTH_REQUIRED"


def test_t4_8_health_and_login_are_public(auth_client: AuthClient) -> None:
    health_response = auth_client.client.get("/api/health")
    login_response = auth_client.client.post(
        "/api/auth/login",
        json={"username": "admin", "password": "admin123"},
    )

    assert health_response.status_code == 200
    assert health_response.json()["code"] == "OK"
    assert login_response.status_code == 200
    assert login_response.json()["code"] == "OK"


def test_default_api_router_protects_route_without_explicit_auth(auth_client: AuthClient) -> None:
    async def test_unprotected_route(request: Request) -> ApiResponse[dict[str, str]]:
        current_user: AppUser = request.state.current_user
        return ApiResponse(data={"username": str(current_user.username)}, trace_id=get_trace_id())

    include_test_route("/test-unprotected", test_unprotected_route)
    try:
        unauthenticated_response = auth_client.client.get("/api/test-unprotected")
        token = login(auth_client.client, "viewer", "viewer123")["data"]["token"]
        authenticated_response = auth_client.client.get("/api/test-unprotected", headers=bearer(token))
    finally:
        remove_test_routes()

    assert unauthenticated_response.status_code == 401
    assert unauthenticated_response.json()["code"] == "AUTH_REQUIRED"
    assert authenticated_response.status_code == 200
    assert authenticated_response.json()["data"] == {"username": "viewer"}


def test_t4_9_admin_can_access_admin_route(auth_client: AuthClient) -> None:
    token = login(auth_client.client, "admin", "admin123")["data"]["token"]

    response = auth_client.client.get("/api/admin/test", headers=bearer(token))

    assert response.status_code == 200
    assert response.json()["data"] == {"admin": True}


def test_t4_10_viewer_cannot_access_admin_route(auth_client: AuthClient) -> None:
    token = login(auth_client.client, "viewer", "viewer123")["data"]["token"]

    response = auth_client.client.get("/api/admin/test", headers=bearer(token))

    assert response.status_code == 403
    assert response.json()["code"] == "PERMISSION_DENIED"


def test_t4_11_disabled_user_with_valid_token_returns_user_disabled(auth_client: AuthClient) -> None:
    user_id = asyncio.run(get_user_id(auth_client.session_factory, "disabled"))
    token, _ = create_access_token(
        user_id=user_id,
        username="disabled",
        role="forecaster",
        secret=get_jwt_secret(),
    )

    response = auth_client.client.get("/api/auth/me", headers=bearer(token))

    assert response.status_code == 403
    assert response.json()["code"] == "USER_DISABLED"


def test_token_username_must_match_user_id_subject(auth_client: AuthClient) -> None:
    admin_user_id = asyncio.run(get_user_id(auth_client.session_factory, "admin"))
    token = jwt.encode(
        {
            "sub": str(admin_user_id),
            "username": "viewer",
            "role": "viewer",
            "exp": datetime.now(UTC) + timedelta(minutes=5),
        },
        get_jwt_secret(),
        algorithm="HS256",
    )

    response = auth_client.client.get("/api/auth/me", headers=bearer(token))

    assert response.status_code == 401
    assert response.json()["code"] == "AUTH_REQUIRED"


def test_t4_12_successful_login_writes_audit_log(auth_client: AuthClient) -> None:
    response = auth_client.client.post(
        "/api/auth/login",
        json={"username": "admin", "password": "admin123"},
    )

    assert response.status_code == 200
    count = asyncio.run(count_audit_logs(auth_client.session_factory, "admin", "login"))
    detail = asyncio.run(get_audit_detail(auth_client.session_factory, "admin", "login"))
    assert count == 1
    assert detail["result"] == "success"


def test_t4_13_failed_login_writes_audit_log(auth_client: AuthClient) -> None:
    response = auth_client.client.post(
        "/api/auth/login",
        json={"username": "admin", "password": "wrong"},
    )

    assert response.status_code == 401
    count = asyncio.run(count_audit_logs(auth_client.session_factory, "admin", "login"))
    detail = asyncio.run(get_audit_detail(auth_client.session_factory, "admin", "login"))
    assert count == 1
    assert detail["result"] == "failure"


def test_t4_14_jwt_claims_include_sub_role_and_exp(auth_client: AuthClient) -> None:
    token = login(auth_client.client, "admin", "admin123")["data"]["token"]

    payload = jwt.decode(token, get_jwt_secret(), algorithms=["HS256"])

    assert payload["sub"] == "1"
    assert payload["role"] == "admin"
    assert "exp" in payload


def test_t4_15_seed_admin_user_can_login_through_migration(tmp_path: Path) -> None:
    db_path = tmp_path / "seed_admin.db"
    cfg = Config(str(BACKEND_ROOT / "alembic.ini"))
    cfg.set_main_option("script_location", str(BACKEND_ROOT / "app/db/migrations"))
    cfg.set_main_option("sqlalchemy.url", f"sqlite+aiosqlite:///{db_path}")
    command.upgrade(cfg, "head")

    engine = create_async_engine(f"sqlite+aiosqlite:///{db_path}")
    session_factory = async_sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)

    async def override_get_db() -> AsyncIterator[AsyncSession]:
        async with session_factory() as session:
            yield session

    app.dependency_overrides[get_db] = override_get_db
    with TestClient(app, raise_server_exceptions=False) as client:
        response = client.post(
            "/api/auth/login",
            json={"username": "admin", "password": "admin123"},
        )

    app.dependency_overrides.pop(get_db, None)
    asyncio.run(engine.dispose())

    assert response.status_code == 200
    assert response.json()["data"]["username"] == "admin"
