from __future__ import annotations

import json
import os
import tempfile
from pathlib import Path

import numpy as np

from app.engines.ptype_builder import PtypeBuildResult
from app.engines.qpf_builder import QpfBuildResult
from app.storage.path_builder import PathBuilder


def atomic_write_npz(path: Path, **arrays: np.ndarray) -> None:
    target = Path(path)
    target.parent.mkdir(parents=True, exist_ok=True)
    tmp_path: Path | None = None
    try:
        with tempfile.NamedTemporaryFile(
            mode="w+b",
            dir=target.parent,
            prefix=f".{target.name}.",
            suffix=".tmp",
            delete=False,
        ) as tmp_file:
            tmp_path = Path(tmp_file.name)
            np.savez(tmp_file, **arrays)
            tmp_file.flush()
            os.fsync(tmp_file.fileno())
        os.replace(tmp_path, target)
        _fsync_dir(target.parent)
        tmp_path = None
    finally:
        if tmp_path is not None and tmp_path.exists():
            tmp_path.unlink()


def _atomic_write_json(path: Path, payload: dict[str, object]) -> None:
    target = Path(path)
    target.parent.mkdir(parents=True, exist_ok=True)
    tmp_path: Path | None = None
    try:
        with tempfile.NamedTemporaryFile(
            mode="w",
            encoding="utf-8",
            dir=target.parent,
            prefix=f".{target.name}.",
            suffix=".tmp",
            delete=False,
        ) as tmp_file:
            tmp_path = Path(tmp_file.name)
            json.dump(payload, tmp_file, ensure_ascii=False, indent=2)
            tmp_file.write("\n")
            tmp_file.flush()
            os.fsync(tmp_file.fileno())
        os.replace(tmp_path, target)
        _fsync_dir(target.parent)
        tmp_path = None
    finally:
        if tmp_path is not None and tmp_path.exists():
            tmp_path.unlink()


def _fsync_dir(path: Path) -> None:
    dir_fd = os.open(path, os.O_RDONLY)
    try:
        os.fsync(dir_fd)
    finally:
        os.close(dir_fd)


def save_window_original(
    case_id: str,
    window_id: str,
    qpf_result: QpfBuildResult,
    ptype_result: PtypeBuildResult,
    path_builder: PathBuilder,
) -> dict[str, Path]:
    original_dir = path_builder.window_original_dir(case_id, window_id)
    original_dir.mkdir(parents=True, exist_ok=True)

    shape = qpf_result.qpf.shape
    zero_float = np.zeros(shape, dtype=np.float64)
    zero_int = np.zeros(shape, dtype=int)
    zero_bool = np.zeros(shape, dtype=bool)

    saved: dict[str, Path] = {}

    def save_npz(key: str, filename: str, **arrays: np.ndarray) -> None:
        path = original_dir / filename
        atomic_write_npz(path, **arrays)
        saved[key] = path

    save_npz("qpf_before_path", "qpf_before.npz", qpf=qpf_result.qpf)
    save_npz("ptype_before_path", "ptype_before.npz", ptype=ptype_result.ptype)
    save_npz(
        "negative_qpf_mask_path",
        "negative_qpf_mask.npz",
        mask=qpf_result.negative_mask,
    )
    save_npz("negative_mask_path", "negative_mask.npz", mask=qpf_result.negative_mask)
    save_npz("missing_mask_path", "missing_mask.npz", mask=qpf_result.missing_mask)
    save_npz(
        "qpf_missing_mask_path",
        "qpf_missing_mask.npz",
        mask=qpf_result.missing_mask,
    )
    save_npz(
        "v000_delta_qpf_path",
        "v000_delta_qpf.npz",
        delta_qpf=zero_float,
    )
    save_npz(
        "v000_change_ptype_path",
        "v000_change_ptype.npz",
        change_ptype=zero_int,
    )
    save_npz(
        "v000_touched_mask_path",
        "v000_touched_mask.npz",
        mask=zero_bool,
    )
    save_npz(
        "v000_changed_mask_path",
        "v000_changed_mask.npz",
        mask=zero_bool,
    )

    if bool(ptype_result.ptype_invalid_mask.any()):
        save_npz(
            "ptype_invalid_mask_path",
            "ptype_invalid_mask.npz",
            mask=ptype_result.ptype_invalid_mask,
        )

    for key, filename, payload in [
        ("qpf_build_manifest_path", "qpf_build_manifest.json", qpf_result.manifest),
        (
            "ptype_source_manifest_path",
            "ptype_source_manifest.json",
            ptype_result.manifest,
        ),
    ]:
        path = original_dir / filename
        _atomic_write_json(path, payload)
        saved[key] = path

    return saved
