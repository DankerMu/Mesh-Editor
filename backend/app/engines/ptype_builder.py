from __future__ import annotations

import logging
from dataclasses import dataclass
from datetime import UTC, datetime

import numpy as np
from numpy.typing import NDArray

from app.core.config import ProductConfig
from app.core.constants import NX, NY
from app.core.error_registry import get_error
from app.core.errors import DomainError
from app.engines.grid_io import read_txt_grid
from app.storage.path_builder import PathBuilder

logger = logging.getLogger(__name__)


@dataclass(frozen=True)
class PtypeBuildResult:
    ptype: NDArray[np.integer]
    qc_status: str
    ptype_missing_leads: list[int]
    effective_lead_count: int
    manifest: dict[str, object]
    ptype_invalid_mask: NDArray[np.bool_]
    tp_missing_leads: list[int]
    used_leads: list[int]
    all_leads: list[int]
    step_negative_count: int
    step_nan_count: int


def _window_id(case_id: str, start_lead: int, end_lead: int) -> str:
    accum_hours = end_lead - start_lead
    return f"{case_id}_ACC{accum_hours}_{start_lead:03d}_{end_lead:03d}"


def _raise_validation_error(detail: dict[str, object]) -> None:
    message, http_status = get_error("VALIDATION_ERROR")
    raise DomainError(
        code="VALIDATION_ERROR",
        message=message,
        detail=detail,
        http_status=http_status,
    )


def enumerate_ptype_leads(
    start_lead: int,
    end_lead: int,
    config: ProductConfig,
) -> list[int]:
    max_lead_hours = int(getattr(config, "max_lead_hours", 240))
    lead_step_hours = int(getattr(config, "lead_step_hours", 3))
    if (
        start_lead % 24 != 0
        or end_lead > max_lead_hours
        or end_lead <= start_lead
        or end_lead % lead_step_hours != 0
    ):
        _raise_validation_error(
            {
                "start_lead": start_lead,
                "end_lead": end_lead,
                "max_lead_hours": max_lead_hours,
                "lead_step_hours": lead_step_hours,
            }
        )
    return list(range(start_lead + lead_step_hours, end_lead + 1, lead_step_hours))


def build_ptype_window(
    case_id: str,
    start_lead: int,
    end_lead: int,
    config: ProductConfig,
    path_builder: PathBuilder,
    expected_shape: tuple[int, int] | None = None,
) -> PtypeBuildResult:
    shape = expected_shape or (NY, NX)
    all_leads = enumerate_ptype_leads(start_lead, end_lead, config)
    threshold = float(config.ptype_qpf_threshold_mm)

    has_rain = np.zeros(shape, dtype=bool)
    has_snow = np.zeros(shape, dtype=bool)
    ptype_invalid_mask = np.zeros(shape, dtype=bool)
    ptype_missing_leads: list[int] = []
    tp_missing_leads: list[int] = []
    used_leads: list[int] = []
    step_negative_count = 0
    step_nan_count = 0
    invalid_ptype_leads: list[int] = []
    invalid_ptype_count = 0

    for lead in all_leads:
        ptype_path = path_builder.ptype_file_path(case_id, lead)
        if not ptype_path.exists():
            ptype_missing_leads.append(lead)
            continue

        ptype_grid = read_txt_grid(ptype_path, dtype="int", expected_shape=shape)
        if ptype_grid.invalid_count > 0:
            invalid_ptype_leads.append(lead)
            invalid_ptype_count += ptype_grid.invalid_count
            ptype_invalid_mask |= ptype_grid.invalid_mask
            logger.warning(
                "PTYPE_INVALID_VALUE lead=%s path=%s invalid_count=%s",
                lead,
                ptype_path,
                ptype_grid.invalid_count,
            )
        if ptype_grid.invalid_count == shape[0] * shape[1]:
            ptype_missing_leads.append(lead)
            continue

        current_tp_path = path_builder.tp_file_path(case_id, lead)
        previous_tp_path = path_builder.tp_file_path(case_id, lead - 3)
        if not current_tp_path.exists() or not previous_tp_path.exists():
            tp_missing_leads.append(lead)
            continue

        current_tp = read_txt_grid(
            current_tp_path, dtype="float", expected_shape=shape
        ).array
        previous_tp = read_txt_grid(
            previous_tp_path, dtype="float", expected_shape=shape
        ).array
        qpf_step = current_tp - previous_tp

        step_nan_mask = np.isnan(current_tp) | np.isnan(previous_tp)
        step_negative_mask = np.isfinite(qpf_step) & (qpf_step < 0)
        step_nan_count += int(step_nan_mask.sum())
        step_negative_count += int(step_negative_mask.sum())

        valid_ptype_mask = ~ptype_grid.missing_mask & ~ptype_grid.invalid_mask
        valid_step_mask = (
            ~step_nan_mask
            & ~step_negative_mask
            & (qpf_step > threshold)
            & valid_ptype_mask
        )
        ptype_values = ptype_grid.array
        has_rain |= valid_step_mask & ((ptype_values == 1) | (ptype_values == 3))
        has_snow |= valid_step_mask & ((ptype_values == 2) | (ptype_values == 3))
        used_leads.append(lead)

    ptype = np.zeros(shape, dtype=int)
    ptype[has_rain & ~has_snow] = 1
    ptype[has_snow & ~has_rain] = 2
    ptype[has_rain & has_snow] = 3

    effective_lead_count = len(used_leads)
    if effective_lead_count == 0:
        qc_status = "fail"
    elif (
        ptype_missing_leads
        or tp_missing_leads
        or invalid_ptype_count > 0
        or step_negative_count > 0
        or step_nan_count > 0
    ):
        qc_status = "warn"
    else:
        qc_status = "pass"

    manifest = {
        "window_id": _window_id(case_id, start_lead, end_lead),
        "case_id": case_id,
        "start_lead": start_lead,
        "end_lead": end_lead,
        "all_leads": all_leads,
        "used_leads": used_leads,
        "ptype_missing_leads": ptype_missing_leads,
        "tp_missing_leads": tp_missing_leads,
        "effective_lead_count": effective_lead_count,
        "step_negative_count": step_negative_count,
        "step_nan_count": step_nan_count,
        "qpf_threshold": threshold,
        "synthesis_rule": "has_rain_has_snow",
        "invalid_ptype_leads": invalid_ptype_leads,
        "invalid_ptype_count": invalid_ptype_count,
        "qc_status": qc_status,
        "built_at": datetime.now(UTC).isoformat(),
    }

    return PtypeBuildResult(
        ptype=ptype,
        qc_status=qc_status,
        ptype_missing_leads=ptype_missing_leads,
        effective_lead_count=effective_lead_count,
        manifest=manifest,
        ptype_invalid_mask=ptype_invalid_mask,
        tp_missing_leads=tp_missing_leads,
        used_leads=used_leads,
        all_leads=all_leads,
        step_negative_count=step_negative_count,
        step_nan_count=step_nan_count,
    )
