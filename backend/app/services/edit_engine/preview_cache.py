from __future__ import annotations

from datetime import UTC, datetime, timedelta
from pathlib import Path
from uuid import uuid4

import numpy as np
import numpy.typing as npt

_previews: dict[str, dict[str, object]] = {}


class PreviewError(Exception):
    def __init__(self, code: str, detail: str = "") -> None:
        self.code = code
        self.detail = detail
        super().__init__(detail or code)


def create_preview(
    session_id: str,
    qpf_after: npt.NDArray[np.float32],
    ptype_after: npt.NDArray[np.uint8],
    operation_mask: npt.NDArray[np.bool_],
    stats: dict[str, object],
    warnings: list[dict[str, object]],
    new_precip_needs_ptype: bool,
    new_precip_count: int,
    preview_dir: str | Path,
) -> str:
    for metadata in _previews.values():
        if metadata["session_id"] == session_id and metadata["status"] == "created":
            metadata["status"] = "overwritten"

    preview_id = str(uuid4())
    directory = Path(preview_dir)
    directory.mkdir(parents=True, exist_ok=True)
    path = directory / f"{preview_id}.npz"
    np.savez_compressed(
        path,
        qpf_after=qpf_after,
        ptype_after=ptype_after,
        operation_mask=operation_mask,
    )
    _previews[preview_id] = {
        "session_id": session_id,
        "status": "created",
        "created_at": datetime.now(UTC),
        "path": str(path),
        "stats": stats,
        "warnings": warnings,
        "new_precip_needs_ptype": new_precip_needs_ptype,
        "new_precip_count": new_precip_count,
    }
    return preview_id


def load_preview(
    preview_id: str, session_id: str, preview_dir: str | Path
) -> dict[str, object]:
    metadata = _previews.get(preview_id)
    if metadata is None:
        raise PreviewError("PREVIEW_EXPIRED", "preview 不存在或已过期")
    if metadata["session_id"] != session_id:
        raise PreviewError("PREVIEW_CONFLICT", "preview 不属于当前 session")
    status = str(metadata["status"])
    if status == "applied":
        raise PreviewError("PREVIEW_CONFLICT", "preview 已应用")
    if status != "created":
        raise PreviewError("PREVIEW_EXPIRED", "preview 已失效")

    path = Path(str(metadata.get("path") or Path(preview_dir) / f"{preview_id}.npz"))
    if not path.exists():
        metadata["status"] = "expired"
        raise PreviewError("PREVIEW_EXPIRED", "preview 文件不存在")

    with np.load(path) as data:
        arrays = {
            "qpf_after": data["qpf_after"].copy(),
            "ptype_after": data["ptype_after"].copy(),
            "operation_mask": data["operation_mask"].copy(),
        }
    return {**metadata, **arrays, "preview_id": preview_id}


def mark_applied(preview_id: str) -> None:
    metadata = _previews.get(preview_id)
    if metadata is None:
        return
    metadata["status"] = "applied"
    path = Path(str(metadata["path"]))
    if path.exists():
        path.unlink()


def cleanup_expired(ttl_minutes: int | float, preview_dir: str | Path) -> int:
    cutoff = datetime.now(UTC) - timedelta(minutes=float(ttl_minutes))
    count = 0
    for metadata in _previews.values():
        if metadata["status"] != "created":
            continue
        created_at = metadata["created_at"]
        if isinstance(created_at, datetime) and created_at <= cutoff:
            metadata["status"] = "expired"
            count += _delete_path(Path(str(metadata["path"])))

    for path in Path(preview_dir).glob("*.npz"):
        if datetime.fromtimestamp(path.stat().st_mtime, tz=UTC) <= cutoff:
            count += _delete_path(path)
    return count


def cleanup_session(session_id: str) -> int:
    count = 0
    for metadata in _previews.values():
        if metadata["session_id"] != session_id:
            continue
        if metadata["status"] != "discarded":
            metadata["status"] = "discarded"
        count += _delete_path(Path(str(metadata["path"])))
    return count


def _delete_path(path: Path) -> int:
    if path.exists():
        path.unlink()
        return 1
    return 0
