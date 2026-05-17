from __future__ import annotations

from collections.abc import AsyncIterator
from datetime import UTC, datetime

import pytest
from sqlalchemy.ext.asyncio import AsyncSession, create_async_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from app.core.errors import DomainError
from app.db.models import AppUser, Base, EditSession, EditVersion, ForecastCase, ProductWindow
from app.repositories.edit_version_repo import edit_version_repo
from app.repositories.review_approval_repo import review_approval_repo
from app.services.approval_service import ApprovalService

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

    async_session = sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)
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
            status="saved",
        )
    )
    await db.commit()


def _version_kwargs(status: str = "submitted", **overrides: object) -> dict[str, object]:
    data: dict[str, object] = {
        "version_id": f"{WINDOW_ID}_v001",
        "window_id": WINDOW_ID,
        "version_no": 1,
        "base_version_id": None,
        "session_id": SESSION_ID,
        "status": status,
        "qpf_after_path": "/data/v001/qpf_after.npz",
        "ptype_after_path": "/data/v001/ptype_after.npz",
        "delta_qpf_path": "/data/v001/delta_qpf.npz",
        "change_ptype_path": "/data/v001/change_ptype.npz",
        "touched_mask_path": "/data/v001/touched_mask.npz",
        "changed_mask_path": "/data/v001/changed_mask.npz",
    }
    data.update(overrides)
    return data


async def test_approve_happy_path(db_session: AsyncSession) -> None:
    await edit_version_repo.create(db_session, **_version_kwargs())
    service = ApprovalService()

    response = await service.review(
        db_session, f"{WINDOW_ID}_v001", "reviewer", "approve", "通过"
    )

    assert response["action"] == "approve"
    assert response["version_status"] == "approved"
    version = await db_session.get(EditVersion, f"{WINDOW_ID}_v001")
    assert version is not None
    assert version.status == "approved"
    approvals = await review_approval_repo.list_by_version_id(
        db_session, f"{WINDOW_ID}_v001"
    )
    assert len(approvals) == 1


async def test_reject_with_comment(db_session: AsyncSession) -> None:
    await edit_version_repo.create(db_session, **_version_kwargs())
    service = ApprovalService()

    response = await service.review(
        db_session, f"{WINDOW_ID}_v001", "reviewer", "reject", "需修订"
    )

    assert response["action"] == "reject"
    assert response["version_status"] == "rejected"
    assert response["comment"] == "需修订"


async def test_reject_without_comment_error(db_session: AsyncSession) -> None:
    await edit_version_repo.create(db_session, **_version_kwargs())
    service = ApprovalService()

    with pytest.raises(DomainError) as exc_info:
        await service.review(db_session, f"{WINDOW_ID}_v001", "reviewer", "reject", "")

    assert exc_info.value.code == "VALIDATION_ERROR"


async def test_approve_non_submitted_error(db_session: AsyncSession) -> None:
    await edit_version_repo.create(db_session, **_version_kwargs(status="draft"))
    service = ApprovalService()

    with pytest.raises(DomainError) as exc_info:
        await service.review(
            db_session, f"{WINDOW_ID}_v001", "reviewer", "approve", None
        )

    assert exc_info.value.code == "VERSION_STATUS_CONFLICT"
