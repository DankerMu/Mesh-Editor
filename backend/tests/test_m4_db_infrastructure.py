from __future__ import annotations

from collections.abc import AsyncIterator
from datetime import UTC, datetime
from pathlib import Path

import pytest
import sqlalchemy as sa
from alembic import command
from alembic.config import Config
from pydantic import ValidationError
from sqlalchemy.exc import IntegrityError
from sqlalchemy.ext.asyncio import AsyncSession, create_async_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from app.db.models import (
    AppUser,
    Base,
    EditSession,
    EditVersion,
    ForecastCase,
    ProductWindow,
    ReleaseProduct,
    ReviewApproval,
)
from app.repositories.edit_version_repo import edit_version_repo
from app.repositories.release_product_repo import release_product_repo
from app.repositories.review_approval_repo import review_approval_repo
from app.schemas.version import (
    VersionDetail,
    VersionListItem,
    VersionReleaseRequest,
    VersionReviewRequest,
    VersionSaveRequest,
    VersionSubmitRequest,
)

CASE_ID = "2026051608"
WINDOW_ID = f"{CASE_ID}_ACC24_000_024"
OTHER_WINDOW_ID = f"{CASE_ID}_ACC24_024_048"
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
    for window_id, start_lead, end_lead in [
        (WINDOW_ID, 0, 24),
        (OTHER_WINDOW_ID, 24, 48),
    ]:
        db.add(
            ProductWindow(
                window_id=window_id,
                case_id=CASE_ID,
                accum_hours=24,
                start_lead=start_lead,
                end_lead=end_lead,
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
            base_version_id=f"{WINDOW_ID}_v000",
            status="editing",
        )
    )
    await db.commit()


def _version_kwargs(version_no: int = 1, **overrides: object) -> dict[str, object]:
    data: dict[str, object] = {
        "version_id": f"{WINDOW_ID}_v{version_no:03d}",
        "window_id": WINDOW_ID,
        "version_no": version_no,
        "base_version_id": f"{WINDOW_ID}_v000",
        "session_id": SESSION_ID,
        "status": "draft",
        "qpf_after_path": f"/data/v{version_no:03d}/qpf_after.npz",
        "ptype_after_path": f"/data/v{version_no:03d}/ptype_after.npz",
        "delta_qpf_path": f"/data/v{version_no:03d}/delta_qpf.npz",
        "change_ptype_path": f"/data/v{version_no:03d}/change_ptype.npz",
        "touched_mask_path": f"/data/v{version_no:03d}/touched_mask.npz",
        "changed_mask_path": f"/data/v{version_no:03d}/changed_mask.npz",
        "version_ptype_transition_path": f"/data/v{version_no:03d}/transition.csv",
        "before_image_path": f"/data/v{version_no:03d}/before.png",
        "after_image_path": f"/data/v{version_no:03d}/after.png",
        "delta_qpf_image_path": f"/data/v{version_no:03d}/delta_qpf.png",
        "change_ptype_image_path": f"/data/v{version_no:03d}/change_ptype.png",
        "touched_mask_image_path": f"/data/v{version_no:03d}/touched_mask.png",
        "changed_mask_image_path": f"/data/v{version_no:03d}/changed_mask.png",
        "review_image_path": f"/data/v{version_no:03d}/review.png",
        "created_by": "forecaster",
    }
    data.update(overrides)
    return data


def _approval_kwargs(index: int = 1, **overrides: object) -> dict[str, object]:
    data: dict[str, object] = {
        "approval_id": f"approval-{index}",
        "version_id": f"{WINDOW_ID}_v001",
        "reviewer_id": "reviewer",
        "action": "approve",
        "comment": "通过",
        "reviewed_at": datetime(2026, 5, 17, 8, index, tzinfo=UTC),
    }
    data.update(overrides)
    return data


def _release_kwargs(index: int = 1, **overrides: object) -> dict[str, object]:
    data: dict[str, object] = {
        "release_id": f"release-{index}",
        "version_id": f"{WINDOW_ID}_v001",
        "window_id": WINDOW_ID,
        "release_status": "active",
        "product_path": f"/release/{index}",
        "manifest_path": f"/release/{index}/product_manifest.json",
        "released_by": "reviewer",
        "released_at": datetime(2026, 5, 17, 9, index, tzinfo=UTC),
    }
    data.update(overrides)
    return data


def _migration_config(db_path: Path) -> Config:
    config = Config("alembic.ini")
    config.set_main_option("sqlalchemy.url", f"sqlite+aiosqlite:///{db_path}")
    return config


def test_m4_model_table_names() -> None:
    assert EditVersion.__tablename__ == "edit_version"
    assert ReviewApproval.__tablename__ == "review_approval"
    assert ReleaseProduct.__tablename__ == "release_product"


def test_m4_migration_creates_tables_with_correct_schema(tmp_path: Path) -> None:
    db_path = tmp_path / "migration.db"
    command.upgrade(_migration_config(db_path), "head")

    engine = sa.create_engine(f"sqlite:///{db_path}")
    inspector = sa.inspect(engine)

    edit_version_columns = {
        column["name"]: column for column in inspector.get_columns("edit_version")
    }
    review_columns = {
        column["name"]: column for column in inspector.get_columns("review_approval")
    }
    release_columns = {
        column["name"]: column for column in inspector.get_columns("release_product")
    }

    assert set(edit_version_columns) == {
        "version_id",
        "window_id",
        "version_no",
        "base_version_id",
        "session_id",
        "status",
        "qpf_after_path",
        "ptype_after_path",
        "delta_qpf_path",
        "change_ptype_path",
        "touched_mask_path",
        "changed_mask_path",
        "version_ptype_transition_path",
        "before_image_path",
        "after_image_path",
        "delta_qpf_image_path",
        "change_ptype_image_path",
        "touched_mask_image_path",
        "changed_mask_image_path",
        "review_image_path",
        "created_by",
        "created_at",
    }
    assert edit_version_columns["version_id"]["primary_key"] == 1
    for column_name in [
        "window_id",
        "version_no",
        "status",
        "qpf_after_path",
        "ptype_after_path",
        "delta_qpf_path",
        "change_ptype_path",
        "touched_mask_path",
        "changed_mask_path",
        "created_at",
    ]:
        assert edit_version_columns[column_name]["nullable"] is False

    assert set(review_columns) == {
        "approval_id",
        "version_id",
        "reviewer_id",
        "action",
        "comment",
        "reviewed_at",
    }
    assert review_columns["approval_id"]["primary_key"] == 1
    for column_name in ["version_id", "reviewer_id", "action", "reviewed_at"]:
        assert review_columns[column_name]["nullable"] is False

    assert set(release_columns) == {
        "release_id",
        "version_id",
        "window_id",
        "release_status",
        "product_path",
        "manifest_path",
        "released_by",
        "released_at",
        "superseded_at",
    }
    assert release_columns["release_id"]["primary_key"] == 1
    for column_name in [
        "version_id",
        "window_id",
        "release_status",
        "released_by",
        "released_at",
    ]:
        assert release_columns[column_name]["nullable"] is False

    edit_indexes = {
        index["name"]: index for index in inspector.get_indexes("edit_version")
    }
    review_indexes = {
        index["name"]: index for index in inspector.get_indexes("review_approval")
    }
    release_indexes = {
        index["name"]: index for index in inspector.get_indexes("release_product")
    }
    edit_session_columns = {
        column["name"]: column for column in inspector.get_columns("edit_session")
    }
    assert edit_indexes["idx_edit_version_window"]["column_names"] == [
        "window_id",
        "status",
    ]
    assert edit_indexes["ux_edit_version_window_no"]["column_names"] == [
        "window_id",
        "version_no",
    ]
    assert edit_indexes["ux_edit_version_window_no"]["unique"] == 1
    assert review_indexes["idx_review_approval_version"]["column_names"] == [
        "version_id",
        "reviewed_at",
    ]
    assert release_indexes["ux_release_active_window"]["column_names"] == [
        "window_id",
    ]
    assert release_indexes["ux_release_active_window"]["unique"] == 1
    assert edit_session_columns["base_version_id"]["type"].length == 64

    review_fks = inspector.get_foreign_keys("review_approval")
    release_fks = inspector.get_foreign_keys("release_product")
    assert any(
        fk["constrained_columns"] == ["version_id"]
        and fk["referred_table"] == "edit_version"
        for fk in review_fks
    )
    assert any(
        fk["constrained_columns"] == ["version_id"]
        and fk["referred_table"] == "edit_version"
        for fk in release_fks
    )
    assert any(
        fk["constrained_columns"] == ["window_id"]
        and fk["referred_table"] == "product_window"
        for fk in release_fks
    )

    engine.dispose()


def test_m4_migration_roundtrip(tmp_path: Path) -> None:
    db_path = tmp_path / "migration.db"
    config = _migration_config(db_path)

    command.upgrade(config, "head")
    command.downgrade(config, "base")
    command.upgrade(config, "head")

    engine = sa.create_engine(f"sqlite:///{db_path}")
    inspector = sa.inspect(engine)
    assert "edit_version" in inspector.get_table_names()
    assert "review_approval" in inspector.get_table_names()
    assert "release_product" in inspector.get_table_names()
    engine.dispose()


async def test_edit_version_repo_create_and_get(
    db_session: AsyncSession,
) -> None:
    version = await edit_version_repo.create(db_session, **_version_kwargs())

    assert version.version_id == f"{WINDOW_ID}_v001"
    assert version.window_id == WINDOW_ID
    assert version.version_no == 1
    assert version.base_version_id == f"{WINDOW_ID}_v000"
    assert version.session_id == SESSION_ID
    assert version.status == "draft"
    assert version.qpf_after_path == "/data/v001/qpf_after.npz"
    assert version.ptype_after_path == "/data/v001/ptype_after.npz"
    assert version.delta_qpf_path == "/data/v001/delta_qpf.npz"
    assert version.change_ptype_path == "/data/v001/change_ptype.npz"
    assert version.touched_mask_path == "/data/v001/touched_mask.npz"
    assert version.changed_mask_path == "/data/v001/changed_mask.npz"
    assert version.version_ptype_transition_path == "/data/v001/transition.csv"
    assert version.before_image_path == "/data/v001/before.png"
    assert version.after_image_path == "/data/v001/after.png"
    assert version.delta_qpf_image_path == "/data/v001/delta_qpf.png"
    assert version.change_ptype_image_path == "/data/v001/change_ptype.png"
    assert version.touched_mask_image_path == "/data/v001/touched_mask.png"
    assert version.changed_mask_image_path == "/data/v001/changed_mask.png"
    assert version.review_image_path == "/data/v001/review.png"
    assert version.created_by == "forecaster"
    assert version.created_at is not None

    fetched = await edit_version_repo.get(db_session, str(version.version_id))
    assert fetched is not None
    assert fetched.version_id == version.version_id
    assert await edit_version_repo.get(db_session, "missing") is None


async def test_edit_version_repo_list_filters_and_order(
    db_session: AsyncSession,
) -> None:
    await edit_version_repo.create(
        db_session, **_version_kwargs(version_no=1, status="draft")
    )
    await edit_version_repo.create(
        db_session, **_version_kwargs(version_no=3, status="submitted")
    )
    await edit_version_repo.create(
        db_session,
        **_version_kwargs(
            version_no=2,
            version_id=f"{OTHER_WINDOW_ID}_v002",
            window_id=OTHER_WINDOW_ID,
            status="draft",
            created_by="other",
        ),
    )

    versions = await edit_version_repo.list(db_session)
    assert [version.version_no for version in versions] == [3, 2, 1]

    draft_versions = await edit_version_repo.list(db_session, status="draft")
    assert {version.version_id for version in draft_versions} == {
        f"{WINDOW_ID}_v001",
        f"{OTHER_WINDOW_ID}_v002",
    }

    window_versions = await edit_version_repo.list(db_session, window_id=WINDOW_ID)
    assert [version.version_id for version in window_versions] == [
        f"{WINDOW_ID}_v003",
        f"{WINDOW_ID}_v001",
    ]

    user_versions = await edit_version_repo.list(db_session, created_by="other")
    assert [version.version_id for version in user_versions] == [
        f"{OTHER_WINDOW_ID}_v002"
    ]


async def test_edit_version_repo_latest_update_and_max(
    db_session: AsyncSession,
) -> None:
    assert await edit_version_repo.get_latest_for_window(db_session, WINDOW_ID) is None
    assert await edit_version_repo.get_max_version_no(db_session, WINDOW_ID) == 0

    for version_no in [1, 4, 2]:
        await edit_version_repo.create(
            db_session, **_version_kwargs(version_no=version_no)
        )

    latest = await edit_version_repo.get_latest_for_window(db_session, WINDOW_ID)
    assert latest is not None
    assert latest.version_no == 4
    assert await edit_version_repo.get_latest_for_window(db_session, "empty") is None

    await edit_version_repo.update_status(db_session, f"{WINDOW_ID}_v004", "submitted")
    updated = await edit_version_repo.get(db_session, f"{WINDOW_ID}_v004")
    assert updated is not None
    assert updated.status == "submitted"

    assert await edit_version_repo.get_max_version_no(db_session, WINDOW_ID) == 4


async def test_edit_version_rejects_duplicate_version_no_per_window(
    db_session: AsyncSession,
) -> None:
    await edit_version_repo.create(db_session, **_version_kwargs(version_no=1))

    with pytest.raises(IntegrityError):
        await edit_version_repo.create(
            db_session,
            **_version_kwargs(
                version_no=1,
                version_id=f"{WINDOW_ID}_duplicate_v001",
            ),
        )


async def test_review_approval_repo_create_and_list(
    db_session: AsyncSession,
) -> None:
    await edit_version_repo.create(db_session, **_version_kwargs())

    first = await review_approval_repo.create(db_session, **_approval_kwargs(index=1))
    second = await review_approval_repo.create(
        db_session,
        **_approval_kwargs(
            index=2,
            approval_id="approval-2",
            action="reject",
            comment="需修订",
        ),
    )

    assert first.approval_id == "approval-1"
    assert first.version_id == f"{WINDOW_ID}_v001"
    assert first.reviewer_id == "reviewer"
    assert first.action == "approve"
    assert first.comment == "通过"
    assert first.reviewed_at is not None

    approvals = await review_approval_repo.list_by_version_id(
        db_session, f"{WINDOW_ID}_v001"
    )
    assert [approval.approval_id for approval in approvals] == [
        second.approval_id,
        first.approval_id,
    ]
    assert await review_approval_repo.list_by_version_id(db_session, "missing") == []


async def test_release_product_repo_create_get_active_and_update(
    db_session: AsyncSession,
) -> None:
    await edit_version_repo.create(db_session, **_version_kwargs())

    release = await release_product_repo.create(db_session, **_release_kwargs(index=1))

    assert release.release_id == "release-1"
    assert release.version_id == f"{WINDOW_ID}_v001"
    assert release.window_id == WINDOW_ID
    assert release.release_status == "active"
    assert release.product_path == "/release/1"
    assert release.manifest_path == "/release/1/product_manifest.json"
    assert release.released_by == "reviewer"
    assert release.released_at is not None
    assert release.superseded_at is None

    active = await release_product_repo.get_active_by_window(db_session, WINDOW_ID)
    assert active is not None
    assert active.release_id == "release-1"
    assert (
        await release_product_repo.get_active_by_window(db_session, OTHER_WINDOW_ID)
        is None
    )

    superseded_at = datetime(2026, 5, 17, 10, 0, tzinfo=UTC)
    await release_product_repo.update_status(
        db_session,
        "release-1",
        "superseded",
        superseded_at=superseded_at,
    )
    updated = await db_session.get(ReleaseProduct, "release-1")
    assert updated is not None
    assert updated.release_status == "superseded"
    assert updated.superseded_at == superseded_at
    assert await release_product_repo.get_active_by_window(db_session, WINDOW_ID) is None


async def test_release_product_allows_only_one_active_release_per_window(
    db_session: AsyncSession,
) -> None:
    await edit_version_repo.create(db_session, **_version_kwargs(version_no=1))
    await edit_version_repo.create(db_session, **_version_kwargs(version_no=2))
    await release_product_repo.create(db_session, **_release_kwargs(index=1))

    with pytest.raises(IntegrityError):
        await release_product_repo.create(
            db_session,
            **_release_kwargs(
                index=2,
                version_id=f"{WINDOW_ID}_v002",
            ),
        )


def test_version_schema_validation_required_fields() -> None:
    with pytest.raises(ValidationError):
        VersionSaveRequest.model_validate({})
    assert VersionSaveRequest(session_id=SESSION_ID).generate_review is True

    with pytest.raises(ValidationError):
        VersionReviewRequest.model_validate({"version_id": f"{WINDOW_ID}_v001"})
    with pytest.raises(ValidationError):
        VersionReviewRequest(version_id=f"{WINDOW_ID}_v001", action="hold")

    with pytest.raises(ValidationError):
        VersionSubmitRequest.model_validate({})
    with pytest.raises(ValidationError):
        VersionReleaseRequest.model_validate({})


def test_version_review_request_reject_requires_comment() -> None:
    with pytest.raises(ValidationError):
        VersionReviewRequest(version_id="x", action="reject", comment=None)
    with pytest.raises(ValidationError):
        VersionReviewRequest(version_id="x", action="reject", comment="")

    reject = VersionReviewRequest(version_id="x", action="reject", comment="需修订")
    assert reject.comment == "需修订"

    approve = VersionReviewRequest(version_id="x", action="approve", comment=None)
    assert approve.comment is None


async def test_version_schema_from_attributes(db_session: AsyncSession) -> None:
    version = await edit_version_repo.create(db_session, **_version_kwargs())
    version.has_images = True

    list_item = VersionListItem.model_validate(version)
    assert list_item.version_id == f"{WINDOW_ID}_v001"
    assert list_item.window_id == WINDOW_ID
    assert list_item.version_no == 1
    assert list_item.base_version_id == f"{WINDOW_ID}_v000"
    assert list_item.status == "draft"
    assert list_item.has_images is True
    assert list_item.created_by == "forecaster"

    detail = VersionDetail.model_validate(version)
    assert detail.version_id == f"{WINDOW_ID}_v001"
    assert detail.base_version_id == f"{WINDOW_ID}_v000"
    assert detail.session_id == SESSION_ID
    assert detail.before_image_path == "/data/v001/before.png"
    assert detail.review_image_path == "/data/v001/review.png"
