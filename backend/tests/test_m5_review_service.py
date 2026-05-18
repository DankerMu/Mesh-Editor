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

from app.core.config import settings
from app.db.models import AppUser, Base, EditVersion, ForecastCase, ProductWindow
from app.repositories.review_product_repo import review_product_repo
from app.services.review_service import generate_review, resolve_lead_hours
from app.storage.path_builder import PathBuilder

CASE_ID = "2026051608"
WINDOW_ID = f"{CASE_ID}_ACC24_024_048"
VERSION_ID = f"{WINDOW_ID}_v001"
TEMPLATE_ID = "snow_phase_review_v1"


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
            start_lead=24,
            end_lead=48,
            status="available",
            qc_status="pass",
            negative_count=0,
            missing_count=0,
        )
    )
    await db.commit()


def _builder(tmp_path: Path) -> PathBuilder:
    return PathBuilder(base_dir=tmp_path / "archive", data_source_root=tmp_path / "src")


def _write_npz(path: Path, value: float = 1.0) -> Path:
    path.parent.mkdir(parents=True, exist_ok=True)
    np.savez_compressed(path, data=np.full((4, 5), value, dtype=np.float32))
    return path


def _write_grid(path: Path, value: float) -> Path:
    path.parent.mkdir(parents=True, exist_ok=True)
    np.savetxt(path, np.full((4, 5), value, dtype=np.float64), delimiter=",")
    return path


async def _seed_version(
    db: AsyncSession,
    builder: PathBuilder,
    *,
    status: str = "approved",
    window_id: str = WINDOW_ID,
    missing_field: str | None = None,
) -> EditVersion:
    window = await db.get(ProductWindow, window_id)
    assert window is not None
    version_root = builder.version_root(window_id, VERSION_ID)
    original_root = builder.window_root(window_id) / "original"
    field_paths = {
        "qpf_before": _write_npz(original_root / "qpf_before.npz", 1.0),
        "ptype_before": _write_npz(original_root / "ptype_before.npz", 1.0),
        "qpf_after": _write_npz(version_root / "qpf_after.npz", 2.0),
        "ptype_after": _write_npz(version_root / "ptype_after.npz", 2.0),
        "delta_qpf": _write_npz(version_root / "delta_qpf.npz", 1.0),
        "change_ptype": _write_npz(version_root / "change_ptype.npz", 1.0),
    }
    if missing_field:
        field_paths[missing_field].unlink()

    window.qpf_before_path = str(field_paths["qpf_before"])  # type: ignore[assignment]
    window.ptype_before_path = str(field_paths["ptype_before"])  # type: ignore[assignment]
    version = EditVersion(
        version_id=VERSION_ID,
        window_id=window_id,
        version_no=1,
        base_version_id=None,
        session_id=None,
        status=status,
        qpf_after_path=str(field_paths["qpf_after"]),
        ptype_after_path=str(field_paths["ptype_after"]),
        delta_qpf_path=str(field_paths["delta_qpf"]),
        change_ptype_path=str(field_paths["change_ptype"]),
        touched_mask_path=str(_write_npz(version_root / "touched_mask.npz", 1.0)),
        changed_mask_path=str(_write_npz(version_root / "changed_mask.npz", 1.0)),
        created_by="forecaster",
    )
    db.add(version)
    await db.flush()
    await db.refresh(version)
    return version


def _write_tp_pair(builder: PathBuilder, start: int, end: int) -> None:
    _write_grid(
        builder.data_source_root / CASE_ID / "tp" / f"{CASE_ID[2:]}.{start:03d}",
        2.0,
    )
    _write_grid(
        builder.data_source_root / CASE_ID / "tp" / f"{CASE_ID[2:]}.{end:03d}",
        5.0,
    )


def _write_ifs(builder: PathBuilder, lead: int, variables: list[str] | None = None) -> None:
    for variable in variables or ["z500", "t850", "rh700", "u850", "v850"]:
        _write_npz(
            builder.data_source_root
            / CASE_ID
            / "ifs"
            / variable
            / f"{CASE_ID[2:]}.{lead:03d}.npz",
            1.0,
        )


def _payload(builder: PathBuilder, review_id: str, window_id: str = WINDOW_ID) -> dict:
    return json.loads(
        builder.review_payload_path(window_id, review_id).read_text(encoding="utf-8")
    )


async def test_generate_review_happy_path_writes_payload(
    db_session: AsyncSession, tmp_path: Path
) -> None:
    builder = _builder(tmp_path)
    await _seed_version(db_session, builder)
    _write_tp_pair(builder, 0, 24)
    _write_tp_pair(builder, 48, 72)
    _write_ifs(builder, 36)

    response = await generate_review(
        db_session, WINDOW_ID, VERSION_ID, TEMPLATE_ID, "1", builder
    )

    assert response.plot_status == "pending"
    payload = _payload(builder, response.review_id)
    assert set(payload) == {
        "metadata",
        "edit_fields",
        "review_windows",
        "ifs_fields",
        "missing_fields",
        "template",
        "output",
        "plot_task",
    }
    assert payload["metadata"]["window_id"] == WINDOW_ID
    assert len(payload["ifs_fields"]) == 5
    assert await review_product_repo.get_by_id(db_session, response.review_id) is not None


async def test_optional_ifs_missing_records_missing_field(
    db_session: AsyncSession, tmp_path: Path
) -> None:
    builder = _builder(tmp_path)
    await _seed_version(db_session, builder)
    _write_ifs(builder, 36, variables=["z500", "u850", "v850"])

    response = await generate_review(
        db_session, WINDOW_ID, VERSION_ID, TEMPLATE_ID, "1", builder
    )
    missing = _payload(builder, response.review_id)["missing_fields"]

    assert any(item["variable_name"] == "t850" for item in missing)
    assert any(item["variable_name"] == "rh700" for item in missing)


async def test_required_edit_field_missing_raises_without_product(
    db_session: AsyncSession, tmp_path: Path
) -> None:
    builder = _builder(tmp_path)
    await _seed_version(db_session, builder, missing_field="delta_qpf")

    with pytest.raises(ValueError, match="必需复盘字段缺失"):
        await generate_review(db_session, WINDOW_ID, VERSION_ID, TEMPLATE_ID, "1", builder)

    assert await review_product_repo.list_by_window(db_session, WINDOW_ID) == []


async def test_prev24_boundary_start_zero_sets_null(
    db_session: AsyncSession, tmp_path: Path
) -> None:
    builder = _builder(tmp_path)
    window = await db_session.get(ProductWindow, WINDOW_ID)
    assert window is not None
    window.start_lead = 0  # type: ignore[assignment]
    window.end_lead = 24  # type: ignore[assignment]
    window.accum_hours = 24  # type: ignore[assignment]
    await _seed_version(db_session, builder)

    response = await generate_review(
        db_session, WINDOW_ID, VERSION_ID, TEMPLATE_ID, "1", builder
    )

    assert _payload(builder, response.review_id)["review_windows"]["prev24"] is None


async def test_next24_boundary_sets_null(
    db_session: AsyncSession, tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> None:
    builder = _builder(tmp_path)
    await _seed_version(db_session, builder)
    monkeypatch.setitem(settings.product_config, "max_lead_hours", 48)

    response = await generate_review(
        db_session, WINDOW_ID, VERSION_ID, TEMPLATE_ID, "1", builder
    )

    assert _payload(builder, response.review_id)["review_windows"]["next24"] is None


def test_resolve_lead_hours_middle_end() -> None:
    assert resolve_lead_hours(24, 48, ["middle", "end"]) == [36, 48]


async def test_version_status_conflict(
    db_session: AsyncSession, tmp_path: Path
) -> None:
    builder = _builder(tmp_path)
    await _seed_version(db_session, builder, status="draft")

    with pytest.raises(ValueError, match="当前状态不允许该操作"):
        await generate_review(db_session, WINDOW_ID, VERSION_ID, TEMPLATE_ID, "1", builder)


async def test_template_not_found(db_session: AsyncSession, tmp_path: Path) -> None:
    builder = _builder(tmp_path)
    await _seed_version(db_session, builder)

    with pytest.raises(ValueError, match="复盘模板不存在"):
        await generate_review(db_session, WINDOW_ID, VERSION_ID, "missing", "1", builder)


async def test_prev24_qpf_differential(
    db_session: AsyncSession, tmp_path: Path
) -> None:
    builder = _builder(tmp_path)
    await _seed_version(db_session, builder)
    _write_tp_pair(builder, 0, 24)

    response = await generate_review(
        db_session, WINDOW_ID, VERSION_ID, TEMPLATE_ID, "1", builder
    )
    prev24 = _payload(builder, response.review_id)["review_windows"]["prev24"]
    with np.load(prev24["qpf_path"]) as payload:
        assert np.allclose(payload["data"], 3.0)


async def test_regenerate_supersedes_existing(
    db_session: AsyncSession, tmp_path: Path
) -> None:
    builder = _builder(tmp_path)
    await _seed_version(db_session, builder)

    first = await generate_review(db_session, WINDOW_ID, VERSION_ID, TEMPLATE_ID, "1", builder)
    second = await generate_review(db_session, WINDOW_ID, VERSION_ID, TEMPLATE_ID, "1", builder)
    first_product = await review_product_repo.get_by_id(db_session, first.review_id)
    second_product = await review_product_repo.get_by_id(db_session, second.review_id)

    assert first_product is not None
    assert first_product.plot_status == "superseded"
    assert second_product is not None
    assert second_product.plot_status == "pending"
