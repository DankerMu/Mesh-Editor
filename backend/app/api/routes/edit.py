from __future__ import annotations

import json
import math
from pathlib import Path
from typing import Any, cast
from uuid import uuid4

import numpy as np
import numpy.typing as npt
from fastapi import APIRouter, Depends, Query, Request
from sqlalchemy import Column
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.dependencies import require_role
from app.core.config import settings
from app.core.constants import NX, NY, REPO_ROOT
from app.core.error_registry import get_error
from app.core.errors import DomainError
from app.core.logging import get_trace_id
from app.db.models import AppUser, AuditLog, EditOperation, EditSession
from app.db.session import get_db
from app.repositories.edit_operation_repo import edit_operation_repo
from app.schemas.common import ApiResponse
from app.schemas.edit import (
    EditApplyRequest,
    EditApplyResponse,
    EditPreviewRequest,
    EditPreviewResponse,
    OperationItem,
    OperationListResponse,
    UndoRedoRequest,
    UndoRedoResponse,
)
from app.services.edit_engine import mask_builder, preview_cache
from app.services.edit_engine.edit_ops import (
    EditContext,
    EditOpError,
    EditResult,
    apply_ptype_set,
    apply_qpf_clear,
    apply_qpf_decrease,
    apply_qpf_increase,
    apply_qpf_multiply,
    apply_qpf_set_value,
    apply_screen_clear,
)
from app.services.edit_engine.mask_builder import MaskError
from app.services.edit_engine.preview_cache import PreviewError
from app.services.edit_engine.replay import compute_can_undo_redo, replay_operations
from app.services.edit_engine.stats_calc import compute_ptype_transition, compute_stats
from app.services.session_service import session_service

router = APIRouter(prefix="/edit", tags=["edit"])

GRID_SHAPE = (NY, NX)
LAT_INDEX_GRID = np.broadcast_to(
    np.arange(NY, dtype=np.int32)[:, None], GRID_SHAPE
)


def _domain_error(code: str, detail: dict[str, Any] | None = None) -> DomainError:
    message, http_status = get_error(code)
    return DomainError(
        code=code,
        message=message,
        detail=detail or {},
        http_status=http_status,
    )


def _trace_id(request: Request) -> str:
    return get_trace_id() or str(getattr(request.state, "trace_id", ""))


async def _get_editing_session(db: AsyncSession, session_id: str) -> EditSession:
    session = await db.get(EditSession, session_id)
    if session is None:
        raise _domain_error("SESSION_NOT_FOUND", {"session_id": session_id})
    if session.status == "expired":
        raise _domain_error("SESSION_EXPIRED", {"session_id": session_id})
    if session.status != "editing":
        raise _domain_error(
            "SESSION_NOT_EDITING",
            {"session_id": session_id, "status": str(session.status)},
        )
    return session


def _session_dir(session_id: str) -> Path:
    return session_service.path_builder.session_root(session_id)


def _preview_dir(session_id: str) -> Path:
    return REPO_ROOT / "tmp" / "previews" / session_id


def _mask_dir(session_id: str) -> Path:
    return REPO_ROOT / "tmp" / "masks" / session_id


def _threshold() -> float:
    return float(settings.product.ptype_qpf_threshold_mm)


def _preview_ttl_minutes() -> float:
    return float(getattr(settings.product, "preview_ttl_minutes", 10))


def _load_array(session_id: str, field_name: str) -> npt.NDArray[Any]:
    path = _session_dir(session_id) / f"{field_name}.npy"
    if not path.exists():
        raise _domain_error(
            "FIELD_NOT_AVAILABLE", {"field_name": field_name, "path": str(path)}
        )
    array = np.load(path)
    if array.shape != GRID_SHAPE:
        raise _domain_error(
            "GRID_SHAPE_MISMATCH",
            {
                "field_name": field_name,
                "expected": list(GRID_SHAPE),
                "actual": list(array.shape),
            },
        )
    return array


def _save_array(session_id: str, field_name: str, array: npt.NDArray[Any]) -> None:
    path = _session_dir(session_id) / f"{field_name}.npy"
    path.parent.mkdir(parents=True, exist_ok=True)
    np.save(path, array)


def _load_session_fields(
    session_id: str,
) -> tuple[
    npt.NDArray[np.float32],
    npt.NDArray[np.float32],
    npt.NDArray[np.uint8],
    npt.NDArray[np.uint8],
    npt.NDArray[np.bool_],
]:
    qpf_before = _load_array(session_id, "qpf_before").astype(np.float32, copy=False)
    qpf_after = _load_array(session_id, "qpf_after").astype(np.float32, copy=False)
    ptype_before = _load_array(session_id, "ptype_before").astype(np.uint8, copy=False)
    ptype_after = _load_array(session_id, "ptype_after").astype(np.uint8, copy=False)
    invalid_mask = _load_array(session_id, "invalid_mask").astype(np.uint8, copy=False)
    valid_mask = ~invalid_mask.astype(bool)
    return qpf_before, qpf_after, ptype_before, ptype_after, valid_mask


def _load_base_fields(
    session_id: str,
) -> tuple[npt.NDArray[np.float32], npt.NDArray[np.uint8], npt.NDArray[np.bool_]]:
    qpf_before = _load_array(session_id, "qpf_before").astype(np.float32, copy=False)
    ptype_before = _load_array(session_id, "ptype_before").astype(np.uint8, copy=False)
    invalid_mask = _load_array(session_id, "invalid_mask").astype(np.uint8, copy=False)
    return qpf_before, ptype_before, ~invalid_mask.astype(bool)


def _coordinates(value: Any) -> list[list[float]]:
    if not isinstance(value, list):
        raise _domain_error("MASK_INVALID_GEOMETRY", {"reason": "coordinates 必须为数组"})
    coordinates: list[list[float]] = []
    for point in value:
        if not isinstance(point, list | tuple) or len(point) != 2:
            raise _domain_error("MASK_INVALID_GEOMETRY", {"reason": "坐标必须为 [lon, lat]"})
        coordinates.append(
            [
                _finite_geometry_float(point[0], "lon"),
                _finite_geometry_float(point[1], "lat"),
            ]
        )
    return coordinates


def _finite_geometry_float(value: Any, name: str) -> float:
    try:
        parsed = float(value)
    except (TypeError, ValueError) as exc:
        raise _domain_error("MASK_INVALID_GEOMETRY", {"reason": f"{name} 必须为数值"}) from exc
    if not math.isfinite(parsed):
        raise _domain_error("MASK_INVALID_GEOMETRY", {"reason": f"{name} 必须为有限数值"})
    return parsed


def _build_mask(payload: EditPreviewRequest, valid_mask: npt.NDArray[np.bool_]) -> npt.NDArray[np.bool_]:
    try:
        if payload.tool == "polygon":
            return mask_builder.polygon_to_mask(
                _coordinates(payload.mask.get("coordinates")), valid_mask
            )
        if payload.tool == "line_buffer":
            return mask_builder.line_buffer_to_mask(
                _coordinates(payload.mask.get("coordinates")),
                _finite_geometry_float(payload.mask.get("width_grid", 0), "width_grid"),
                valid_mask,
            )
        return mask_builder.brush_path_to_mask(
            _coordinates(payload.mask.get("points", payload.mask.get("coordinates"))),
            _finite_geometry_float(payload.mask.get("radius_grid", 0), "radius_grid"),
            valid_mask,
        )
    except MaskError as exc:
        raise _domain_error(exc.code, {"detail": exc.detail}) from exc
    except (TypeError, ValueError) as exc:
        raise _domain_error("MASK_INVALID_GEOMETRY", {"detail": str(exc)}) from exc


def _apply_edit_operation(payload: EditPreviewRequest, ctx: EditContext) -> EditResult:
    params = payload.parameters
    try:
        if payload.operation == "set_value":
            _require_variable(payload, "qpf")
            return apply_qpf_set_value(ctx, _required_float(params.value, "value"))
        if payload.operation == "increase":
            _require_variable(payload, "qpf")
            return apply_qpf_increase(
                ctx,
                _required_float(params.delta_mm, "delta_mm"),
                params.only_nonzero,
            )
        if payload.operation == "decrease":
            _require_variable(payload, "qpf")
            return apply_qpf_decrease(ctx, _required_float(params.delta_mm, "delta_mm"))
        if payload.operation == "multiply":
            _require_variable(payload, "qpf")
            return apply_qpf_multiply(ctx, _required_float(params.factor, "factor"))
        if payload.operation == "clear":
            _require_variable(payload, "qpf")
            return apply_qpf_clear(ctx)
        if payload.operation in {"ptype_set", "set_ptype"}:
            _require_variable(payload, "ptype")
            return apply_ptype_set(
                ctx, _required_int(params.target_ptype, "target_ptype")
            )
        if payload.operation == "screen_clear":
            _require_variable(payload, "qpf")
            return apply_screen_clear(ctx, params.threshold)
    except EditOpError as exc:
        raise _domain_error(exc.code, {"detail": exc.detail}) from exc
    raise _domain_error(
        "INVALID_OPERATION_PARAM", {"operation": payload.operation}
    )


def _require_variable(payload: EditPreviewRequest, expected: str) -> None:
    if payload.variable != expected:
        raise _domain_error(
            "INVALID_OPERATION_PARAM",
            {
                "operation": payload.operation,
                "variable": payload.variable,
                "expected_variable": expected,
            },
        )


def _required_float(value: float | None, name: str) -> float:
    if value is None:
        raise _domain_error("INVALID_OPERATION_PARAM", {"missing": name})
    return float(value)


def _required_int(value: int | None, name: str) -> int:
    if value is None:
        raise _domain_error("INVALID_OPERATION_PARAM", {"missing": name})
    return int(value)


def _new_precip_mask(
    qpf_before: npt.NDArray[np.float32],
    qpf_after: npt.NDArray[np.float32],
    ptype_after: npt.NDArray[np.uint8],
    operation_mask: npt.NDArray[np.bool_],
    valid_mask: npt.NDArray[np.bool_],
) -> npt.NDArray[np.bool_]:
    threshold = _threshold()
    return (
        operation_mask
        & valid_mask
        & (qpf_before <= threshold)
        & (qpf_after > threshold)
        & (ptype_after == 0)
    )


def _response_stats(stats: dict[str, object]) -> tuple[dict[str, Any], dict[str, Any]]:
    before = stats.get("before", {})
    after = stats.get("after", {})
    return dict(before) if isinstance(before, dict) else {}, dict(after) if isinstance(after, dict) else {}


@router.post(
    "/preview",
    response_model=ApiResponse[EditPreviewResponse],
)
async def preview_edit(
    payload: EditPreviewRequest,
    request: Request,
    db: AsyncSession = Depends(get_db),
    current_user: AppUser = Depends(require_role("admin", "reviewer", "forecaster")),
) -> ApiResponse[EditPreviewResponse]:
    _ = current_user
    await _get_editing_session(db, payload.session_id)
    preview_cache.cleanup_expired(_preview_ttl_minutes(), _preview_dir(payload.session_id))

    _, qpf_current, _, ptype_current, valid_mask = _load_session_fields(payload.session_id)
    operation_mask = _build_mask(payload, valid_mask)
    ctx = EditContext(
        qpf_before=qpf_current,
        ptype_before=ptype_current,
        operation_mask=operation_mask,
        valid_mask=valid_mask,
        qpf_ptype_threshold=_threshold(),
    )
    result = _apply_edit_operation(payload, ctx)

    stats = compute_stats(
        qpf_current,
        result.qpf_after,
        operation_mask,
        valid_mask,
        LAT_INDEX_GRID,
    )
    transition = compute_ptype_transition(
        ptype_current,
        result.ptype_after,
        operation_mask,
        valid_mask,
    )
    new_mask = _new_precip_mask(
        qpf_current,
        result.qpf_after,
        result.ptype_after,
        operation_mask,
        valid_mask,
    )
    preview_id = preview_cache.create_preview(
        payload.session_id,
        result.qpf_after,
        result.ptype_after,
        operation_mask,
        stats,
        cast(list[dict[str, object]], result.warnings),
        result.new_precip_needs_ptype,
        result.new_precip_count,
        _preview_dir(payload.session_id),
        request_snapshot=payload.model_dump(mode="json"),
        new_precip_mask=new_mask,
    )
    before_stats, after_stats = _response_stats(stats)
    return ApiResponse(
        data=EditPreviewResponse(
            preview_id=preview_id,
            affected_grid_count=result.affected_grid_count,
            affected_area_km2=_float_value(stats.get("area_km2", 0.0)),
            before_stats=before_stats,
            after_stats=after_stats,
            op_ptype_transition=transition,
            new_precip_needs_ptype=result.new_precip_needs_ptype,
            new_precip_count=result.new_precip_count,
            warnings=result.warnings,
        ),
        trace_id=_trace_id(request),
    )


@router.post(
    "/apply",
    response_model=ApiResponse[EditApplyResponse],
)
async def apply_edit(
    payload: EditApplyRequest,
    request: Request,
    db: AsyncSession = Depends(get_db),
    current_user: AppUser = Depends(require_role("admin", "reviewer", "forecaster")),
) -> ApiResponse[EditApplyResponse]:
    _ = current_user
    session = await _get_editing_session(db, payload.session_id)
    preview_cache.cleanup_expired(_preview_ttl_minutes(), _preview_dir(payload.session_id))
    try:
        preview = preview_cache.load_preview(
            payload.preview_id, payload.session_id, _preview_dir(payload.session_id)
        )
    except PreviewError as exc:
        raise _domain_error(exc.code, {"detail": exc.detail}) from exc

    ptype_before_apply = _load_array(payload.session_id, "ptype_after").astype(
        np.uint8, copy=False
    )
    invalid_mask = _load_array(payload.session_id, "invalid_mask").astype(
        np.uint8, copy=False
    )
    valid_mask = ~invalid_mask.astype(bool)
    qpf_after = np.asarray(preview["qpf_after"], dtype=np.float32)
    ptype_after = np.asarray(preview["ptype_after"], dtype=np.uint8)
    operation_mask = np.asarray(preview["operation_mask"], dtype=bool)
    new_precip_mask = np.asarray(preview["new_precip_mask"], dtype=bool)

    if bool(preview["new_precip_needs_ptype"]):
        if payload.target_ptype is None:
            raise _domain_error(
                "NEW_PRECIP_NEEDS_PTYPE",
                {"new_precip_count": _int_value(preview["new_precip_count"])},
            )
        if payload.target_ptype not in (0, 1, 2, 3):
            raise _domain_error("INVALID_PTYPE", {"target_ptype": payload.target_ptype})
        ptype_after[new_precip_mask] = np.uint8(payload.target_ptype)

    touched_mask = _load_array(payload.session_id, "touched_mask").astype(
        np.uint8, copy=False
    )
    touched_after = (touched_mask.astype(bool) | (operation_mask & valid_mask)).astype(
        np.uint8
    )

    active_sequence = await _max_active_sequence(db, payload.session_id)
    await edit_operation_repo.delete_after_sequence(db, payload.session_id, active_sequence)
    sequence_no = active_sequence + 1
    operation_id = str(uuid4())
    mask_path = _mask_dir(payload.session_id) / f"{operation_id}.npz"
    mask_path.parent.mkdir(parents=True, exist_ok=True)
    np.savez_compressed(mask_path, operation_mask=operation_mask & valid_mask)

    stats_value = preview.get("stats", {})
    stats = stats_value if isinstance(stats_value, dict) else {}
    before_stats, after_stats = _response_stats(stats)
    transition = compute_ptype_transition(
        ptype_before_apply,
        ptype_after,
        operation_mask,
        valid_mask,
    )
    snapshot = preview.get("request_snapshot", {})
    snapshot_dict = snapshot if isinstance(snapshot, dict) else {}
    parameters_value = snapshot_dict.get("parameters", {})
    parameters = dict(parameters_value) if isinstance(parameters_value, dict) else {}
    if bool(preview["new_precip_needs_ptype"]) and payload.target_ptype is not None:
        parameters["target_ptype"] = payload.target_ptype

    operation = await edit_operation_repo.create(
        db,
        operation_id=operation_id,
        session_id=payload.session_id,
        window_id=str(session.window_id),
        sequence_no=sequence_no,
        tool_name=str(snapshot_dict.get("tool", "")),
        variable_name=str(snapshot_dict.get("variable", "")),
        operation_type=str(snapshot_dict.get("operation", "")),
        parameters_json=json.dumps(parameters, ensure_ascii=False),
        mask_geometry_json=json.dumps(snapshot_dict.get("mask", {}), ensure_ascii=False),
        mask_raster_path=str(mask_path),
        before_stats_json=json.dumps(before_stats, ensure_ascii=False),
        after_stats_json=json.dumps(after_stats, ensure_ascii=False),
        op_ptype_transition_json=json.dumps(transition, ensure_ascii=False),
        is_undone=0,
    )
    db.add(
        AuditLog(
            user_id=int(current_user.id),
            username=str(current_user.username),
            action="edit_apply",
            resource_type="operation",
            resource_id=str(operation.operation_id),
            detail_json=json.dumps(
                {
                    "session_id": payload.session_id,
                    "window_id": str(session.window_id),
                    "sequence_no": sequence_no,
                    "preview_id": payload.preview_id,
                },
                ensure_ascii=False,
            ),
        )
    )
    _save_array(payload.session_id, "qpf_after", qpf_after.astype(np.float32, copy=False))
    _save_array(payload.session_id, "ptype_after", ptype_after.astype(np.uint8, copy=False))
    _save_array(payload.session_id, "touched_mask", touched_after)
    db.add(session)
    await db.commit()

    preview_cache.mark_applied(payload.preview_id)
    operations = await edit_operation_repo.query_by_session(db, payload.session_id)
    can_undo, can_redo = compute_can_undo_redo(operations)
    return ApiResponse(
        data=EditApplyResponse(
            operation_id=str(operation.operation_id),
            sequence_no=sequence_no,
            applied=True,
            can_undo=can_undo,
            can_redo=can_redo,
        ),
        trace_id=_trace_id(request),
    )


@router.post(
    "/undo",
    response_model=ApiResponse[UndoRedoResponse],
)
async def undo_edit(
    payload: UndoRedoRequest,
    request: Request,
    db: AsyncSession = Depends(get_db),
    current_user: AppUser = Depends(require_role("admin", "reviewer", "forecaster")),
) -> ApiResponse[UndoRedoResponse]:
    _ = current_user
    await _get_editing_session(db, payload.session_id)
    operations = await edit_operation_repo.query_by_session(db, payload.session_id)
    active = [operation for operation in operations if int(operation.is_undone) == 0]
    if not active:
        raise _domain_error("NOTHING_TO_UNDO", {"session_id": payload.session_id})
    target = max(active, key=lambda operation: int(operation.sequence_no))
    await edit_operation_repo.update_is_undone(db, str(target.operation_id), 1)
    operations = await edit_operation_repo.query_by_session(db, payload.session_id)
    await _replay_and_save(payload.session_id, operations)
    preview_cache.cleanup_session(payload.session_id)
    await db.commit()
    can_undo, can_redo = compute_can_undo_redo(operations)
    return ApiResponse(
        data=UndoRedoResponse(
            can_undo=can_undo,
            can_redo=can_redo,
            operation_count=len(operations),
        ),
        trace_id=_trace_id(request),
    )


@router.post(
    "/redo",
    response_model=ApiResponse[UndoRedoResponse],
)
async def redo_edit(
    payload: UndoRedoRequest,
    request: Request,
    db: AsyncSession = Depends(get_db),
    current_user: AppUser = Depends(require_role("admin", "reviewer", "forecaster")),
) -> ApiResponse[UndoRedoResponse]:
    _ = current_user
    await _get_editing_session(db, payload.session_id)
    operations = await edit_operation_repo.query_by_session(db, payload.session_id)
    undone = [operation for operation in operations if int(operation.is_undone) == 1]
    if not undone:
        raise _domain_error("NOTHING_TO_REDO", {"session_id": payload.session_id})
    target = min(undone, key=lambda operation: int(operation.sequence_no))
    await edit_operation_repo.update_is_undone(db, str(target.operation_id), 0)
    operations = await edit_operation_repo.query_by_session(db, payload.session_id)
    await _replay_and_save(payload.session_id, operations)
    preview_cache.cleanup_session(payload.session_id)
    await db.commit()
    can_undo, can_redo = compute_can_undo_redo(operations)
    return ApiResponse(
        data=UndoRedoResponse(
            can_undo=can_undo,
            can_redo=can_redo,
            operation_count=len(operations),
        ),
        trace_id=_trace_id(request),
    )


@router.get(
    "/operations",
    response_model=ApiResponse[OperationListResponse],
)
async def list_operations(
    request: Request,
    session_id: str = Query(...),
    db: AsyncSession = Depends(get_db),
    current_user: AppUser = Depends(require_role("admin", "reviewer", "forecaster")),
) -> ApiResponse[OperationListResponse]:
    _ = current_user
    await _get_editing_session(db, session_id)
    operations = await edit_operation_repo.query_by_session(db, session_id)
    return ApiResponse(
        data=OperationListResponse(
            operations=[_operation_item(operation) for operation in operations]
        ),
        trace_id=_trace_id(request),
    )


async def _max_active_sequence(db: AsyncSession, session_id: str) -> int:
    operations = await edit_operation_repo.query_by_session(db, session_id)
    active_sequences = [
        int(operation.sequence_no)
        for operation in operations
        if int(operation.is_undone) == 0
    ]
    return max(active_sequences, default=0)


async def _replay_and_save(
    session_id: str, operations: list[EditOperation]
) -> None:
    qpf_base, ptype_base, valid_mask = _load_base_fields(session_id)
    qpf_after, ptype_after, touched_mask = replay_operations(
        operations,
        qpf_base,
        ptype_base,
        valid_mask,
        threshold=_threshold(),
    )
    _save_array(session_id, "qpf_after", qpf_after.astype(np.float32, copy=False))
    _save_array(session_id, "ptype_after", ptype_after.astype(np.uint8, copy=False))
    _save_array(session_id, "touched_mask", touched_mask.astype(np.uint8))


def _operation_item(operation: EditOperation) -> OperationItem:
    after_stats = _json_object(operation.after_stats_json)
    created_at = operation.created_at
    if isinstance(created_at, Column):
        raise _domain_error("INTERNAL_ERROR", {"field": "created_at"})
    return OperationItem(
        sequence_no=int(operation.sequence_no),
        tool_name=str(operation.tool_name),
        operation_type=str(operation.operation_type),
        variable_name=str(operation.variable_name),
        affected_grid_count=int(after_stats.get("count", 0)),
        is_undone=int(operation.is_undone),
        created_at=created_at,
    )


def _json_object(value: object) -> dict[str, Any]:
    if not value:
        return {}
    try:
        loaded = json.loads(str(value))
    except json.JSONDecodeError:
        return {}
    return loaded if isinstance(loaded, dict) else {}


def _float_value(value: object) -> float:
    if isinstance(value, int | float | str):
        return float(value)
    return 0.0


def _int_value(value: object) -> int:
    if isinstance(value, int | float | str):
        return int(value)
    return 0
