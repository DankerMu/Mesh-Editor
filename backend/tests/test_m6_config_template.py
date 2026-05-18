import asyncio
import json
from collections.abc import AsyncIterator, Iterator
from datetime import datetime, timedelta
from pathlib import Path
from typing import NamedTuple

import pytest
from fastapi.testclient import TestClient
from passlib.context import CryptContext
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine
from sqlalchemy.pool import StaticPool

from app.core.security import create_access_token, get_jwt_secret
from app.db.models import AppUser, AuditLog, Base, ConfigSnapshot
from app.db.session import get_db
from app.main import app
from app.repositories.config_snapshot_repo import ConfigSnapshotRepository
from app.services.config_service import ConfigService


pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")


class M6Client(NamedTuple):
    client: TestClient
    session_factory: async_sessionmaker[AsyncSession]
    tokens: dict[str, str]


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


async def _snapshots(
    session_factory: async_sessionmaker[AsyncSession],
    config_type: str | None = None,
) -> list[ConfigSnapshot]:
    async with session_factory() as db:
        query = select(ConfigSnapshot)
        if config_type is not None:
            query = query.where(ConfigSnapshot.config_type == config_type)
        result = await db.execute(query.order_by(ConfigSnapshot.created_at.desc()))
        return list(result.scalars().all())


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
    }

    with TestClient(app, raise_server_exceptions=False) as client:
        yield M6Client(client=client, session_factory=session_factory, tokens=tokens)

    app.dependency_overrides.pop(get_db, None)
    asyncio.run(engine.dispose())


@pytest.fixture()
def temp_repo_root(tmp_path: Path, monkeypatch: pytest.MonkeyPatch) -> Path:
    root = tmp_path / "repo"
    schemas_dir = root / "schemas"
    schemas_dir.mkdir(parents=True)
    (schemas_dir / "product_config.json").write_text(
        json.dumps({"max_lead_hours": 240, "lead_step_hours": 3}, ensure_ascii=False),
        encoding="utf-8",
    )

    from app.api.routes import config as config_route
    from app.services import review_templates

    monkeypatch.setattr(
        config_route,
        "config_service",
        ConfigService(repo_root=root),
    )
    monkeypatch.setattr(
        review_templates,
        "TEMPLATE_CONFIG_PATH",
        schemas_dir / "review_templates.json",
    )
    review_templates.reload_templates(path=schemas_dir / "missing_templates.json")
    yield root
    review_templates.reload_templates(path=schemas_dir / "missing_templates.json")


async def test_4_t1_config_snapshot_repository_create_list_and_latest(
    m6_client: M6Client,
) -> None:
    repo = ConfigSnapshotRepository()
    older = datetime(2026, 5, 18, 8, 0, 0)
    newer = datetime(2026, 5, 18, 9, 0, 0)

    async with m6_client.session_factory() as db:
        await repo.create(db, "s1", "product_config", '{"version": 1}', "admin", older)
        await repo.create(db, "s2", "product_config", '{"version": 2}', "admin", newer)
        await repo.create(db, "s3", "template_config", '{"version": 3}', "admin", newer)
        await db.commit()

        items = await repo.list_by_type(db, "product_config", limit=10)
        latest = await repo.get_latest(db, "product_config")
        missing = await repo.get_latest(db, "plot_config")

    assert [item.snapshot_id for item in items] == ["s2", "s1"]
    assert latest is not None
    assert latest.snapshot_id == "s2"
    assert missing is None


def test_4_t2_config_service_get_config_reads_product_config() -> None:
    payload = ConfigService().get_config("product_config")

    assert payload["max_lead_hours"] == 240
    assert "accum_products" in payload


async def test_4_t3_config_service_update_config_writes_file_snapshot_and_audit(
    m6_client: M6Client,
    temp_repo_root: Path,
) -> None:
    service = ConfigService(repo_root=temp_repo_root)
    user_id = await _user_id(m6_client.session_factory, "admin")
    payload = {"max_lead_hours": 168, "lead_step_hours": 3}

    async with m6_client.session_factory() as db:
        snapshot = await service.update_config(
            db,
            "product_config",
            payload,
            user_id=user_id,
            username="admin",
            ip_address="127.0.0.1",
        )

    saved = json.loads(
        (temp_repo_root / "schemas" / "product_config.json").read_text(
            encoding="utf-8"
        )
    )
    logs = await _audit_logs(m6_client.session_factory, "config_change")
    assert snapshot.config_type == "product_config"
    assert saved == payload
    assert len(logs) == 1
    assert logs[0].resource_type == "config"
    assert logs[0].resource_id == "product_config"


async def test_4_t4_config_service_update_config_invalid_json_rejected(
    m6_client: M6Client,
    temp_repo_root: Path,
) -> None:
    service = ConfigService(repo_root=temp_repo_root)
    user_id = await _user_id(m6_client.session_factory, "admin")

    async with m6_client.session_factory() as db:
        with pytest.raises(Exception) as exc_info:
            await service.update_config(
                db,
                "product_config",
                {},
                user_id=user_id,
                username="admin",
                ip_address=None,
            )

    assert getattr(exc_info.value, "code") == "CONFIG_VALIDATION_FAILED"


def test_4_t5_get_config_admin_gets_current_config(
    m6_client: M6Client,
    temp_repo_root: Path,
) -> None:
    response = m6_client.client.get(
        "/api/config/product_config",
        headers=_bearer(m6_client.tokens["admin"]),
    )

    body = response.json()
    assert response.status_code == 200
    assert body["data"]["max_lead_hours"] == 240
    assert body["trace_id"]


def test_4_t6_put_config_non_admin_denied(
    m6_client: M6Client,
    temp_repo_root: Path,
) -> None:
    response = m6_client.client.put(
        "/api/config/product_config",
        headers=_bearer(m6_client.tokens["viewer"]),
        json={"max_lead_hours": 168},
    )

    assert response.status_code == 403
    assert response.json()["code"] == "PERMISSION_DENIED"


def test_4_t7_get_config_history_returns_snapshot_list(
    m6_client: M6Client,
) -> None:
    async def seed() -> None:
        repo = ConfigSnapshotRepository()
        async with m6_client.session_factory() as db:
            await repo.create(
                db,
                "older",
                "product_config",
                '{"v": 1}',
                "admin",
                datetime(2026, 5, 18, 8, 0, 0),
            )
            await repo.create(
                db,
                "newer",
                "product_config",
                '{"v": 2}',
                "admin",
                datetime(2026, 5, 18, 9, 0, 0),
            )
            await db.commit()

    asyncio.run(seed())
    response = m6_client.client.get(
        "/api/config/product_config/history",
        headers=_bearer(m6_client.tokens["admin"]),
    )

    body = response.json()
    assert response.status_code == 200
    assert body["data"]["total"] == 2
    assert [item["snapshot_id"] for item in body["data"]["items"]] == [
        "newer",
        "older",
    ]
    assert "config_json" not in body["data"]["items"][0]


def test_5_t1_get_templates_returns_list_with_default(
    m6_client: M6Client,
    temp_repo_root: Path,
) -> None:
    response = m6_client.client.get(
        "/api/templates",
        headers=_bearer(m6_client.tokens["viewer"]),
    )

    body = response.json()
    assert response.status_code == 200
    assert body["data"][0]["template_id"] == "snow_phase_review_v1"
    assert body["data"][0]["panel_count"] == 5


def test_5_t2_get_template_detail(
    m6_client: M6Client,
    temp_repo_root: Path,
) -> None:
    response = m6_client.client.get(
        "/api/templates/snow_phase_review_v1",
        headers=_bearer(m6_client.tokens["viewer"]),
    )

    body = response.json()
    assert response.status_code == 200
    assert body["data"]["template_name"] == "雨雪相态复盘 V1"
    assert len(body["data"]["panels"]) == 5


def test_5_t3_get_template_nonexistent_returns_404(
    m6_client: M6Client,
    temp_repo_root: Path,
) -> None:
    response = m6_client.client.get(
        "/api/templates/nonexistent",
        headers=_bearer(m6_client.tokens["viewer"]),
    )

    assert response.status_code == 404
    assert response.json()["code"] == "TEMPLATE_NOT_FOUND"


def test_5_t4_put_template_admin_updates_cache_snapshot_and_audit(
    m6_client: M6Client,
    temp_repo_root: Path,
) -> None:
    payload = {
        "template_name": "雨雪相态复盘 V2",
        "required_fields": [
            "qpf_before",
            "ptype_before",
            "qpf_after",
            "ptype_after",
            "delta_qpf",
            "change_ptype",
        ],
        "optional_fields": ["z500", "t850", "rh700"],
        "allow_partial_success": True,
        "panels": [
            {
                "id": "before",
                "type": "precip_phase",
                "fields": ["qpf_before", "ptype_before"],
            },
            {
                "id": "after",
                "type": "precip_phase",
                "fields": ["qpf_after", "ptype_after"],
            },
            {"id": "delta", "type": "delta_qpf", "fields": ["delta_qpf"]},
            {"id": "change", "type": "change_ptype", "fields": ["change_ptype"]},
        ],
        "review_time_policy": "middle",
    }

    response = m6_client.client.put(
        "/api/templates/snow_phase_review_v1",
        headers=_bearer(m6_client.tokens["admin"]),
        json=payload,
    )

    body = response.json()
    assert response.status_code == 200
    assert body["message"] == "模板更新成功"
    assert body["data"]["template_id"] == "snow_phase_review_v1"
    assert body["data"]["snapshot_id"]

    detail = m6_client.client.get(
        "/api/templates/snow_phase_review_v1",
        headers=_bearer(m6_client.tokens["viewer"]),
    )
    assert detail.status_code == 200
    assert detail.json()["data"]["template_name"] == "雨雪相态复盘 V2"

    template_file = temp_repo_root / "schemas" / "review_templates.json"
    saved = json.loads(template_file.read_text(encoding="utf-8"))
    assert saved["snow_phase_review_v1"]["template_name"] == "雨雪相态复盘 V2"

    snapshots = asyncio.run(_snapshots(m6_client.session_factory, "template_config"))
    logs = asyncio.run(_audit_logs(m6_client.session_factory, "config_change"))
    assert len(snapshots) == 1
    assert snapshots[0].config_type == "template_config"
    assert len(logs) == 1
    assert logs[0].resource_type == "template"
    assert logs[0].resource_id == "snow_phase_review_v1"


def test_5_t5_put_template_non_admin_denied(
    m6_client: M6Client,
    temp_repo_root: Path,
) -> None:
    response = m6_client.client.put(
        "/api/templates/snow_phase_review_v1",
        headers=_bearer(m6_client.tokens["viewer"]),
        json={
            "template_name": "雨雪相态复盘 V2",
            "required_fields": ["qpf_before"],
            "optional_fields": [],
            "allow_partial_success": True,
            "panels": [
                {"id": "before", "type": "precip_phase", "fields": ["qpf_before"]}
            ],
            "review_time_policy": "middle",
        },
    )

    assert response.status_code == 403
    assert response.json()["code"] == "PERMISSION_DENIED"


def test_config_file_not_found_returns_config_not_found(
    m6_client: M6Client,
    temp_repo_root: Path,
) -> None:
    (temp_repo_root / "schemas" / "product_config.json").unlink()
    response = m6_client.client.get(
        "/api/config/product_config",
        headers=_bearer(m6_client.tokens["admin"]),
    )

    assert response.status_code == 404
    assert response.json()["code"] == "CONFIG_NOT_FOUND"


@pytest.mark.parametrize(
    ("payload", "message"),
    [
        (
            {
                "template_name": "bad",
                "required_fields": [],
                "optional_fields": [],
                "allow_partial_success": True,
                "panels": [{"id": "p1", "type": "x", "fields": ["a"]}],
                "review_time_policy": "middle",
            },
            "required_fields 不能为空",
        ),
        (
            {
                "template_name": "bad",
                "required_fields": ["a"],
                "optional_fields": [],
                "allow_partial_success": True,
                "panels": [],
                "review_time_policy": "middle",
            },
            "panels 不能为空",
        ),
        (
            {
                "template_name": "bad",
                "required_fields": ["a"],
                "optional_fields": [],
                "allow_partial_success": True,
                "panels": [
                    {"id": "p1", "type": "x", "fields": ["a"]},
                    {"id": "p1", "type": "y", "fields": ["a"]},
                ],
                "review_time_policy": "middle",
            },
            "panels 中存在重复的 id",
        ),
        (
            {
                "template_name": "bad",
                "required_fields": ["a"],
                "optional_fields": [],
                "allow_partial_success": True,
                "panels": [{"id": "p1", "type": "x", "fields": ["unknown"]}],
                "review_time_policy": "middle",
            },
            "unknown",
        ),
    ],
)
def test_template_validation_errors(
    m6_client: M6Client,
    temp_repo_root: Path,
    payload: dict[str, object],
    message: str,
) -> None:
    response = m6_client.client.put(
        "/api/templates/snow_phase_review_v1",
        headers=_bearer(m6_client.tokens["admin"]),
        json=payload,
    )

    body = response.json()
    assert response.status_code == 422
    assert body["code"] == "VALIDATION_ERROR"
    assert message in json.dumps(body, ensure_ascii=False)
