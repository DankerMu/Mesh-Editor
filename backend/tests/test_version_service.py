from __future__ import annotations

from collections.abc import AsyncIterator
from datetime import UTC, datetime
from pathlib import Path

import numpy as np
import pytest
from sqlalchemy.ext.asyncio import AsyncSession, create_async_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from app.core.errors import DomainError
from app.db.models import AppUser, Base, EditSession, EditVersion, ForecastCase, ProductWindow
from app.repositories.edit_operation_repo import edit_operation_repo
from app.repositories.edit_version_repo import edit_version_repo
from app.services.version_service import VersionService
from app.storage.path_builder import PathBuilder

CASE_ID = "2026051608"
WINDOW_ID = f"{CASE_ID}_ACC24_000_024"
SESSION_ID = "00000000-0000-0000-0000-000000000100"
GRID_SHAPE = (501, 821)


@pytest.fixture()
async def db_session() -> AsyncIterator[AsyncSession]:
    engine = create_async_engine(
        "sqlite+aiosqlite://",
        connect_args={"check_same_thread": False},
        poolclass=StaticPool,
    )
    async with engine.begin() as connection:
        await connection.run_sync(Base.metadata.create_all)

    async_session = sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)
    async with async_session() as session:
        await _seed_parent_rows(session)
        yield session

    await engine.dispose()


@pytest.fixture()
def service(tmp_path: Path) -> VersionService:
    return VersionService(path_builder=PathBuilder(base_dir=tmp_path))


async def _seed_parent_rows(db: AsyncSession, session_status: str = "editing") -> None:
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
            base_version_id=None,
            status=session_status,
        )
    )
    await db.commit()


def _write_session_fields(builder: PathBuilder) -> None:
    session_dir = builder.session_root(SESSION_ID)
    session_dir.mkdir(parents=True, exist_ok=True)
    qpf_before = np.zeros(GRID_SHAPE, dtype=np.float32)
    qpf_after = qpf_before.copy()
    qpf_after[0, 0] = 1.25
    ptype_before = np.zeros(GRID_SHAPE, dtype=np.uint8)
    ptype_after = ptype_before.copy()
    ptype_after[0, 0] = 1
    touched_mask = np.zeros(GRID_SHAPE, dtype=np.uint8)
    touched_mask[0, 0] = 1
    for name, data in [
        ("qpf_before", qpf_before),
        ("qpf_after", qpf_after),
        ("ptype_before", ptype_before),
        ("ptype_after", ptype_after),
        ("touched_mask", touched_mask),
    ]:
        np.savez_compressed(session_dir / f"{name}.npz", data=data)


def _version_kwargs(version_no: int = 1, **overrides: object) -> dict[str, object]:
    data: dict[str, object] = {
        "version_id": f"{WINDOW_ID}_v{version_no:03d}",
        "window_id": WINDOW_ID,
        "version_no": version_no,
        "base_version_id": None,
        "session_id": SESSION_ID,
        "status": "draft",
        "qpf_after_path": f"/data/v{version_no:03d}/qpf_after.npz",
        "ptype_after_path": f"/data/v{version_no:03d}/ptype_after.npz",
        "delta_qpf_path": f"/data/v{version_no:03d}/delta_qpf.npz",
        "change_ptype_path": f"/data/v{version_no:03d}/change_ptype.npz",
        "touched_mask_path": f"/data/v{version_no:03d}/touched_mask.npz",
        "changed_mask_path": f"/data/v{version_no:03d}/changed_mask.npz",
    }
    data.update(overrides)
    return data


async def test_save_version_happy_path(
    db_session: AsyncSession, service: VersionService
) -> None:
    _write_session_fields(service.path_builder)
    await edit_operation_repo.create(
        db_session,
        operation_id="00000000-0000-0000-0000-000000000001",
        session_id=SESSION_ID,
        window_id=WINDOW_ID,
        sequence_no=1,
        tool_name="polygon",
        variable_name="qpf",
        operation_type="set",
        parameters_json='{"value": 1.25}',
        is_undone=0,
    )

    response = await service.save_version(db_session, SESSION_ID)

    assert response["version_id"] == f"{WINDOW_ID}_v001"
    version = await db_session.get(EditVersion, response["version_id"])
    assert version is not None
    assert version.status == "draft"
    assert version.after_image_path is not None
    version_dir = service.path_builder.version_root(WINDOW_ID, response["version_id"])
    for filename in [
        "qpf_after.npz",
        "ptype_after.npz",
        "delta_qpf.npz",
        "change_ptype.npz",
        "touched_mask.npz",
        "changed_mask.npz",
        "operations.jsonl",
    ]:
        assert (version_dir / filename).exists()
    with np.load(version_dir / "delta_qpf.npz") as payload:
        assert payload["data"][0, 0] == pytest.approx(1.25)
    assert (version_dir / "operations.jsonl").read_text(encoding="utf-8").count("\n") == 1

    session = await db_session.get(EditSession, SESSION_ID)
    assert session is not None
    assert session.status == "saved"


async def test_save_version_session_not_editing(
    db_session: AsyncSession, service: VersionService
) -> None:
    session = await db_session.get(EditSession, SESSION_ID)
    assert session is not None
    session.status = "saved"  # type: ignore[assignment]
    await db_session.flush()

    with pytest.raises(DomainError) as exc_info:
        await service.save_version(db_session, SESSION_ID)

    assert exc_info.value.code == "SESSION_STATUS_INVALID"


async def test_save_version_base_outdated(
    db_session: AsyncSession, service: VersionService
) -> None:
    await edit_version_repo.create(
        db_session, **_version_kwargs(version_no=1, version_id=f"{WINDOW_ID}_v001")
    )
    session = await db_session.get(EditSession, SESSION_ID)
    assert session is not None
    session.base_version_id = None  # type: ignore[assignment]
    await db_session.flush()
    _write_session_fields(service.path_builder)

    with pytest.raises(DomainError) as exc_info:
        await service.save_version(db_session, SESSION_ID)

    assert exc_info.value.code == "VERSION_BASE_OUTDATED"


async def test_save_version_image_failure_still_saves(
    db_session: AsyncSession, tmp_path: Path
) -> None:
    def failing_plotter(*_: object, **__: object) -> bytes:
        raise RuntimeError("plot failed")

    service = VersionService(
        path_builder=PathBuilder(base_dir=tmp_path),
        precip_plotter=failing_plotter,
    )
    _write_session_fields(service.path_builder)

    response = await service.save_version(db_session, SESSION_ID)

    version = await db_session.get(EditVersion, response["version_id"])
    assert version is not None
    assert version.status == "draft"
    assert version.before_image_path is None
    assert version.after_image_path is None


async def test_submit_happy_path(db_session: AsyncSession, service: VersionService) -> None:
    await edit_version_repo.create(db_session, **_version_kwargs())

    response = await service.submit(db_session, f"{WINDOW_ID}_v001")

    assert response == {"version_id": f"{WINDOW_ID}_v001", "status": "submitted"}


async def test_submit_non_draft(db_session: AsyncSession, service: VersionService) -> None:
    await edit_version_repo.create(db_session, **_version_kwargs(status="submitted"))

    with pytest.raises(DomainError) as exc_info:
        await service.submit(db_session, f"{WINDOW_ID}_v001")

    assert exc_info.value.code == "VERSION_STATUS_CONFLICT"
