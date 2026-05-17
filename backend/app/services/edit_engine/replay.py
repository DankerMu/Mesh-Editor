from __future__ import annotations

import json
import logging
from collections.abc import Mapping
from pathlib import Path
from typing import Any

import numpy as np
import numpy.typing as npt

from app.core.constants import NX, NY
from app.services.edit_engine.edit_ops import (
    EditContext,
    apply_ptype_set,
    apply_qpf_clear,
    apply_qpf_decrease,
    apply_qpf_increase,
    apply_qpf_multiply,
    apply_qpf_set_value,
    apply_screen_clear,
)

logger = logging.getLogger(__name__)
GRID_SHAPE = (NY, NX)


def replay_operations(
    operations: list[Any],
    qpf_base: npt.NDArray[np.float32],
    ptype_base: npt.NDArray[np.uint8],
    valid_mask: npt.NDArray[np.bool_],
    threshold: float = 0.1,
) -> tuple[npt.NDArray[np.float32], npt.NDArray[np.uint8], npt.NDArray[np.bool_]]:
    qpf_current = qpf_base.copy()
    ptype_current = ptype_base.copy()
    valid = np.asarray(valid_mask, dtype=bool)
    touched_mask = np.zeros(valid.shape, dtype=bool)

    for operation in sorted(
        operations, key=lambda item: int(_getattr(item, "sequence_no", 0))
    ):
        operation_mask = _load_operation_mask(operation, valid.shape) & valid
        touched_mask |= operation_mask
        if int(_getattr(operation, "is_undone", 0)) == 1:
            continue

        ctx = EditContext(
            qpf_before=qpf_current,
            ptype_before=ptype_current,
            operation_mask=operation_mask,
            valid_mask=valid,
            qpf_ptype_threshold=threshold,
        )
        result = _apply_operation(ctx, operation)
        qpf_current = result.qpf_after
        ptype_current = result.ptype_after

    return qpf_current, ptype_current, touched_mask


def compute_can_undo_redo(operations: list[Any]) -> tuple[bool, bool]:
    can_undo = any(_get_is_undone(operation) == 0 for operation in operations)
    can_redo = any(_get_is_undone(operation) == 1 for operation in operations)
    return can_undo, can_redo


def _apply_operation(ctx: EditContext, operation: Any):
    operation_type = str(_getattr(operation, "operation_type", "")).lower()
    parameters = _parameters(_getattr(operation, "parameters_json"))

    if operation_type in {"set_value", "qpf_set_value", "set"}:
        result = apply_qpf_set_value(
            ctx, float(_first(parameters, "value", "target_value"))
        )
        return _apply_target_ptype_if_needed(ctx, result, parameters)
    if operation_type in {"increase", "qpf_increase"}:
        result = apply_qpf_increase(
            ctx,
            float(_first(parameters, "delta_mm", "delta", "value")),
            bool(parameters.get("only_nonzero", False)),
        )
        return _apply_target_ptype_if_needed(ctx, result, parameters)
    if operation_type in {"decrease", "qpf_decrease"}:
        result = apply_qpf_decrease(
            ctx, float(_first(parameters, "delta_mm", "delta", "value"))
        )
        return _apply_target_ptype_if_needed(ctx, result, parameters)
    if operation_type in {"multiply", "qpf_multiply"}:
        result = apply_qpf_multiply(ctx, float(_first(parameters, "factor", "value")))
        return _apply_target_ptype_if_needed(ctx, result, parameters)
    if operation_type in {"clear", "qpf_clear"}:
        result = apply_qpf_clear(ctx)
        return _apply_target_ptype_if_needed(ctx, result, parameters)
    if operation_type in {"ptype_set", "set_ptype"}:
        return apply_ptype_set(ctx, int(_first(parameters, "target_ptype", "ptype", "value")))
    if operation_type in {"screen_clear", "qpf_screen_clear"}:
        threshold = parameters.get("threshold")
        result = apply_screen_clear(ctx, None if threshold is None else float(threshold))
        return _apply_target_ptype_if_needed(ctx, result, parameters)

    raise ValueError(f"unsupported operation_type: {operation_type}")


def _apply_target_ptype_if_needed(
    ctx: EditContext,
    result: Any,
    parameters: Mapping[str, Any],
) -> Any:
    if parameters.get("target_ptype") is None:
        return result

    target_ptype = int(parameters["target_ptype"])
    ptype_after = result.ptype_after.copy()
    new_precip = (
        np.asarray(ctx.operation_mask, dtype=bool)
        & np.asarray(ctx.valid_mask, dtype=bool)
        & (ctx.qpf_before <= ctx.qpf_ptype_threshold)
        & (result.qpf_after > ctx.qpf_ptype_threshold)
        & (ptype_after == 0)
    )
    ptype_after[new_precip] = np.uint8(target_ptype)
    return type(result)(
        qpf_after=result.qpf_after,
        ptype_after=ptype_after,
        affected_grid_count=result.affected_grid_count,
        warnings=result.warnings,
        new_precip_needs_ptype=result.new_precip_needs_ptype,
        new_precip_count=result.new_precip_count,
    )


def _load_operation_mask(operation: Any, shape: tuple[int, ...]) -> npt.NDArray[np.bool_]:
    for key in ("operation_mask", "mask", "mask_data"):
        value = _getattr(operation, key)
        if value is not None:
            return np.asarray(value, dtype=bool)

    path_value = _getattr(operation, "mask_raster_path")
    if path_value:
        return _load_mask_from_path(path_value, shape)

    return np.zeros(shape, dtype=bool)


def _load_mask_from_path(path_value: Any, shape: tuple[int, ...]) -> npt.NDArray[np.bool_]:
    try:
        path = Path(str(path_value)).resolve()
        if path.suffix != ".npz":
            raise ValueError("mask path must end with .npz")

        with np.load(path) as data:
            if "operation_mask" in data:
                mask = data["operation_mask"]
            elif "mask" in data:
                mask = data["mask"]
            else:
                raise ValueError("mask npz missing operation_mask or mask array")

            if mask.shape != GRID_SHAPE or mask.shape != shape:
                raise ValueError(f"mask shape mismatch: {mask.shape}")
            if mask.dtype != np.dtype(bool):
                raise ValueError(f"mask dtype mismatch: {mask.dtype}")
            return mask.copy()
    except Exception as exc:
        logger.warning("failed to load replay mask from %s: %s", path_value, exc)
        return np.zeros(shape, dtype=bool)


def _parameters(value: Any) -> dict[str, Any]:
    if value is None or value == "":
        return {}
    if isinstance(value, Mapping):
        return dict(value)
    loaded = json.loads(str(value))
    if isinstance(loaded, dict):
        return loaded
    raise ValueError("parameters_json must decode to an object")


def _first(parameters: Mapping[str, Any], *keys: str) -> Any:
    for key in keys:
        if key in parameters:
            return parameters[key]
    raise ValueError(f"missing operation parameter: {keys[0]}")


def _get_is_undone(operation: Any) -> int:
    return int(_getattr(operation, "is_undone", 0))


def _getattr(obj: Any, key: str, default: Any = None) -> Any:
    if isinstance(obj, Mapping):
        return obj.get(key, default)
    try:
        return obj[key]
    except (KeyError, IndexError, TypeError):
        return getattr(obj, key, default)
