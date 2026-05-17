from __future__ import annotations

import json
from collections.abc import Mapping
from pathlib import Path
from typing import Any

import numpy as np
import numpy.typing as npt

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


def replay_operations(
    operations: list[dict[str, Any]],
    qpf_base: npt.NDArray[np.float32],
    ptype_base: npt.NDArray[np.uint8],
    valid_mask: npt.NDArray[np.bool_],
    threshold: float = 0.1,
) -> tuple[npt.NDArray[np.float32], npt.NDArray[np.uint8], npt.NDArray[np.bool_]]:
    qpf_current = qpf_base.copy()
    ptype_current = ptype_base.copy()
    valid = np.asarray(valid_mask, dtype=bool)
    touched_mask = np.zeros(valid.shape, dtype=bool)

    for operation in sorted(operations, key=lambda item: int(item.get("sequence_no", 0))):
        operation_mask = _load_operation_mask(operation, valid.shape) & valid
        touched_mask |= operation_mask
        if int(operation.get("is_undone", 0)) == 1:
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


def _apply_operation(ctx: EditContext, operation: Mapping[str, Any]):
    operation_type = str(operation.get("operation_type", "")).lower()
    parameters = _parameters(operation.get("parameters_json"))

    if operation_type in {"set_value", "qpf_set_value", "set"}:
        return apply_qpf_set_value(ctx, float(_first(parameters, "value", "target_value")))
    if operation_type in {"increase", "qpf_increase"}:
        return apply_qpf_increase(
            ctx,
            float(_first(parameters, "delta_mm", "delta", "value")),
            bool(parameters.get("only_nonzero", False)),
        )
    if operation_type in {"decrease", "qpf_decrease"}:
        return apply_qpf_decrease(
            ctx, float(_first(parameters, "delta_mm", "delta", "value"))
        )
    if operation_type in {"multiply", "qpf_multiply"}:
        return apply_qpf_multiply(ctx, float(_first(parameters, "factor", "value")))
    if operation_type in {"clear", "qpf_clear"}:
        return apply_qpf_clear(ctx)
    if operation_type in {"ptype_set", "set_ptype"}:
        return apply_ptype_set(ctx, int(_first(parameters, "target_ptype", "ptype", "value")))
    if operation_type in {"screen_clear", "qpf_screen_clear"}:
        threshold = parameters.get("threshold")
        return apply_screen_clear(ctx, None if threshold is None else float(threshold))

    raise ValueError(f"unsupported operation_type: {operation_type}")


def _load_operation_mask(
    operation: Mapping[str, Any], shape: tuple[int, ...]
) -> npt.NDArray[np.bool_]:
    for key in ("operation_mask", "mask", "mask_data"):
        value = operation.get(key)
        if value is not None:
            return np.asarray(value, dtype=bool)

    path_value = operation.get("mask_raster_path")
    if path_value:
        with np.load(Path(str(path_value))) as data:
            if "operation_mask" in data:
                return np.asarray(data["operation_mask"], dtype=bool)
            if "mask" in data:
                return np.asarray(data["mask"], dtype=bool)

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
    if isinstance(operation, Mapping):
        return int(operation.get("is_undone", 0))
    return int(getattr(operation, "is_undone", 0))
