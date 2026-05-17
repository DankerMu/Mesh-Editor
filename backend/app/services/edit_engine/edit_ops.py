from __future__ import annotations

import math
from dataclasses import dataclass

import numpy as np
import numpy.typing as npt


class EditOpError(Exception):
    def __init__(self, code: str, detail: str = "") -> None:
        self.code = code
        self.detail = detail
        super().__init__(detail or code)


@dataclass(frozen=True)
class EditContext:
    qpf_before: npt.NDArray[np.float32]
    ptype_before: npt.NDArray[np.uint8]
    operation_mask: npt.NDArray[np.bool_]
    valid_mask: npt.NDArray[np.bool_]
    qpf_ptype_threshold: float = 0.1


@dataclass(frozen=True)
class EditResult:
    qpf_after: npt.NDArray[np.float32]
    ptype_after: npt.NDArray[np.uint8]
    affected_grid_count: int
    warnings: list[dict[str, int | str]]
    new_precip_needs_ptype: bool = False
    new_precip_count: int = 0


def apply_qpf_set_value(ctx: EditContext, value: float) -> EditResult:
    if value < 0:
        raise EditOpError("INVALID_OPERATION_PARAM", "qpf 设定值必须 >= 0")
    if not math.isfinite(value):
        raise EditOpError("INVALID_OPERATION_PARAM", "参数必须是有限数值")
    qpf_after, ptype_after = _copy_fields(ctx)
    mask = _effective_mask(ctx)
    qpf_after[mask] = np.float32(value)
    if value <= ctx.qpf_ptype_threshold:
        ptype_after[mask] = 0
    return _build_result(ctx, qpf_after, ptype_after, mask)


def apply_qpf_increase(
    ctx: EditContext, delta_mm: float, only_nonzero: bool = False
) -> EditResult:
    if delta_mm <= 0:
        raise EditOpError("INVALID_OPERATION_PARAM", "qpf 增量必须 > 0")
    if not math.isfinite(delta_mm):
        raise EditOpError("INVALID_OPERATION_PARAM", "参数必须是有限数值")
    qpf_after, ptype_after = _copy_fields(ctx)
    mask = _effective_mask(ctx)
    if only_nonzero:
        mask = mask & (ctx.qpf_before > ctx.qpf_ptype_threshold)
    qpf_after[mask] = qpf_after[mask] + np.float32(delta_mm)
    return _build_result(ctx, qpf_after, ptype_after, mask)


def apply_qpf_decrease(ctx: EditContext, delta_mm: float) -> EditResult:
    if delta_mm <= 0:
        raise EditOpError("INVALID_OPERATION_PARAM", "qpf 减量必须 > 0")
    if not math.isfinite(delta_mm):
        raise EditOpError("INVALID_OPERATION_PARAM", "参数必须是有限数值")
    qpf_after, ptype_after = _copy_fields(ctx)
    mask = _effective_mask(ctx)
    qpf_after[mask] = np.maximum(qpf_after[mask] - np.float32(delta_mm), np.float32(0))
    return _build_result(ctx, qpf_after, ptype_after, mask)


def apply_qpf_multiply(ctx: EditContext, factor: float) -> EditResult:
    if factor < 0:
        raise EditOpError("INVALID_OPERATION_PARAM", "qpf 乘数必须 >= 0")
    if not math.isfinite(factor):
        raise EditOpError("INVALID_OPERATION_PARAM", "参数必须是有限数值")
    qpf_after, ptype_after = _copy_fields(ctx)
    mask = _effective_mask(ctx)
    qpf_after[mask] = qpf_after[mask] * np.float32(factor)
    if factor == 0:
        ptype_after[mask] = 0
    return _build_result(ctx, qpf_after, ptype_after, mask)


def apply_qpf_clear(ctx: EditContext) -> EditResult:
    qpf_after, ptype_after = _copy_fields(ctx)
    mask = _effective_mask(ctx)
    qpf_after[mask] = 0
    ptype_after[mask] = 0
    return _build_result(ctx, qpf_after, ptype_after, mask)


def apply_ptype_set(ctx: EditContext, target_ptype: int) -> EditResult:
    if target_ptype not in (0, 1, 2, 3):
        raise EditOpError("INVALID_OPERATION_PARAM", "ptype 必须为 0/1/2/3")
    qpf_after, ptype_after = _copy_fields(ctx)
    precip_mask = _effective_mask(ctx) & (ctx.qpf_before > ctx.qpf_ptype_threshold)
    ptype_after[precip_mask] = np.uint8(target_ptype)
    return _build_result(ctx, qpf_after, ptype_after, precip_mask)


def apply_screen_clear(
    ctx: EditContext, threshold: float | None = None
) -> EditResult:
    active_threshold = ctx.qpf_ptype_threshold if threshold is None else threshold
    if active_threshold < 0:
        raise EditOpError("INVALID_OPERATION_PARAM", "筛除阈值必须 >= 0")
    if not math.isfinite(active_threshold):
        raise EditOpError("INVALID_OPERATION_PARAM", "参数必须是有限数值")
    qpf_after, ptype_after = _copy_fields(ctx)
    mask = _effective_mask(ctx) & (ctx.qpf_before <= active_threshold)
    qpf_after[mask] = 0
    ptype_after[mask] = 0
    return _build_result(ctx, qpf_after, ptype_after, mask)


def enforce_qpf_ptype_consistency(
    qpf: npt.NDArray[np.float32],
    ptype: npt.NDArray[np.uint8],
    threshold: float,
    mask: npt.NDArray[np.bool_],
) -> tuple[npt.NDArray[np.uint8], list[dict[str, int | str]]]:
    warnings: list[dict[str, int | str]] = []
    scoped = np.asarray(mask, dtype=bool)

    invalid_ptype = scoped & ~np.isin(ptype, [0, 1, 2, 3])
    invalid_count = int(np.count_nonzero(invalid_ptype))
    if invalid_count:
        ptype[invalid_ptype] = 0
        warnings.append({"code": "PTYPE_INVALID_CORRECTED", "count": invalid_count})

    should_clear = scoped & (qpf <= threshold) & (ptype != 0)
    clear_count = int(np.count_nonzero(should_clear))
    if clear_count:
        ptype[should_clear] = 0
        warnings.append({"code": "PTYPE_AUTO_CLEARED", "count": clear_count})

    missing_ptype = scoped & (qpf > threshold) & (ptype == 0)
    missing_count = int(np.count_nonzero(missing_ptype))
    if missing_count:
        warnings.append({"code": "PTYPE_MISSING", "count": missing_count})

    return ptype, warnings


def detect_new_precip(
    qpf_before: npt.NDArray[np.float32],
    qpf_after: npt.NDArray[np.float32],
    ptype_after: npt.NDArray[np.uint8],
    threshold: float,
    mask: npt.NDArray[np.bool_],
) -> tuple[bool, int]:
    new_precip = (
        np.asarray(mask, dtype=bool)
        & (qpf_before <= threshold)
        & (qpf_after > threshold)
        & (ptype_after == 0)
    )
    count = int(np.count_nonzero(new_precip))
    return count > 0, count


def _copy_fields(
    ctx: EditContext,
) -> tuple[npt.NDArray[np.float32], npt.NDArray[np.uint8]]:
    return ctx.qpf_before.copy(), ctx.ptype_before.copy()


def _effective_mask(ctx: EditContext) -> npt.NDArray[np.bool_]:
    return np.asarray(ctx.operation_mask, dtype=bool) & np.asarray(ctx.valid_mask, dtype=bool)


def _build_result(
    ctx: EditContext,
    qpf_after: npt.NDArray[np.float32],
    ptype_after: npt.NDArray[np.uint8],
    affected_mask: npt.NDArray[np.bool_],
) -> EditResult:
    ptype_after, warnings = enforce_qpf_ptype_consistency(
        qpf_after, ptype_after, ctx.qpf_ptype_threshold, affected_mask
    )
    needs_ptype, new_precip_count = detect_new_precip(
        ctx.qpf_before,
        qpf_after,
        ptype_after,
        ctx.qpf_ptype_threshold,
        affected_mask,
    )
    return EditResult(
        qpf_after=qpf_after,
        ptype_after=ptype_after,
        affected_grid_count=int(np.count_nonzero(affected_mask)),
        warnings=warnings,
        new_precip_needs_ptype=needs_ptype,
        new_precip_count=new_precip_count,
    )
