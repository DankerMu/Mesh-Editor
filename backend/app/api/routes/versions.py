from __future__ import annotations

import json
from pathlib import Path
from typing import Any

import numpy as np
import numpy.typing as npt
from fastapi import APIRouter, Depends, Query, Request, Response
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.dependencies import require_role
from app.core.constants import NX, NY
from app.core.error_registry import get_error
from app.core.errors import DomainError
from app.core.logging import get_trace_id
from app.db.models import AppUser, AuditLog, EditVersion, ProductWindow
from app.db.session import get_db
from app.repositories.edit_version_repo import edit_version_repo
from app.repositories.review_approval_repo import review_approval_repo
from app.schemas.common import ApiResponse
from app.schemas.version import (
    VersionListItem,
    VersionReleaseRequest,
    VersionReviewRequest,
    VersionSaveRequest,
    VersionSaveResponse,
    VersionSubmitRequest,
)
from app.services.approval_service import approval_service
from app.services.release_service import release_service
from app.services.version_service import version_service

router = APIRouter(prefix="/version", tags=["Versions"])
list_router = APIRouter(prefix="/versions", tags=["Versions"])

VERSION_FIELDS = {
    "qpf_before",
    "ptype_before",
    "qpf_after",
    "ptype_after",
    "delta_qpf",
    "change_ptype",
    "touched_mask",
    "changed_mask",
}
FLOAT_FIELDS = {"qpf_before", "qpf_after", "delta_qpf"}
INT8_FIELDS = {"change_ptype"}
GRID_SHAPE = (NY, NX)
IMAGE_KEYS = {
    "before_product": "before_image_path",
    "after_product": "after_image_path",
    "delta_qpf": "delta_qpf_image_path",
    "change_ptype": "change_ptype_image_path",
    "touched_mask": "touched_mask_image_path",
    "changed_mask": "changed_mask_image_path",
}


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


def _field_dtype(field_name: str) -> np.dtype[Any]:
    if field_name in FLOAT_FIELDS:
        return np.dtype("float32")
    if field_name in INT8_FIELDS:
        return np.dtype("int8")
    return np.dtype("uint8")


def _field_headers(field_name: str, byte_length: int) -> dict[str, str]:
    return {
        "X-Grid-Rows": str(NY),
        "X-Grid-Cols": str(NX),
        "X-Grid-Dtype": str(_field_dtype(field_name)),
        "X-Grid-Order": "C",
        "X-Grid-Byte-Length": str(byte_length),
        "X-Grid-Variable": field_name,
    }


async def _write_audit_log(
    db: AsyncSession,
    *,
    user: AppUser,
    action: str,
    resource_id: str | None,
    detail: dict[str, Any] | None = None,
) -> None:
    db.add(
        AuditLog(
            user_id=int(user.id),
            username=str(user.username),
            action=action,
            resource_type="version",
            resource_id=resource_id,
            detail_json=json.dumps(detail or {}, ensure_ascii=False),
        )
    )
    await db.flush()


def _version_list_item(version: EditVersion) -> VersionListItem:
    return VersionListItem(
        version_id=str(version.version_id),
        window_id=str(version.window_id),
        version_no=int(version.version_no),
        base_version_id=(
            None if version.base_version_id is None else str(version.base_version_id)
        ),
        status=str(version.status),
        has_images=any(
            bool(getattr(version, attr_name)) for attr_name in IMAGE_KEYS.values()
        ),
        created_by=None if version.created_by is None else str(version.created_by),
        created_at=version.created_at,  # type: ignore[arg-type]
    )


@router.post(
    "/save",
    response_model=ApiResponse[VersionSaveResponse],
)
async def save_version(
    payload: VersionSaveRequest,
    request: Request,
    db: AsyncSession = Depends(get_db),
    current_user: AppUser = Depends(require_role("admin", "reviewer", "forecaster")),
) -> ApiResponse[VersionSaveResponse]:
    data = await version_service.save_version(
        db, payload.session_id, payload.generate_review
    )
    await _write_audit_log(
        db,
        user=current_user,
        action="version_save",
        resource_id=str(data["version_id"]),
        detail={"session_id": payload.session_id},
    )
    await db.commit()
    return ApiResponse(
        data=VersionSaveResponse.model_validate(data),
        trace_id=_trace_id(request),
    )


@router.post("/submit", response_model=ApiResponse[dict[str, Any]])
async def submit_version(
    payload: VersionSubmitRequest,
    request: Request,
    db: AsyncSession = Depends(get_db),
    current_user: AppUser = Depends(require_role("admin", "reviewer", "forecaster")),
) -> ApiResponse[dict[str, Any]]:
    data = await version_service.submit(db, payload.version_id)
    await _write_audit_log(
        db,
        user=current_user,
        action="version_submit",
        resource_id=payload.version_id,
    )
    await db.commit()
    return ApiResponse(data=data, trace_id=_trace_id(request))


@router.post("/review", response_model=ApiResponse[dict[str, Any]])
async def review_version(
    payload: VersionReviewRequest,
    request: Request,
    db: AsyncSession = Depends(get_db),
    current_user: AppUser = Depends(require_role("admin", "reviewer")),
) -> ApiResponse[dict[str, Any]]:
    data = await approval_service.review(
        db,
        payload.version_id,
        str(current_user.id),
        payload.action,
        payload.comment,
    )
    await _write_audit_log(
        db,
        user=current_user,
        action="version_review",
        resource_id=payload.version_id,
        detail={"action": payload.action, "approval_id": data.get("approval_id")},
    )
    await db.commit()
    return ApiResponse(data=data, trace_id=_trace_id(request))


@router.post("/release", response_model=ApiResponse[dict[str, Any]])
async def release_version(
    payload: VersionReleaseRequest,
    request: Request,
    db: AsyncSession = Depends(get_db),
    current_user: AppUser = Depends(require_role("admin", "reviewer")),
) -> ApiResponse[dict[str, Any]]:
    data = await release_service.release(db, payload.version_id, str(current_user.id))
    await _write_audit_log(
        db,
        user=current_user,
        action="version_release",
        resource_id=payload.version_id,
        detail={"release_id": data.get("release_id")},
    )
    await db.commit()
    return ApiResponse(data=data, trace_id=_trace_id(request))


@list_router.get("", response_model=ApiResponse[list[VersionListItem]])
async def list_versions(
    request: Request,
    status: str | None = Query(None),
    window_id: str | None = Query(None),
    created_by: str | None = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: AppUser = Depends(require_role("admin", "reviewer", "forecaster")),
) -> ApiResponse[list[VersionListItem]]:
    effective_created_by = (
        str(current_user.id) if current_user.role == "forecaster" else created_by
    )
    versions = await edit_version_repo.list(
        db,
        window_id=window_id,
        status=status,
        created_by=effective_created_by,
    )
    versions.sort(key=lambda version: version.created_at, reverse=True)
    return ApiResponse(
        data=[_version_list_item(version) for version in versions],
        trace_id=_trace_id(request),
    )


@list_router.get("/{version_id}", response_model=ApiResponse[dict[str, Any]])
async def get_version_detail(
    version_id: str,
    request: Request,
    db: AsyncSession = Depends(get_db),
    current_user: AppUser = Depends(require_role("admin", "reviewer", "forecaster")),
) -> ApiResponse[dict[str, Any]]:
    _ = current_user
    version = await edit_version_repo.get(db, version_id)
    if version is None:
        raise _domain_error("VERSION_NOT_FOUND", {"version_id": version_id})

    approvals = await review_approval_repo.list_by_version_id(db, version_id)
    item = _version_list_item(version)
    data: dict[str, Any] = item.model_dump(mode="json")
    data.update(
        {
            "session_id": (
                None if version.session_id is None else str(version.session_id)
            ),
            "image_paths": {
                key: (
                    None if getattr(version, attr_name) is None else str(getattr(version, attr_name))
                )
                for key, attr_name in IMAGE_KEYS.items()
            },
            "field_urls": {
                field: f"/api/version/{version_id}/field/{field}"
                for field in sorted(VERSION_FIELDS)
            },
            "operation_summary": _operation_summary(_version_dir(version)),
            "approval_history": [
                {
                    "approval_id": str(approval.approval_id),
                    "version_id": str(approval.version_id),
                    "reviewer_id": str(approval.reviewer_id),
                    "action": str(approval.action),
                    "comment": approval.comment,
                    "reviewed_at": approval.reviewed_at.isoformat(),
                }
                for approval in approvals
            ],
        }
    )
    data.update(
        {
            attr_name: (
                None if getattr(version, attr_name) is None else str(getattr(version, attr_name))
            )
            for attr_name in [
                "before_image_path",
                "after_image_path",
                "delta_qpf_image_path",
                "change_ptype_image_path",
                "touched_mask_image_path",
                "changed_mask_image_path",
                "review_image_path",
            ]
        }
    )
    return ApiResponse(data=data, trace_id=_trace_id(request))


@router.get("/{version_id}/field/{field_name}")
async def get_version_field(
    version_id: str,
    field_name: str,
    db: AsyncSession = Depends(get_db),
    current_user: AppUser = Depends(require_role("admin", "reviewer", "forecaster")),
) -> Response:
    _ = current_user
    if field_name not in VERSION_FIELDS:
        raise _domain_error(
            "FIELD_NOT_AVAILABLE",
            {"field_name": field_name, "valid_fields": sorted(VERSION_FIELDS)},
        )

    version = await edit_version_repo.get(db, version_id)
    if version is None:
        raise _domain_error("VERSION_NOT_FOUND", {"version_id": version_id})

    data = await _read_version_field(db, version, field_name)
    return Response(
        content=data,
        media_type="application/octet-stream",
        headers=_field_headers(field_name, len(data)),
    )


def _version_dir(version: EditVersion) -> Path:
    qpf_after = Path(str(version.qpf_after_path))
    return qpf_after.parent


def _operation_summary(version_dir: Path) -> dict[str, int]:
    path = version_dir / "operations.jsonl"
    operation_count = 0
    affected_grid_count = 0
    if not path.exists():
        return {
            "operation_count": operation_count,
            "affected_grid_count": affected_grid_count,
        }

    with path.open(encoding="utf-8") as handle:
        for line in handle:
            if not line.strip():
                continue
            operation_count += 1
            try:
                operation = json.loads(line)
            except json.JSONDecodeError:
                continue
            affected_grid_count += _operation_affected_count(operation)
    return {
        "operation_count": operation_count,
        "affected_grid_count": affected_grid_count,
    }


def _operation_affected_count(operation: dict[str, Any]) -> int:
    after_stats = operation.get("after_stats_json")
    if isinstance(after_stats, str) and after_stats:
        try:
            parsed = json.loads(after_stats)
        except json.JSONDecodeError:
            parsed = {}
        if isinstance(parsed, dict):
            return int(parsed.get("count", 0) or 0)
    return 0


async def _read_version_field(
    db: AsyncSession, version: EditVersion, field_name: str
) -> bytes:
    if field_name in {"qpf_before", "ptype_before"}:
        return await _read_base_field(db, version, field_name)

    path_by_field = {
        "qpf_after": version.qpf_after_path,
        "ptype_after": version.ptype_after_path,
        "delta_qpf": version.delta_qpf_path,
        "change_ptype": version.change_ptype_path,
        "touched_mask": version.touched_mask_path,
        "changed_mask": version.changed_mask_path,
    }
    return _read_npz_array(Path(str(path_by_field[field_name])), field_name)


async def _read_base_field(
    db: AsyncSession, version: EditVersion, field_name: str
) -> bytes:
    window = await db.get(ProductWindow, str(version.window_id))
    if window is None:
        raise _domain_error("WINDOW_NOT_FOUND", {"window_id": str(version.window_id)})
    original_dir = version_service.path_builder.window_original_dir(
        str(window.case_id), str(window.window_id)
    )
    filename = {
        "qpf_before": "qpf_before.npz",
        "ptype_before": "ptype_before.npz",
    }[field_name]
    return _read_npz_array(original_dir / filename, field_name)


def _read_npz_array(path: Path, field_name: str) -> bytes:
    if not path.exists():
        raise _domain_error(
            "FIELD_NOT_AVAILABLE", {"field_name": field_name, "path": str(path)}
        )
    with np.load(path) as payload:
        array = payload["data"] if "data" in payload else payload[payload.files[0]]
    return _array_bytes(array, field_name)


def _array_bytes(array: npt.NDArray[Any], field_name: str) -> bytes:
    if array.shape != GRID_SHAPE:
        raise _domain_error(
            "GRID_SHAPE_MISMATCH",
            {
                "field_name": field_name,
                "expected": list(GRID_SHAPE),
                "actual": list(array.shape),
            },
        )
    array = array.astype(_field_dtype(field_name), copy=False)
    return np.ascontiguousarray(array).tobytes(order="C")
