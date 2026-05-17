from datetime import UTC, datetime, timedelta
from pathlib import Path

import numpy as np
import pytest

from app.services.edit_engine import preview_cache
from app.services.edit_engine.preview_cache import (
    PreviewError,
    cleanup_expired,
    cleanup_session,
    create_preview,
    load_preview,
    mark_applied,
)


@pytest.fixture(autouse=True)
def clear_previews() -> None:
    preview_cache._previews.clear()


def _create(tmp_path: Path, session_id: str = "session-1") -> str:
    return create_preview(
        session_id,
        np.ones((2, 2), dtype=np.float32),
        np.ones((2, 2), dtype=np.uint8),
        np.ones((2, 2), dtype=bool),
        {"after": {"sum": 4}},
        [],
        False,
        0,
        tmp_path,
    )


def test_create_generates_preview_id_and_writes_npz(tmp_path: Path) -> None:
    preview_id = _create(tmp_path)

    assert preview_id in preview_cache._previews
    assert (tmp_path / f"{preview_id}.npz").exists()
    assert preview_cache._previews[preview_id]["status"] == "created"


def test_load_valid_preview(tmp_path: Path) -> None:
    preview_id = _create(tmp_path)

    loaded = load_preview(preview_id, "session-1", tmp_path)

    assert loaded["preview_id"] == preview_id
    assert np.array_equal(loaded["qpf_after"], np.ones((2, 2), dtype=np.float32))


def test_load_wrong_session_error(tmp_path: Path) -> None:
    preview_id = _create(tmp_path)

    with pytest.raises(PreviewError) as exc_info:
        load_preview(preview_id, "other-session", tmp_path)

    assert exc_info.value.code == "PREVIEW_CONFLICT"


def test_load_wrong_status_error(tmp_path: Path) -> None:
    preview_id = _create(tmp_path)
    preview_cache._previews[preview_id]["status"] = "overwritten"

    with pytest.raises(PreviewError) as exc_info:
        load_preview(preview_id, "session-1", tmp_path)

    assert exc_info.value.code == "PREVIEW_EXPIRED"


def test_overwrite_marks_previous_preview(tmp_path: Path) -> None:
    old_id = _create(tmp_path)
    new_id = _create(tmp_path)

    assert old_id != new_id
    assert preview_cache._previews[old_id]["status"] == "overwritten"
    assert preview_cache._previews[new_id]["status"] == "created"


def test_mark_applied_status_change_and_file_deletion(tmp_path: Path) -> None:
    preview_id = _create(tmp_path)

    mark_applied(preview_id)

    assert preview_cache._previews[preview_id]["status"] == "applied"
    assert not (tmp_path / f"{preview_id}.npz").exists()


def test_cleanup_expired_deletes_old_files(tmp_path: Path) -> None:
    preview_id = _create(tmp_path)
    preview_cache._previews[preview_id]["created_at"] = datetime.now(UTC) - timedelta(
        minutes=60
    )

    count = cleanup_expired(30, tmp_path)

    assert count == 1
    assert preview_cache._previews[preview_id]["status"] == "expired"
    assert not (tmp_path / f"{preview_id}.npz").exists()


def test_cleanup_session_discards_all_session_previews(tmp_path: Path) -> None:
    first = _create(tmp_path)
    second = _create(tmp_path, session_id="session-2")

    count = cleanup_session("session-1")

    assert count == 1
    assert preview_cache._previews[first]["status"] == "discarded"
    assert preview_cache._previews[second]["status"] == "created"


def test_status_transitions_created_applied_expired_overwritten(tmp_path: Path) -> None:
    applied = _create(tmp_path)
    mark_applied(applied)
    expired = _create(tmp_path, "session-expired")
    preview_cache._previews[expired]["created_at"] = datetime.now(UTC) - timedelta(
        minutes=60
    )
    cleanup_expired(30, tmp_path)
    overwritten = _create(tmp_path, "session-overwrite")
    current = _create(tmp_path, "session-overwrite")

    assert preview_cache._previews[applied]["status"] == "applied"
    assert preview_cache._previews[expired]["status"] == "expired"
    assert preview_cache._previews[overwritten]["status"] == "overwritten"
    assert preview_cache._previews[current]["status"] == "created"
