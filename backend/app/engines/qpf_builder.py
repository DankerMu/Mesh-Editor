from __future__ import annotations

from dataclasses import dataclass
from datetime import UTC, datetime
from hashlib import sha256
from pathlib import Path

import numpy as np
from numpy.typing import NDArray

from app.core.config import ProductConfig
from app.core.constants import NX, NY
from app.engines.grid_io import read_txt_grid
from app.storage.path_builder import PathBuilder


@dataclass(frozen=True)
class QpfBuildResult:
    qpf: NDArray[np.float64]
    qc_status: str
    negative_count: int
    negative_min_value: float | None
    negative_abs_max: float | None
    missing_count: int
    missing_mask: NDArray[np.bool_]
    manifest: dict[str, object]
    negative_mask: NDArray[np.bool_]
    start_tp_path: Path | None
    end_tp_path: Path


def _window_id(case_id: str, start_lead: int, end_lead: int) -> str:
    accum_hours = end_lead - start_lead
    return f"{case_id}_ACC{accum_hours}_{start_lead:03d}_{end_lead:03d}"


def _sha256(path: Path | None) -> str | None:
    if path is None or not path.exists():
        return None
    digest = sha256()
    with path.open("rb") as file:
        for chunk in iter(lambda: file.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def _failure_result(
    *,
    case_id: str,
    start_lead: int,
    end_lead: int,
    start_path: Path | None,
    end_path: Path,
    expected_shape: tuple[int, int],
    reason: str,
    start_lead_fallback: bool = False,
) -> QpfBuildResult:
    qpf = np.full(expected_shape, np.nan, dtype=np.float64)
    missing_mask = np.ones(expected_shape, dtype=bool)
    negative_mask = np.zeros(expected_shape, dtype=bool)
    manifest = {
        "window_id": _window_id(case_id, start_lead, end_lead),
        "case_id": case_id,
        "start_lead": start_lead,
        "end_lead": end_lead,
        "start_tp_path": str(start_path) if start_path is not None else None,
        "end_tp_path": str(end_path),
        "start_tp_sha256": _sha256(start_path),
        "end_tp_sha256": _sha256(end_path),
        "start_lead_fallback": start_lead_fallback,
        "negative_count": 0,
        "negative_min_value": None,
        "negative_abs_max": None,
        "missing_count": int(missing_mask.sum()),
        "qc_status": "fail",
        "failure_reason": reason,
        "built_at": datetime.now(UTC).isoformat(),
    }
    return QpfBuildResult(
        qpf=qpf,
        qc_status="fail",
        negative_count=0,
        negative_min_value=None,
        negative_abs_max=None,
        missing_count=int(missing_mask.sum()),
        missing_mask=missing_mask,
        manifest=manifest,
        negative_mask=negative_mask,
        start_tp_path=start_path,
        end_tp_path=end_path,
    )


def build_qpf_window(
    case_id: str,
    start_lead: int,
    end_lead: int,
    config: ProductConfig,
    path_builder: PathBuilder,
    expected_shape: tuple[int, int] | None = None,
) -> QpfBuildResult:
    shape = expected_shape or (NY, NX)
    start_path = path_builder.tp_file_path(case_id, start_lead)
    end_path = path_builder.tp_file_path(case_id, end_lead)

    if not end_path.exists():
        return _failure_result(
            case_id=case_id,
            start_lead=start_lead,
            end_lead=end_lead,
            start_path=start_path,
            end_path=end_path,
            expected_shape=shape,
            reason="end_tp_missing",
        )

    end_grid = read_txt_grid(end_path, dtype="float", expected_shape=shape)

    start_lead_fallback = False
    if start_path.exists():
        start_grid_array = read_txt_grid(
            start_path, dtype="float", expected_shape=shape
        ).array
    elif start_lead == 0 and config.allow_zero_start_lead_fallback:
        start_grid_array = np.zeros(shape, dtype=np.float64)
        start_lead_fallback = True
    else:
        return _failure_result(
            case_id=case_id,
            start_lead=start_lead,
            end_lead=end_lead,
            start_path=start_path,
            end_path=end_path,
            expected_shape=shape,
            reason="start_tp_missing",
        )

    qpf = end_grid.array.astype(np.float64, copy=False) - start_grid_array.astype(
        np.float64, copy=False
    )
    missing_mask = np.isnan(end_grid.array) | np.isnan(start_grid_array)
    negative_mask = np.isfinite(qpf) & (qpf < 0)
    negative_values = qpf[negative_mask]
    negative_count = int(negative_values.size)
    missing_count = int(missing_mask.sum())
    negative_min_value = float(negative_values.min()) if negative_count > 0 else None
    negative_abs_max = (
        float(np.abs(negative_values).max()) if negative_count > 0 else None
    )
    qc_status = "warn" if negative_count > 0 or missing_count > 0 else "pass"

    manifest = {
        "window_id": _window_id(case_id, start_lead, end_lead),
        "case_id": case_id,
        "start_lead": start_lead,
        "end_lead": end_lead,
        "start_tp_path": str(start_path),
        "end_tp_path": str(end_path),
        "start_tp_sha256": None if start_lead_fallback else _sha256(start_path),
        "end_tp_sha256": _sha256(end_path),
        "start_lead_fallback": start_lead_fallback,
        "negative_count": negative_count,
        "negative_min_value": negative_min_value,
        "negative_abs_max": negative_abs_max,
        "missing_count": missing_count,
        "qc_status": qc_status,
        "built_at": datetime.now(UTC).isoformat(),
    }

    return QpfBuildResult(
        qpf=qpf,
        qc_status=qc_status,
        negative_count=negative_count,
        negative_min_value=negative_min_value,
        negative_abs_max=negative_abs_max,
        missing_count=missing_count,
        missing_mask=missing_mask,
        manifest=manifest,
        negative_mask=negative_mask,
        start_tp_path=None if start_lead_fallback else start_path,
        end_tp_path=end_path,
    )
