import asyncio
import csv
import io
import json
import zipfile
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

from app.core.errors import DomainError
from app.core.security import create_access_token, get_jwt_secret
from app.db.models import (
    AppUser,
    AuditLog,
    Base,
    EditOperation,
    EditSession,
    EditVersion,
    ForecastCase,
    ProductWindow,
    ReleaseProduct,
    ReviewProduct,
)
from app.db.session import get_db
from app.main import app
from app.services.stats_service import StatsService
from app.services.storage_monitor_service import StorageMonitorService
from app.services.task_monitor_service import TaskMonitorService
from app.storage.path_builder import PathBuilder

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
                    username="reviewer",
                    password_hash=pwd_context.hash("reviewer123"),
                    display_name="审核员",
                    role="reviewer",
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
                    created_at=base_time,
                    updated_at=base_time,
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
        username: create_access_token(
            user_id=asyncio.run(_user_id(session_factory, username)),
            username=username,
            role=username,
            secret=get_jwt_secret(),
        )[0]
        for username in ["admin", "reviewer", "viewer"]
    }

    with TestClient(app, raise_server_exceptions=False) as client:
        yield M6Client(client=client, session_factory=session_factory, tokens=tokens)

    app.dependency_overrides.pop(get_db, None)
    asyncio.run(engine.dispose())


def test_6_t1_storage_monitor_summary_and_cache(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> None:
    base_dir = tmp_path / "data"
    cases = base_dir / "cases"
    releases = base_dir / "releases"
    reviews = base_dir / "reviews"
    tmp_dir = base_dir / "tmp"
    for path in [cases, releases, reviews, tmp_dir]:
        path.mkdir(parents=True)
    (cases / "a.bin").write_bytes(b"a" * 10)
    (releases / "b.bin").write_bytes(b"b" * 20)
    (reviews / "c.bin").write_bytes(b"c" * 30)
    (tmp_dir / "d.bin").write_bytes(b"d" * 40)

    calls = 0
    original_walk = __import__("os").walk

    def counted_walk(*args: object, **kwargs: object) -> object:
        nonlocal calls
        calls += 1
        return original_walk(*args, **kwargs)

    monkeypatch.setattr("app.services.storage_monitor_service.os.walk", counted_walk)
    service = StorageMonitorService(PathBuilder(base_dir=base_dir), ttl_seconds=60)

    first = service.get_storage_summary()
    second = service.get_storage_summary()

    assert first is second
    assert calls == 4
    sizes = {item.type: item.size_bytes for item in first.breakdown}
    assert sizes == {"cases": 10, "releases": 20, "reviews": 30, "tmp": 40}
    counts = {item.type: item.file_count for item in first.breakdown}
    assert counts == {"cases": 1, "releases": 1, "reviews": 1, "tmp": 1}
    assert first.total_bytes > 0
    assert first.free_bytes > 0


def test_6_t2_storage_monitor_empty_directory_returns_zeros(tmp_path: Path) -> None:
    base_dir = tmp_path / "data"
    base_dir.mkdir()
    service = StorageMonitorService(PathBuilder(base_dir=base_dir), ttl_seconds=0)

    summary = service.get_storage_summary()

    assert {item.type: item.size_bytes for item in summary.breakdown} == {
        "cases": 0,
        "releases": 0,
        "reviews": 0,
        "tmp": 0,
    }
    assert all(item.file_count == 0 for item in summary.breakdown)


async def _seed_task_data(session_factory: async_sessionmaker[AsyncSession]) -> None:
    base_time = datetime(2026, 5, 18, 8, 0, 0)
    async with session_factory() as db:
        db.add(ForecastCase(case_id="2026051808", init_time=base_time))
        db.add(
            ProductWindow(
                window_id="w-task",
                case_id="2026051808",
                accum_hours=24,
                start_lead=0,
                end_lead=24,
            )
        )
        db.add(
            EditVersion(
                version_id="v-task",
                window_id="w-task",
                version_no=1,
                status="draft",
                qpf_after_path="/tmp/qpf",
                ptype_after_path="/tmp/ptype",
                delta_qpf_path="/tmp/delta",
                change_ptype_path="/tmp/change",
                touched_mask_path="/tmp/touched",
                changed_mask_path="/tmp/changed",
                created_by="admin",
                created_at=base_time,
            )
        )
        db.add_all(
            [
                ReviewProduct(
                    review_id=f"p{i}",
                    window_id="w-task",
                    version_id="v-task",
                    template_id="tpl",
                    plot_status="pending",
                    created_at=base_time,
                )
                for i in range(3)
            ]
            + [
                ReviewProduct(
                    review_id=f"r{i}",
                    window_id="w-task",
                    version_id="v-task",
                    template_id="tpl",
                    plot_status="running",
                    created_at=base_time,
                )
                for i in range(2)
            ]
            + [
                ReviewProduct(
                    review_id="failed-1",
                    window_id="w-task",
                    version_id="v-task",
                    template_id="tpl",
                    plot_status="failed",
                    attempt=2,
                    locked_by="worker",
                    locked_at=base_time,
                    next_retry_at=base_time,
                    plot_started_at=base_time,
                    plot_finished_at=base_time + timedelta(minutes=1),
                    error_log_path="failed details",
                    created_at=base_time,
                ),
                ReviewProduct(
                    review_id="permanent-1",
                    window_id="w-task",
                    version_id="v-task",
                    template_id="tpl",
                    plot_status="permanently_failed",
                    attempt=3,
                    plot_finished_at=base_time + timedelta(minutes=2),
                    created_at=base_time,
                ),
                ReviewProduct(
                    review_id="success-1",
                    window_id="w-task",
                    version_id="v-task",
                    template_id="tpl",
                    plot_status="success",
                    created_at=base_time,
                ),
            ]
        )
        await db.commit()


async def test_6_t3_task_monitor_summary_counts(m6_client: M6Client) -> None:
    await _seed_task_data(m6_client.session_factory)

    async with m6_client.session_factory() as db:
        summary = await TaskMonitorService().get_task_summary(db)

    assert summary.counts.pending == 3
    assert summary.counts.running == 2
    assert summary.counts.success == 1
    assert summary.counts.failed == 1
    assert summary.counts.permanently_failed == 1
    assert [item.review_id for item in summary.recent_failed] == [
        "permanent-1",
        "failed-1",
    ]


async def test_6_t4_manual_retry_failed_task_writes_audit(m6_client: M6Client) -> None:
    await _seed_task_data(m6_client.session_factory)
    admin_id = await _user_id(m6_client.session_factory, "admin")

    async with m6_client.session_factory() as db:
        product = await TaskMonitorService().manual_retry(
            db,
            "failed-1",
            user_id=admin_id,
            username="admin",
            ip_address="127.0.0.1",
        )

    async with m6_client.session_factory() as db:
        log_result = await db.execute(
            select(AuditLog).where(AuditLog.action == "task_retry")
        )
        logs = list(log_result.scalars().all())

    assert product.plot_status == "pending"
    assert product.attempt == 0
    assert product.locked_by is None
    assert product.plot_finished_at is None
    assert len(logs) == 1
    assert logs[0].resource_id == "failed-1"
    assert logs[0].user_id == admin_id


async def test_6_t5_manual_retry_non_failed_task_rejected(
    m6_client: M6Client,
) -> None:
    await _seed_task_data(m6_client.session_factory)
    admin_id = await _user_id(m6_client.session_factory, "admin")

    async with m6_client.session_factory() as db:
        with pytest.raises(DomainError) as exc_info:
            await TaskMonitorService().manual_retry(
                db,
                "success-1",
                user_id=admin_id,
                username="admin",
                ip_address=None,
            )

    assert exc_info.value.code == "TASK_NOT_RETRYABLE"


def test_6_t6_get_monitor_storage_admin_allowed_non_admin_denied(
    m6_client: M6Client,
    tmp_path: Path,
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    from app.api.routes import monitor as monitor_route

    monkeypatch.setattr(
        monitor_route,
        "storage_monitor_service",
        StorageMonitorService(PathBuilder(base_dir=tmp_path), ttl_seconds=0),
    )
    admin_response = m6_client.client.get(
        "/api/monitor/storage", headers=_bearer(m6_client.tokens["admin"])
    )
    viewer_response = m6_client.client.get(
        "/api/monitor/storage", headers=_bearer(m6_client.tokens["viewer"])
    )

    assert admin_response.status_code == 200
    assert admin_response.json()["data"]["breakdown"]
    assert viewer_response.status_code == 403


def test_6_t7_get_monitor_tasks_reviewer_allowed(m6_client: M6Client) -> None:
    asyncio.run(_seed_task_data(m6_client.session_factory))

    response = m6_client.client.get(
        "/api/monitor/tasks", headers=_bearer(m6_client.tokens["reviewer"])
    )

    assert response.status_code == 200
    assert response.json()["data"]["counts"]["failed"] == 1


def test_6_t8_post_monitor_task_retry_admin_only(m6_client: M6Client) -> None:
    asyncio.run(_seed_task_data(m6_client.session_factory))

    denied = m6_client.client.post(
        "/api/monitor/tasks/failed-1/retry",
        headers=_bearer(m6_client.tokens["reviewer"]),
    )
    allowed = m6_client.client.post(
        "/api/monitor/tasks/failed-1/retry",
        headers=_bearer(m6_client.tokens["admin"]),
    )

    assert denied.status_code == 403
    assert allowed.status_code == 200
    assert allowed.json()["data"] == {
        "review_id": "failed-1",
        "plot_status": "pending",
    }


async def _seed_stats_data(session_factory: async_sessionmaker[AsyncSession]) -> dict[str, int]:
    base_time = datetime(2026, 5, 1, 1, 0, 0)
    async with session_factory() as db:
        admin_id = int(
            (
                await db.execute(select(AppUser.id).where(AppUser.username == "admin"))
            ).scalar_one()
        )
        reviewer_id = int(
            (
                await db.execute(
                    select(AppUser.id).where(AppUser.username == "reviewer")
                )
            ).scalar_one()
        )
        db.add(ForecastCase(case_id="2026050108", init_time=base_time))
        db.add_all(
            [
                ProductWindow(
                    window_id="w24",
                    case_id="2026050108",
                    accum_hours=24,
                    start_lead=0,
                    end_lead=24,
                ),
                ProductWindow(
                    window_id="w48",
                    case_id="2026050108",
                    accum_hours=48,
                    start_lead=24,
                    end_lead=72,
                ),
            ]
        )
        db.add_all(
            [
                EditSession(
                    session_id="s-admin",
                    window_id="w24",
                    user_id=admin_id,
                    status="saved",
                    created_at=base_time,
                ),
                EditSession(
                    session_id="s-reviewer",
                    window_id="w48",
                    user_id=reviewer_id,
                    status="saved",
                    created_at=base_time,
                ),
            ]
        )
        db.add_all(
            [
                EditOperation(
                    operation_id="op1",
                    session_id="s-admin",
                    window_id="w24",
                    sequence_no=1,
                    tool_name="brush",
                    variable_name="qpf",
                    operation_type="replace",
                    after_stats_json=json.dumps({"count": 10}),
                    op_ptype_transition_json=json.dumps({"0_to_1": 10, "1_to_3": 5}),
                    created_at=base_time,
                ),
                EditOperation(
                    operation_id="op2",
                    session_id="s-admin",
                    window_id="w24",
                    sequence_no=2,
                    tool_name="polygon",
                    variable_name="ptype",
                    operation_type="set_ptype",
                    after_stats_json=json.dumps({"count": 20}),
                    op_ptype_transition_json=json.dumps({"0->1": 7, "2->3": 1}),
                    created_at=base_time + timedelta(hours=1),
                ),
                EditOperation(
                    operation_id="op3",
                    session_id="s-reviewer",
                    window_id="w48",
                    sequence_no=1,
                    tool_name="brush",
                    variable_name="qpf",
                    operation_type="replace",
                    after_stats_json=json.dumps({"count": 30}),
                    created_at=base_time + timedelta(hours=2),
                ),
            ]
        )
        db.add_all(
            [
                EditVersion(
                    version_id="v1",
                    window_id="w24",
                    version_no=1,
                    session_id="s-admin",
                    status="released",
                    qpf_after_path="/tmp/qpf1",
                    ptype_after_path="/tmp/ptype1",
                    delta_qpf_path="/tmp/delta1",
                    change_ptype_path="/tmp/change1",
                    touched_mask_path="/tmp/touched1",
                    changed_mask_path="/tmp/changed1",
                    created_by="admin",
                    created_at=base_time + timedelta(hours=3),
                ),
                EditVersion(
                    version_id="v2",
                    window_id="w48",
                    version_no=1,
                    session_id="s-reviewer",
                    status="draft",
                    qpf_after_path="/tmp/qpf2",
                    ptype_after_path="/tmp/ptype2",
                    delta_qpf_path="/tmp/delta2",
                    change_ptype_path="/tmp/change2",
                    touched_mask_path="/tmp/touched2",
                    changed_mask_path="/tmp/changed2",
                    created_by="reviewer",
                    created_at=base_time + timedelta(hours=4),
                ),
            ]
        )
        db.add(
            ReleaseProduct(
                release_id="rel1",
                version_id="v1",
                window_id="w24",
                release_status="active",
                released_by="admin",
                released_at=base_time + timedelta(hours=5),
            )
        )
        await db.commit()
    return {"admin": admin_id, "reviewer": reviewer_id}


async def test_7_t1_operation_stats_aggregates_correctly(
    m6_client: M6Client,
) -> None:
    await _seed_stats_data(m6_client.session_factory)

    async with m6_client.session_factory() as db:
        stats = await StatsService().get_operation_stats(
            db,
            start_date=datetime(2026, 5, 1).date(),
            end_date=datetime(2026, 5, 1).date(),
        )

    assert stats.total_sessions == 2
    assert stats.total_operations == 3
    assert stats.total_versions_saved == 2
    assert stats.total_versions_released == 1
    assert stats.by_tool == {"brush": 2, "polygon": 1}
    assert stats.by_operation == {"replace": 2, "set_ptype": 1}
    assert stats.by_accum_hours["24"] == {"sessions": 1, "versions": 1}
    assert stats.by_accum_hours["48"] == {"sessions": 1, "versions": 1}


async def test_7_t2_operation_stats_filter_by_user_id(
    m6_client: M6Client,
) -> None:
    ids = await _seed_stats_data(m6_client.session_factory)

    async with m6_client.session_factory() as db:
        stats = await StatsService().get_operation_stats(
            db,
            start_date=datetime(2026, 5, 1).date(),
            end_date=datetime(2026, 5, 1).date(),
            user_id=ids["admin"],
        )

    assert stats.total_sessions == 1
    assert stats.total_operations == 2
    assert stats.total_versions_saved == 1
    assert stats.by_tool == {"brush": 1, "polygon": 1}


async def test_7_t3_operation_stats_empty_period_returns_zeros(
    m6_client: M6Client,
) -> None:
    await _seed_stats_data(m6_client.session_factory)

    async with m6_client.session_factory() as db:
        stats = await StatsService().get_operation_stats(
            db,
            start_date=datetime(2020, 1, 1).date(),
            end_date=datetime(2020, 1, 31).date(),
        )

    assert stats.total_sessions == 0
    assert stats.total_operations == 0
    assert stats.total_versions_saved == 0
    assert stats.total_versions_released == 0
    assert stats.by_accum_hours == {}
    assert stats.by_tool == {}
    assert stats.by_operation == {}


async def test_7_t4_ptype_transition_stats_sums_matrices(
    m6_client: M6Client,
) -> None:
    await _seed_stats_data(m6_client.session_factory)

    async with m6_client.session_factory() as db:
        stats = await StatsService().get_ptype_transition_stats(
            db,
            start_date=datetime(2026, 5, 1).date(),
            end_date=datetime(2026, 5, 1).date(),
        )

    assert stats.total_operations_with_transitions == 2
    assert stats.matrix["0->1"] == 17
    assert stats.matrix["1->3"] == 5
    assert stats.matrix["2->3"] == 1
    assert len(stats.matrix) == 16
    assert stats.top_transitions[0].transition == "0->1"
    assert stats.top_transitions[0].label == "none->rain"


async def test_7_t5_ptype_transition_stats_single_operation_matches(
    m6_client: M6Client,
) -> None:
    ids = await _seed_stats_data(m6_client.session_factory)

    async with m6_client.session_factory() as db:
        stats = await StatsService().get_ptype_transition_stats(
            db,
            start_date=datetime(2026, 5, 1).date(),
            end_date=datetime(2026, 5, 1).date(),
            user_id=ids["admin"],
            window_id="w24",
        )

    assert stats.total_operations_with_transitions == 2
    assert stats.matrix["0->1"] == 17
    assert stats.matrix["1->3"] == 5
    assert all(key in stats.matrix for key in [f"{i}->{j}" for i in range(4) for j in range(4)])


async def test_7_t6_export_operations_csv_has_correct_columns(
    m6_client: M6Client,
) -> None:
    await _seed_stats_data(m6_client.session_factory)

    async with m6_client.session_factory() as db:
        exported = await StatsService().export_stats_csv(
            db,
            start_date=datetime(2026, 5, 1).date(),
            end_date=datetime(2026, 5, 1).date(),
            include=["operations"],
        )

    rows = list(csv.reader(io.StringIO(exported.content.decode("utf-8-sig"))))
    assert exported.media_type == "text/csv"
    assert rows[0] == [
        "case_id",
        "window_id",
        "accum_hours",
        "session_id",
        "user_id",
        "operation_id",
        "sequence_no",
        "tool_name",
        "variable_name",
        "operation_type",
        "affected_count",
        "created_at",
    ]
    assert len(rows) == 4
    assert rows[1][5] == "op1"
    assert rows[1][10] == "10"
    assert rows[1][11] == "2026-05-01 09:00:00"


async def test_7_t7_export_multiple_types_returns_zip(m6_client: M6Client) -> None:
    await _seed_stats_data(m6_client.session_factory)

    async with m6_client.session_factory() as db:
        exported = await StatsService().export_stats_csv(
            db,
            start_date=datetime(2026, 5, 1).date(),
            end_date=datetime(2026, 5, 1).date(),
            include=["operations", "ptype_transitions", "version_summary"],
        )

    assert exported.media_type == "application/zip"
    with zipfile.ZipFile(io.BytesIO(exported.content)) as zf:
        assert sorted(zf.namelist()) == [
            "operations.csv",
            "ptype_transitions.csv",
            "version_summary.csv",
        ]


def test_7_t8_get_stats_operations_date_range_exceeded(
    m6_client: M6Client,
) -> None:
    response = m6_client.client.get(
        "/api/stats/operations",
        headers=_bearer(m6_client.tokens["viewer"]),
        params={"start_date": "2025-01-01", "end_date": "2026-03-02"},
    )

    assert response.status_code == 422
    assert response.json()["code"] == "STATS_DATE_RANGE_EXCEEDED"


def test_7_t9_post_stats_export_returns_csv(m6_client: M6Client) -> None:
    asyncio.run(_seed_stats_data(m6_client.session_factory))

    response = m6_client.client.post(
        "/api/stats/export",
        headers=_bearer(m6_client.tokens["viewer"]),
        json={
            "start_date": "2026-05-01",
            "end_date": "2026-05-01",
            "format": "csv",
            "include": ["operations"],
        },
    )

    assert response.status_code == 200
    assert response.headers["content-type"].startswith("text/csv")
    assert "operations_20260501_20260501.csv" in response.headers[
        "content-disposition"
    ]
    assert response.content.decode("utf-8-sig").splitlines()[0].startswith("case_id")


def test_7_t10_get_stats_ptype_transitions_viewer_can_access(
    m6_client: M6Client,
) -> None:
    asyncio.run(_seed_stats_data(m6_client.session_factory))

    response = m6_client.client.get(
        "/api/stats/ptype-transitions",
        headers=_bearer(m6_client.tokens["viewer"]),
        params={"start_date": "2026-05-01", "end_date": "2026-05-01"},
    )

    assert response.status_code == 200
    assert response.json()["data"]["matrix"]["0->1"] == 17
