from __future__ import annotations

import numpy as np
import numpy.typing as npt

from app.core.constants import DLAT, DLON, LAT_MIN


def compute_stats(
    qpf_before: npt.NDArray[np.float32],
    qpf_after: npt.NDArray[np.float32],
    mask: npt.NDArray[np.bool_],
    valid_mask: npt.NDArray[np.bool_],
    lat_indices: npt.NDArray[np.integer],
) -> dict[str, object]:
    effective_mask = np.asarray(mask, dtype=bool) & np.asarray(valid_mask, dtype=bool)
    count = int(np.count_nonzero(effective_mask))
    if count == 0:
        zero_stats = {"min": 0.0, "max": 0.0, "mean": 0.0, "sum": 0.0, "count": 0}
        return {
            "before": zero_stats.copy(),
            "after": zero_stats.copy(),
            "increase_sum": 0.0,
            "decrease_sum": 0.0,
            "max_increase": 0.0,
            "max_decrease": 0.0,
            "area_km2": 0.0,
        }

    before_values = qpf_before[effective_mask].astype(np.float64)
    after_values = qpf_after[effective_mask].astype(np.float64)
    diff = after_values - before_values

    affected_lat_indices = np.asarray(lat_indices)[effective_mask]
    lat_rad = np.deg2rad(LAT_MIN + affected_lat_indices.astype(np.float64) * DLAT)
    dx_km = DLON * 111.32 * np.cos(lat_rad)
    dy_km = DLAT * 110.574
    area_km2 = float(np.sum(dx_km * dy_km))

    increases = diff[diff > 0]
    decreases = -diff[diff < 0]

    return {
        "before": _field_stats(before_values),
        "after": _field_stats(after_values),
        "increase_sum": float(np.sum(increases)) if increases.size else 0.0,
        "decrease_sum": float(np.sum(decreases)) if decreases.size else 0.0,
        "max_increase": float(np.max(increases)) if increases.size else 0.0,
        "max_decrease": float(np.max(decreases)) if decreases.size else 0.0,
        "area_km2": area_km2,
    }


def compute_ptype_transition(
    ptype_before: npt.NDArray[np.uint8],
    ptype_after: npt.NDArray[np.uint8],
    mask: npt.NDArray[np.bool_],
    valid_mask: npt.NDArray[np.bool_],
) -> dict[str, int]:
    effective_mask = np.asarray(mask, dtype=bool) & np.asarray(valid_mask, dtype=bool)
    transitions: dict[str, int] = {}
    for old in range(4):
        for new in range(4):
            transitions[f"{old}_to_{new}"] = int(
                np.count_nonzero(
                    effective_mask & (ptype_before == old) & (ptype_after == new)
                )
            )
    return transitions


def _field_stats(values: npt.NDArray[np.float64]) -> dict[str, float | int]:
    return {
        "min": float(np.min(values)),
        "max": float(np.max(values)),
        "mean": float(np.mean(values)),
        "sum": float(np.sum(values)),
        "count": int(values.size),
    }
