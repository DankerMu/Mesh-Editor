from __future__ import annotations

import json
from collections.abc import AsyncIterator
from datetime import UTC, datetime
from pathlib import Path

import numpy as np
import pytest
from sqlalchemy.ext.asyncio import AsyncSession, create_async_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from app.core.errors import DomainError
from app.db.models import AppUser, Base, EditSession, EditVersion, ForecastCase, ProductWindow, ReleaseProduct
from app.repositories.edit_version_repo import edit_version_repo
from app.repositories.release_product_repo import release_product_repo
from app.services.release_service import (
    ReleaseService,
    generate_ptype_transition_csv,
    npz_to_txt,
)
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
def builder(tmp_path: Path) -> PathBuilder:
    return PathBuilder(base_dir=tmp_path)


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


def _write_version_files(builder: PathBuilder, version_id: str) -> dict[str, str]:
    version_dir = builder.version_root(WINDOW_ID, version_id)
    images_dir = builder.version_images_dir(WINDOW_ID, version_id)
    version_dir.mkdir(parents=True, exist_ok=True)
    images_dir.mkdir(parents=True, exist_ok=True)
    qpf_after = np.zeros(GRID_SHAPE, dtype=np.float32)
    qpf_after[0, 0] = 1.234
    ptype_after = np.zeros(GRID_SHAPE, dtype=np.uint8)
    ptype_after[0, 0] = 2
    delta_qpf = qpf_after.copy()
    change_ptype = np.zeros(GRID_SHAPE, dtype=np.int8)
    change_ptype[0, 0] = 1
    touched_mask = np.zeros(GRID_SHAPE, dtype=np.uint8)
    changed_mask = np.zeros(GRID_SHAPE, dtype=np.uint8)
    touched_mask[0, 0] = 1
    changed_mask[0, 0] = 1
    arrays = {
        "qpf_after": qpf_after,
        "ptype_after": ptype_after,
        "delta_qpf": delta_qpf,
        "change_ptype": change_ptype,
        "touched_mask": touched_mask,
        "changed_mask": changed_mask,
    }
    for name, data in arrays.items():
        np.savez_compressed(version_dir / f"{name}.npz", data=data)
    (images_dir / "after_product.png").write_bytes(b"\x89PNG\r\n\x1a\nimage")
    return {
        "qpf_after_path": str(version_dir / "qpf_after.npz"),
        "ptype_after_path": str(version_dir / "ptype_after.npz"),
        "delta_qpf_path": str(version_dir / "delta_qpf.npz"),
        "change_ptype_path": str(version_dir / "change_ptype.npz"),
        "touched_mask_path": str(version_dir / "touched_mask.npz"),
        "changed_mask_path": str(version_dir / "changed_mask.npz"),
        "after_image_path": str(images_dir / "after_product.png"),
    }


def _version_kwargs(
    builder: PathBuilder,
    version_no: int = 1,
    status: str = "approved",
    **overrides: object,
) -> dict[str, object]:
    version_id = f"{WINDOW_ID}_v{version_no:03d}"
    paths = _write_version_files(builder, version_id)
    data: dict[str, object] = {
        "version_id": version_id,
        "window_id": WINDOW_ID,
        "version_no": version_no,
        "base_version_id": None,
        "session_id": SESSION_ID,
        "status": status,
        **paths,
    }
    data.update(overrides)
    return data


async def test_release_happy_path(
    db_session: AsyncSession, builder: PathBuilder
) -> None:
    await edit_version_repo.create(db_session, **_version_kwargs(builder))
    service = ReleaseService(path_builder=builder)

    response = await service.release(db_session, f"{WINDOW_ID}_v001", "reviewer")

    release_dir = builder.release_root(WINDOW_ID, f"{WINDOW_ID}_v001")
    assert response["release_status"] == "active"
    assert (release_dir / "fields" / "qpf_after.npz").exists()
    assert (release_dir / "fields" / "qpf_after.txt").exists()
    assert (release_dir / "derived" / "version_ptype_transition.csv").exists()
    manifest = json.loads(
        (release_dir / "product_manifest.json").read_text(encoding="utf-8")
    )
    assert manifest["version_id"] == f"{WINDOW_ID}_v001"
    assert manifest["grid"]["rows"] == 501
    assert manifest["grid"]["lat_start"] == 25.0
    assert manifest["grid"]["lat_end"] == 50.0
    assert "product_path" not in manifest
    assert set(manifest["images"]) == {
        "before_product",
        "after_product",
        "delta_qpf",
        "change_ptype",
        "touched_mask",
        "changed_mask",
    }
    assert manifest["images"]["after_product"] == "images/after_product.png"
    assert manifest["images"]["before_product"] is None
    version = await db_session.get(EditVersion, f"{WINDOW_ID}_v001")
    assert version is not None
    assert version.status == "released"
    release = await db_session.get(ReleaseProduct, response["release_id"])
    assert release is not None


async def test_release_supersedes_old(
    db_session: AsyncSession, builder: PathBuilder
) -> None:
    await edit_version_repo.create(
        db_session, **_version_kwargs(builder, version_no=1, status="released")
    )
    await release_product_repo.create(
        db_session,
        release_id="release-old",
        version_id=f"{WINDOW_ID}_v001",
        window_id=WINDOW_ID,
        release_status="active",
        product_path="/old",
        manifest_path="/old/product_manifest.json",
        released_by="reviewer",
        released_at=datetime(2026, 5, 17, 8, 0),
    )
    await edit_version_repo.create(
        db_session, **_version_kwargs(builder, version_no=2, status="approved")
    )
    service = ReleaseService(path_builder=builder)

    await service.release(db_session, f"{WINDOW_ID}_v002", "reviewer")

    old_release = await db_session.get(ReleaseProduct, "release-old")
    old_version = await db_session.get(EditVersion, f"{WINDOW_ID}_v001")
    new_version = await db_session.get(EditVersion, f"{WINDOW_ID}_v002")
    assert old_release is not None
    assert old_release.release_status == "superseded"
    assert old_release.superseded_at is not None
    assert old_version is not None
    assert old_version.status == "superseded"
    assert new_version is not None
    assert new_version.status == "released"


async def test_release_non_approved_error(
    db_session: AsyncSession, builder: PathBuilder
) -> None:
    await edit_version_repo.create(
        db_session, **_version_kwargs(builder, status="submitted")
    )
    service = ReleaseService(path_builder=builder)

    with pytest.raises(DomainError) as exc_info:
        await service.release(db_session, f"{WINDOW_ID}_v001", "reviewer")

    assert exc_info.value.code == "VERSION_STATUS_CONFLICT"


def test_npz_to_txt_float32(tmp_path: Path) -> None:
    npz_path = tmp_path / "qpf.npz"
    output_path = tmp_path / "qpf.txt"
    np.savez_compressed(npz_path, data=np.array([[1.234, 0.0]], dtype=np.float32))

    npz_to_txt(npz_path, output_path, "float32")

    assert output_path.read_text(encoding="utf-8").strip() == "1.23,0.00"


def test_npz_to_txt_uint8(tmp_path: Path) -> None:
    npz_path = tmp_path / "ptype.npz"
    output_path = tmp_path / "ptype.txt"
    np.savez_compressed(npz_path, data=np.array([[1, 2, 3]], dtype=np.uint8))

    npz_to_txt(npz_path, output_path, "uint8")

    assert output_path.read_text(encoding="utf-8").strip() == "1,2,3"


def test_ptype_transition_csv(tmp_path: Path) -> None:
    ptype_before = np.array([[0, 1, 2], [3, 1, 0]], dtype=np.uint8)
    ptype_after = np.array([[1, 1, 3], [0, 2, 0]], dtype=np.uint8)
    changed_mask = np.array([[1, 0, 1], [1, 1, 0]], dtype=np.uint8)
    output_path = tmp_path / "transition.csv"

    generate_ptype_transition_csv(ptype_before, ptype_after, changed_mask, output_path)

    lines = output_path.read_text(encoding="utf-8").splitlines()
    assert lines[0] == "from_ptype,to_ptype,grid_count"
    assert "0,1,1" in lines
    assert "2,3,1" in lines
    assert "3,0,1" in lines
    assert "1,2,1" in lines
