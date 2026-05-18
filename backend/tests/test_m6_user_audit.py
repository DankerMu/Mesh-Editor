import asyncio
import json
from collections.abc import AsyncIterator, Iterator
from datetime import datetime, timedelta
from importlib import import_module
from pathlib import Path
from typing import NamedTuple

import pytest
import sqlalchemy as sa
from alembic import command
from alembic.config import Config
from fastapi.testclient import TestClient
from passlib.context import CryptContext
from pydantic import ValidationError
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine
from sqlalchemy.pool import StaticPool

from app.core.error_registry import ERROR_INFO
from app.core.security import create_access_token, get_jwt_secret
from app.db.models import AppUser, AuditLog, Base, ConfigSnapshot
from app.db.session import get_db
from app.main import app
from app.repositories.audit_log_repo import AuditLogRepository
from app.repositories.user_repo import UserRepository
from app.schemas.m6 import UserCreateRequest


BACKEND_ROOT = Path(__file__).resolve().parents[1]
pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")


class M6Client(NamedTuple):
    client: TestClient
    session_factory: async_sessionmaker[AsyncSession]
    tokens: dict[str, str]


def _migration_config(db_path: Path) -> Config:
    cfg = Config(str(BACKEND_ROOT / "alembic.ini"))
    cfg.set_main_option("script_location", str(BACKEND_ROOT / "app/db/migrations"))
    cfg.set_main_option("sqlalchemy.url", f"sqlite+aiosqlite:///{db_path}")
    return cfg


async def _seed_users(session_factory: async_sessionmaker[AsyncSession]) -> None:
    base_time = datetime(2026, 5, 18, 8, 0, 0)
    async with session_factory() as db:
        db.add_all(
            [
                AppUser(
                    username="admin",
                    password_hash=pwd_context.hash("admin123"),
                    display_name="系统管理员",
                    role="admin",
                    is_active=True,
                    created_at=base_time,
                    updated_at=base_time,
                ),
                AppUser(
                    username="viewer",
                    password_hash=pwd_context.hash("viewer123"),
                    display_name="查看员",
                    role="viewer",
                    is_active=True,
                    created_at=base_time + timedelta(minutes=1),
                    updated_at=base_time + timedelta(minutes=1),
                ),
                AppUser(
                    username="forecaster",
                    password_hash=pwd_context.hash("forecaster123"),
                    display_name="预报员",
                    role="forecaster",
                    is_active=True,
                    created_at=base_time + timedelta(minutes=2),
                    updated_at=base_time + timedelta(minutes=2),
                ),
                AppUser(
                    username="inactive_viewer",
                    password_hash=pwd_context.hash("viewer123"),
                    display_name="禁用查看员",
                    role="viewer",
                    is_active=False,
                    created_at=base_time + timedelta(minutes=3),
                    updated_at=base_time + timedelta(minutes=3),
                ),
            ]
        )
        await db.commit()


async def _user_id(
    session_factory: async_sessionmaker[AsyncSession], username: str
) -> int:
    async with session_factory() as db:
        result = await db.execute(select(AppUser.id).where(AppUser.username == username))
        return int(result.scalar_one())


async def _audit_logs(
    session_factory: async_sessionmaker[AsyncSession],
    action: str | None = None,
) -> list[AuditLog]:
    async with session_factory() as db:
        query = select(AuditLog)
        if action is not None:
            query = query.where(AuditLog.action == action)
        result = await db.execute(query.order_by(AuditLog.id.asc()))
        return list(result.scalars().all())


async def _seed_audit_logs(
    session_factory: async_sessionmaker[AsyncSession],
) -> dict[str, int]:
    admin_id = await _user_id(session_factory, "admin")
    viewer_id = await _user_id(session_factory, "viewer")
    forecaster_id = await _user_id(session_factory, "forecaster")
    base_time = datetime(2026, 5, 18, 9, 0, 0)
    async with session_factory() as db:
        db.add_all(
            [
                AuditLog(
                    user_id=admin_id,
                    username="admin",
                    action="user_manage",
                    resource_type="user",
                    resource_id="2",
                    detail_json=json.dumps({"operation": "update"}),
                    created_at=base_time,
                ),
                AuditLog(
                    user_id=viewer_id,
                    username="viewer",
                    action="login",
                    resource_type="auth",
                    resource_id="viewer",
                    detail_json=json.dumps({"result": "success"}),
                    created_at=base_time + timedelta(minutes=1),
                ),
                AuditLog(
                    user_id=forecaster_id,
                    username="forecaster",
                    action="save",
                    resource_type="version",
                    resource_id="v001",
                    detail_json=json.dumps({"result": "success"}),
                    created_at=base_time + timedelta(minutes=2),
                ),
                AuditLog(
                    user_id=forecaster_id,
                    username="forecaster",
                    action="save",
                    resource_type="version",
                    resource_id="v002",
                    detail_json=json.dumps({"result": "success"}),
                    created_at=base_time + timedelta(minutes=3),
                ),
            ]
        )
        await db.commit()
    return {"admin": admin_id, "viewer": viewer_id, "forecaster": forecaster_id}


def _bearer(token: str) -> dict[str, str]:
    return {"Authorization": f"Bearer {token}"}


@pytest.fixture()
def m6_client() -> Iterator[M6Client]:
    engine = create_async_engine(
        "sqlite+aiosqlite://",
        connect_args={"check_same_thread": False},
        poolclass=StaticPool,
    )
    session_factory = async_sessionmaker(
        engine, class_=AsyncSession, expire_on_commit=False
    )

    async def setup_database() -> None:
        async with engine.begin() as connection:
            await connection.run_sync(Base.metadata.create_all)
        await _seed_users(session_factory)

    asyncio.run(setup_database())

    async def override_get_db() -> AsyncIterator[AsyncSession]:
        async with session_factory() as session:
            yield session

    app.dependency_overrides[get_db] = override_get_db
    tokens = {
        "admin": create_access_token(
            user_id=asyncio.run(_user_id(session_factory, "admin")),
            username="admin",
            role="admin",
            secret=get_jwt_secret(),
        )[0],
        "viewer": create_access_token(
            user_id=asyncio.run(_user_id(session_factory, "viewer")),
            username="viewer",
            role="viewer",
            secret=get_jwt_secret(),
        )[0],
        "forecaster": create_access_token(
            user_id=asyncio.run(_user_id(session_factory, "forecaster")),
            username="forecaster",
            role="forecaster",
            secret=get_jwt_secret(),
        )[0],
    }

    with TestClient(app, raise_server_exceptions=False) as client:
        yield M6Client(client=client, session_factory=session_factory, tokens=tokens)

    app.dependency_overrides.pop(get_db, None)
    asyncio.run(engine.dispose())


def test_1_t1_config_snapshot_model_table_and_columns() -> None:
    assert ConfigSnapshot.__tablename__ == "config_snapshot"
    columns = ConfigSnapshot.__table__.c
    assert isinstance(columns.snapshot_id.type, sa.String)
    assert columns.snapshot_id.primary_key
    assert isinstance(columns.config_type.type, sa.String)
    assert not columns.config_type.nullable
    assert isinstance(columns.config_json.type, sa.Text)
    assert not columns.config_json.nullable
    assert isinstance(columns.changed_by.type, sa.String)
    assert columns.changed_by.nullable
    assert isinstance(columns.created_at.type, sa.DateTime)
    assert not columns.created_at.nullable


def test_1_t2_migration_v009_creates_config_snapshot_table(tmp_path: Path) -> None:
    migration = import_module("app.db.migrations.versions.v009_m6_config_snapshot")
    assert migration.revision == "v009"
    assert migration.down_revision == "v008"

    db_path = tmp_path / "m6.db"
    command.upgrade(_migration_config(db_path), "head")

    engine = sa.create_engine(f"sqlite:///{db_path}")
    try:
        inspector = sa.inspect(engine)
        assert "config_snapshot" in inspector.get_table_names()
        columns = {
            column["name"]: column
            for column in inspector.get_columns("config_snapshot")
        }
        assert set(columns) == {
            "snapshot_id",
            "config_type",
            "config_json",
            "changed_by",
            "created_at",
        }
        assert columns["snapshot_id"]["primary_key"] == 1
        assert not columns["config_type"]["nullable"]
        assert not columns["config_json"]["nullable"]
        assert not columns["created_at"]["nullable"]

        app_user_columns = {
            column["name"]: column for column in inspector.get_columns("app_user")
        }
        assert "last_login_at" in app_user_columns
        assert isinstance(app_user_columns["last_login_at"]["type"], sa.DateTime)
        assert app_user_columns["last_login_at"]["nullable"]
    finally:
        engine.dispose()


def test_1_t3_m6_error_codes_registered() -> None:
    assert ERROR_INFO["USER_NOT_FOUND"].http_status == 404
    assert ERROR_INFO["USER_NOT_FOUND"].message == "用户未找到"
    assert ERROR_INFO["USER_ALREADY_EXISTS"].http_status == 409
    assert ERROR_INFO["USER_ALREADY_EXISTS"].message == "用户名已存在"
    assert ERROR_INFO["STATS_DATE_RANGE_EXCEEDED"].http_status == 400
    assert ERROR_INFO["TASK_NOT_RETRYABLE"].http_status == 409


def test_1_t4_user_create_request_validation() -> None:
    payload = UserCreateRequest.model_validate(
        {
            "username": "zhangsan",
            "display_name": "张三",
            "role": "forecaster",
            "password": "secure123",
        }
    )
    assert payload.username == "zhangsan"

    with pytest.raises(ValidationError):
        UserCreateRequest.model_validate(
            {
                "username": "ab",
                "display_name": "张三",
                "role": "forecaster",
                "password": "secure123",
            }
        )
    with pytest.raises(ValidationError):
        UserCreateRequest.model_validate(
            {
                "username": "zhangsan",
                "display_name": "张三",
                "role": "superadmin",
                "password": "secure123",
            }
        )
    with pytest.raises(ValidationError):
        UserCreateRequest.model_validate(
            {
                "username": "zhangsan",
                "display_name": "张三",
                "role": "forecaster",
                "password": "short",
            }
        )


async def test_2_t1_user_repository_list_all_paginates_and_filters(
    m6_client: M6Client,
) -> None:
    repo = UserRepository()
    async with m6_client.session_factory() as db:
        first_page = await repo.list_all(db, page=1, page_size=2)
        active_viewers = await repo.list_all(
            db, role="viewer", is_active=True, page=1, page_size=20
        )

    assert [user.username for user in first_page] == ["inactive_viewer", "forecaster"]
    assert [user.username for user in active_viewers] == ["viewer"]


async def test_2_t2_user_repository_count_with_filters(m6_client: M6Client) -> None:
    repo = UserRepository()
    async with m6_client.session_factory() as db:
        total = await repo.count(db)
        active_viewers = await repo.count(db, role="viewer", is_active=True)
        inactive_users = await repo.count(db, is_active=False)

    assert total == 4
    assert active_viewers == 1
    assert inactive_users == 1


def test_2_t3_post_users_admin_creates_user_without_password_hash(
    m6_client: M6Client,
) -> None:
    response = m6_client.client.post(
        "/api/users",
        headers=_bearer(m6_client.tokens["admin"]),
        json={
            "username": "zhangsan",
            "display_name": "张三",
            "role": "forecaster",
            "password": "secure123",
        },
    )

    body = response.json()
    assert response.status_code == 201
    assert body["message"] == "用户创建成功"
    assert body["data"]["username"] == "zhangsan"
    assert body["data"]["role"] == "forecaster"
    assert body["data"]["is_active"] is True
    assert "last_login_at" in body["data"]
    assert body["data"]["last_login_at"] is None
    assert "password_hash" not in json.dumps(body, ensure_ascii=False)
    logs = asyncio.run(_audit_logs(m6_client.session_factory, "user_manage"))
    assert len(logs) == 1
    assert json.loads(str(logs[0].detail_json))["operation"] == "create"


def test_2_t4_post_users_duplicate_username_returns_409(m6_client: M6Client) -> None:
    response = m6_client.client.post(
        "/api/users",
        headers=_bearer(m6_client.tokens["admin"]),
        json={
            "username": "viewer",
            "display_name": "重复用户",
            "role": "viewer",
            "password": "secure123",
        },
    )

    assert response.status_code == 409
    assert response.json()["code"] == "USER_ALREADY_EXISTS"


def test_2_t5_post_users_viewer_denied(m6_client: M6Client) -> None:
    response = m6_client.client.post(
        "/api/users",
        headers=_bearer(m6_client.tokens["viewer"]),
        json={
            "username": "lisi",
            "display_name": "李四",
            "role": "viewer",
            "password": "secure123",
        },
    )

    assert response.status_code == 403
    assert response.json()["code"] == "PERMISSION_DENIED"


def test_2_t5b_post_users_duplicate_username_integrity_error_returns_409(
    m6_client: M6Client,
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    from app.api.routes import users as users_route

    async def missing_precheck(*args: object, **kwargs: object) -> None:
        return None

    monkeypatch.setattr(users_route.user_repo, "get_by_username", missing_precheck)
    response = m6_client.client.post(
        "/api/users",
        headers=_bearer(m6_client.tokens["admin"]),
        json={
            "username": "viewer",
            "display_name": "竞态重复用户",
            "role": "viewer",
            "password": "secure123",
        },
    )

    assert response.status_code == 409
    assert response.json()["code"] == "USER_ALREADY_EXISTS"


def test_2_t6_put_users_admin_disables_user_and_writes_audit(
    m6_client: M6Client,
) -> None:
    user_id = asyncio.run(_user_id(m6_client.session_factory, "forecaster"))
    response = m6_client.client.put(
        f"/api/users/{user_id}",
        headers=_bearer(m6_client.tokens["admin"]),
        json={"is_active": False},
    )

    assert response.status_code == 200
    assert response.json()["data"]["is_active"] is False
    logs = asyncio.run(_audit_logs(m6_client.session_factory, "user_manage"))
    assert len(logs) == 1
    detail = json.loads(str(logs[0].detail_json))
    assert detail["operation"] == "disable"
    assert detail["target_user_id"] == user_id


def test_2_t7_put_users_updates_role_from_forecaster_to_reviewer(
    m6_client: M6Client,
) -> None:
    user_id = asyncio.run(_user_id(m6_client.session_factory, "forecaster"))
    response = m6_client.client.put(
        f"/api/users/{user_id}",
        headers=_bearer(m6_client.tokens["admin"]),
        json={"role": "reviewer"},
    )

    assert response.status_code == 200
    assert response.json()["data"]["role"] == "reviewer"


def test_2_t8_get_users_pagination_and_role_filter(m6_client: M6Client) -> None:
    response = m6_client.client.get(
        "/api/users?role=viewer&page=1&page_size=1",
        headers=_bearer(m6_client.tokens["admin"]),
    )

    body = response.json()
    assert response.status_code == 200
    assert body["data"]["total"] == 2
    assert body["data"]["page"] == 1
    assert body["data"]["page_size"] == 1
    assert len(body["data"]["items"]) == 1
    assert body["data"]["items"][0]["role"] == "viewer"
    assert "password_hash" not in json.dumps(body, ensure_ascii=False)


def test_2_t9_put_users_missing_user_returns_404(m6_client: M6Client) -> None:
    response = m6_client.client.put(
        "/api/users/9999",
        headers=_bearer(m6_client.tokens["admin"]),
        json={"display_name": "不存在"},
    )

    assert response.status_code == 404
    assert response.json()["code"] == "USER_NOT_FOUND"


async def test_3_t1_audit_log_repository_list_by_filters_paginates(
    m6_client: M6Client,
) -> None:
    ids = await _seed_audit_logs(m6_client.session_factory)
    repo = AuditLogRepository()
    async with m6_client.session_factory() as db:
        logs = await repo.list_by_filters(
            db,
            user_id=ids["forecaster"],
            action="save",
            page=1,
            page_size=1,
        )

    assert len(logs) == 1
    assert logs[0].resource_id == "v002"


async def test_3_t2_audit_log_repository_count_by_filters(
    m6_client: M6Client,
) -> None:
    ids = await _seed_audit_logs(m6_client.session_factory)
    repo = AuditLogRepository()
    async with m6_client.session_factory() as db:
        count = await repo.count_by_filters(
            db,
            user_id=ids["forecaster"],
            action="save",
            resource_type="version",
        )

    assert count == 2


def test_3_t3_get_audit_logs_filter_by_user_id(m6_client: M6Client) -> None:
    ids = asyncio.run(_seed_audit_logs(m6_client.session_factory))
    response = m6_client.client.get(
        f"/api/audit/logs?user_id={ids['forecaster']}",
        headers=_bearer(m6_client.tokens["admin"]),
    )

    body = response.json()
    assert response.status_code == 200
    assert body["data"]["total"] == 2
    assert {item["username"] for item in body["data"]["items"]} == {"forecaster"}


def test_3_t4_get_audit_logs_filter_by_action_and_time_range(
    m6_client: M6Client,
) -> None:
    asyncio.run(_seed_audit_logs(m6_client.session_factory))
    response = m6_client.client.get(
        "/api/audit/logs",
        headers=_bearer(m6_client.tokens["admin"]),
        params={
            "action": "save",
            "start_date": "2026-05-18T09:02:00",
            "end_date": "2026-05-18T09:03:00",
        },
    )

    body = response.json()
    assert response.status_code == 200
    assert body["data"]["total"] == 1
    assert body["data"]["items"][0]["resource_id"] == "v001"


def test_3_t5_get_audit_logs_empty_result(m6_client: M6Client) -> None:
    asyncio.run(_seed_audit_logs(m6_client.session_factory))
    response = m6_client.client.get(
        "/api/audit/logs?action=release&resource_type=missing",
        headers=_bearer(m6_client.tokens["admin"]),
    )

    body = response.json()
    assert response.status_code == 200
    assert body["data"] == {"items": [], "total": 0, "page": 1, "page_size": 20}


def test_3_t6_get_audit_logs_viewer_can_access(m6_client: M6Client) -> None:
    asyncio.run(_seed_audit_logs(m6_client.session_factory))
    response = m6_client.client.get(
        "/api/audit/logs",
        headers=_bearer(m6_client.tokens["viewer"]),
    )

    assert response.status_code == 200
    assert response.json()["data"]["total"] == 4


def test_3_t7_get_audit_logs_invalid_action_returns_422(
    m6_client: M6Client,
) -> None:
    response = m6_client.client.get(
        "/api/audit/logs?action=delete_everything",
        headers=_bearer(m6_client.tokens["viewer"]),
    )

    assert response.status_code == 422
    assert response.json()["code"] == "VALIDATION_ERROR"


def test_3_t8_get_audit_logs_accepts_version_action_names(
    m6_client: M6Client,
) -> None:
    for action in [
        "version_save",
        "version_submit",
        "version_review",
        "version_release",
    ]:
        response = m6_client.client.get(
            f"/api/audit/logs?action={action}",
            headers=_bearer(m6_client.tokens["viewer"]),
        )

        assert response.status_code == 200
        assert response.json()["data"]["total"] == 0
