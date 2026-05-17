from __future__ import annotations

from collections.abc import AsyncIterator
from datetime import UTC, datetime
from pathlib import Path

import pytest
import sqlalchemy as sa
from alembic import command
from alembic.config import Config
from sqlalchemy.ext.asyncio import AsyncSession, create_async_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from app.db.models import AppUser, Base, EditSession, ForecastCase, ProductWindow
from app.repositories.edit_operation_repo import edit_operation_repo

CASE_ID = "2026051608"
WINDOW_ID = f"{CASE_ID}_ACC24_000_024"
SESSION_ID = "00000000-0000-0000-0000-000000000100"


@pytest.fixture()
async def db_session() -> AsyncIterator[AsyncSession]:
    engine = create_async_engine(
        "sqlite+aiosqlite://",
        connect_args={"check_same_thread": False},
        poolclass=StaticPool,
    )
    async with engine.begin() as connection:
        await connection.run_sync(Base.metadata.create_all)

    async_session = sessionmaker(
        engine, class_=AsyncSession, expire_on_commit=False
    )
    async with async_session() as session:
        await _seed_parent_rows(session)
        yield session

    await engine.dispose()


async def _seed_parent_rows(db: AsyncSession) -> None:
    db.add(
        AppUser(
            id=1,
            username="forecaster",
            password_hash="hash",
            display_name="预报员",
            role="forecaster",
            is_active=True,
        )
    )
    db.add(
        ForecastCase(
            case_id=CASE_ID,
            init_time=datetime(2026, 5, 16, 8, tzinfo=UTC),
            status="complete",
        )
    )
    db.add(
        ProductWindow(
            window_id=WINDOW_ID,
            case_id=CASE_ID,
            accum_hours=24,
            start_lead=0,
            end_lead=24,
            status="available",
            qc_status="pass",
            negative_count=0,
            missing_count=0,
        )
    )
    db.add(
        EditSession(
            session_id=SESSION_ID,
            window_id=WINDOW_ID,
            user_id=1,
            status="editing",
        )
    )
    await db.commit()


def _operation_kwargs(sequence_no: int = 1, **overrides: object) -> dict[str, object]:
    data: dict[str, object] = {
        "operation_id": f"00000000-0000-0000-0000-{sequence_no:012d}",
        "session_id": SESSION_ID,
        "window_id": WINDOW_ID,
        "sequence_no": sequence_no,
        "tool_name": "polygon",
        "variable_name": "qpf",
        "operation_type": "increase",
        "parameters_json": '{"delta": 5.0}',
        "mask_geometry_json": '{"type": "polygon", "coordinates": [[100, 30]]}',
        "mask_raster_path": "/tmp/mask.npz",
        "before_stats_json": '{"mean": 3.0}',
        "after_stats_json": '{"mean": 8.0}',
        "op_ptype_transition_json": '[[0, 0, 0, 0], [0, 1, 0, 0]]',
        "is_undone": 0,
    }
    data.update(overrides)
    return data


async def test_create_operation(db_session: AsyncSession) -> None:
    operation = await edit_operation_repo.create(
        db_session, **_operation_kwargs(sequence_no=1)
    )

    assert operation.operation_id == "00000000-0000-0000-0000-000000000001"
    assert operation.session_id == SESSION_ID
    assert operation.window_id == WINDOW_ID
    assert operation.sequence_no == 1
    assert operation.tool_name == "polygon"
    assert operation.variable_name == "qpf"
    assert operation.operation_type == "increase"
    assert operation.parameters_json == '{"delta": 5.0}'
    assert operation.mask_geometry_json == '{"type": "polygon", "coordinates": [[100, 30]]}'
    assert operation.mask_raster_path == "/tmp/mask.npz"
    assert operation.before_stats_json == '{"mean": 3.0}'
    assert operation.after_stats_json == '{"mean": 8.0}'
    assert operation.op_ptype_transition_json == '[[0, 0, 0, 0], [0, 1, 0, 0]]'
    assert operation.is_undone == 0
    assert operation.created_at is not None


async def test_query_by_session_ordered(db_session: AsyncSession) -> None:
    for sequence_no in [3, 1, 2]:
        await edit_operation_repo.create(
            db_session, **_operation_kwargs(sequence_no=sequence_no)
        )

    operations = await edit_operation_repo.query_by_session(db_session, SESSION_ID)

    assert [operation.sequence_no for operation in operations] == [1, 2, 3]


async def test_query_by_session_filter_undone(db_session: AsyncSession) -> None:
    await edit_operation_repo.create(db_session, **_operation_kwargs(sequence_no=1))
    await edit_operation_repo.create(
        db_session, **_operation_kwargs(sequence_no=2, is_undone=1)
    )
    await edit_operation_repo.create(db_session, **_operation_kwargs(sequence_no=3))

    operations = await edit_operation_repo.query_by_session(
        db_session, SESSION_ID, include_undone=False
    )

    assert [operation.sequence_no for operation in operations] == [1, 3]


async def test_update_is_undone(db_session: AsyncSession) -> None:
    operation = await edit_operation_repo.create(
        db_session, **_operation_kwargs(sequence_no=1)
    )

    await edit_operation_repo.update_is_undone(
        db_session, str(operation.operation_id), is_undone=1
    )
    updated = await edit_operation_repo.query_by_session(db_session, SESSION_ID)

    assert updated[0].is_undone == 1


async def test_delete_after_sequence(db_session: AsyncSession) -> None:
    for sequence_no in range(1, 6):
        await edit_operation_repo.create(
            db_session, **_operation_kwargs(sequence_no=sequence_no)
        )

    deleted_count = await edit_operation_repo.delete_after_sequence(
        db_session, SESSION_ID, sequence_no=3
    )
    operations = await edit_operation_repo.query_by_session(db_session, SESSION_ID)

    assert deleted_count == 2
    assert [operation.sequence_no for operation in operations] == [1, 2, 3]


async def test_get_max_sequence(db_session: AsyncSession) -> None:
    assert await edit_operation_repo.get_max_sequence(db_session, SESSION_ID) == 0

    for sequence_no in range(1, 4):
        await edit_operation_repo.create(
            db_session, **_operation_kwargs(sequence_no=sequence_no)
        )

    assert await edit_operation_repo.get_max_sequence(db_session, SESSION_ID) == 3


def test_migration_creates_table(tmp_path: Path) -> None:
    db_path = tmp_path / "migration.db"
    config = Config("alembic.ini")
    config.set_main_option("sqlalchemy.url", f"sqlite+aiosqlite:///{db_path}")

    command.upgrade(config, "head")

    engine = sa.create_engine(f"sqlite:///{db_path}")
    inspector = sa.inspect(engine)
    columns = {column["name"]: column for column in inspector.get_columns("edit_operation")}
    indexes = inspector.get_indexes("edit_operation")

    assert set(columns) == {
        "operation_id",
        "session_id",
        "window_id",
        "sequence_no",
        "tool_name",
        "variable_name",
        "operation_type",
        "parameters_json",
        "mask_geometry_json",
        "mask_raster_path",
        "before_stats_json",
        "after_stats_json",
        "op_ptype_transition_json",
        "is_undone",
        "created_at",
    }
    assert columns["operation_id"]["primary_key"] == 1
    assert columns["session_id"]["nullable"] is False
    assert columns["is_undone"]["nullable"] is False
    assert columns["created_at"]["nullable"] is False
    assert any(
        index["name"] == "idx_edit_op_session_seq"
        and index["column_names"] == ["session_id", "sequence_no"]
        for index in indexes
    )

    engine.dispose()
