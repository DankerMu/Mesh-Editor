from __future__ import annotations

import io
import json
import zipfile
from collections.abc import Iterable
from pathlib import Path
from typing import Any

from fastapi import APIRouter, Depends, Query, Request
from fastapi.responses import StreamingResponse
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.dependencies import require_role
from app.core.error_registry import ERROR_INFO, get_error
from app.core.errors import DomainError
from app.core.logging import get_trace_id
from app.db.models import AppUser, AuditLog, ReviewProduct
from app.db.session import get_db
from app.repositories.review_product_repo import review_product_repo
from app.schemas.common import ApiResponse
from app.schemas.review import (
    ReviewExportRequest,
    ReviewGenerateRequest,
    ReviewGenerateResponse,
    ReviewProductDetail,
    ReviewProductListItem,
    ReviewProductListResponse,
    ReviewProductVersionItem,
)
from app.services.review_service import generate_review
from app.storage.path_builder import path_builder

router = APIRouter(prefix="/review", tags=["Reviews"])
list_router = APIRouter(tags=["Reviews"])

_REVIEW_READY_STATUSES = {"success", "partial_success"}
_SERVICE_ERROR_CODES = {
    "VERSION_NOT_FOUND",
    "VERSION_STATUS_CONFLICT",
    "TEMPLATE_NOT_FOUND",
    "WINDOW_NOT_FOUND",
    "REQUIRED_FIELD_MISSING",
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


def _service_error(exc: ValueError, detail: dict[str, Any] | None = None) -> DomainError:
    message = str(exc)
    for code in _SERVICE_ERROR_CODES:
        if ERROR_INFO[code].message == message:
            return _domain_error(code, detail)
    return _domain_error("INTERNAL_ERROR", {"message": message})


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
            resource_type="review",
            resource_id=resource_id,
            detail_json=json.dumps(detail or {}, ensure_ascii=False),
        )
    )
    await db.flush()


async def _get_review_or_404(db: AsyncSession, review_id: str) -> ReviewProduct:
    product = await review_product_repo.get_by_id(db, review_id)
    if product is None:
        raise _domain_error("REVIEW_NOT_FOUND", {"review_id": review_id})
    return product


def _detail(product: ReviewProduct) -> ReviewProductDetail:
    return ReviewProductDetail.model_validate(product)


def _list_item(product: ReviewProduct) -> ReviewProductListItem:
    return ReviewProductListItem.model_validate(product)


def _version_item(product: ReviewProduct) -> ReviewProductVersionItem:
    return ReviewProductVersionItem.model_validate(product)


@router.post(
    "/generate",
    response_model=ApiResponse[ReviewGenerateResponse],
)
async def generate_review_endpoint(
    payload: ReviewGenerateRequest,
    request: Request,
    db: AsyncSession = Depends(get_db),
    current_user: AppUser = Depends(require_role("admin", "reviewer", "forecaster")),
) -> ApiResponse[ReviewGenerateResponse]:
    try:
        data = await generate_review(
            db,
            payload.window_id,
            payload.version_id,
            payload.template_id,
            str(current_user.id),
            path_builder,
        )
    except ValueError as exc:
        raise _service_error(exc, payload.model_dump()) from exc

    await _write_audit_log(
        db,
        user=current_user,
        action="review_generate",
        resource_id=data.review_id,
        detail=payload.model_dump(),
    )
    await db.commit()
    return ApiResponse(data=data, trace_id=_trace_id(request))


@router.post("/export")
async def export_review(
    payload: ReviewExportRequest,
    db: AsyncSession = Depends(get_db),
) -> StreamingResponse:
    product = await _get_review_or_404(db, payload.review_id)
    if str(product.plot_status) not in _REVIEW_READY_STATUSES:
        raise _domain_error(
            "REVIEW_NOT_READY",
            {"review_id": payload.review_id, "plot_status": str(product.plot_status)},
        )

    review_root = path_builder.review_root(str(product.window_id), str(product.review_id))
    buffer = _build_review_zip(review_root)
    filename = f"review_package_{product.window_id}_{product.version_id}.zip"
    return StreamingResponse(
        iter([buffer.getvalue()]),
        media_type="application/zip",
        headers={"Content-Disposition": f'attachment; filename="{filename}"'},
    )


@router.get(
    "/case/{case_id}",
    response_model=ApiResponse[list[ReviewProductListItem]],
)
async def list_reviews_by_case(
    case_id: str,
    request: Request,
    db: AsyncSession = Depends(get_db),
) -> ApiResponse[list[ReviewProductListItem]]:
    products = await review_product_repo.list_by_case(db, case_id)
    return ApiResponse(
        data=[_list_item(product) for product in products],
        trace_id=_trace_id(request),
    )


@router.get(
    "/window/{window_id}/versions",
    response_model=ApiResponse[list[ReviewProductVersionItem]],
)
async def list_reviews_by_window_versions(
    window_id: str,
    request: Request,
    db: AsyncSession = Depends(get_db),
) -> ApiResponse[list[ReviewProductVersionItem]]:
    products = await review_product_repo.list_by_window_with_versions(db, window_id)
    return ApiResponse(
        data=[_version_item(product) for product in products],
        trace_id=_trace_id(request),
    )


@router.get(
    "/{review_id}",
    response_model=ApiResponse[ReviewProductDetail],
)
async def get_review_detail(
    review_id: str,
    request: Request,
    db: AsyncSession = Depends(get_db),
) -> ApiResponse[ReviewProductDetail]:
    product = await _get_review_or_404(db, review_id)
    return ApiResponse(data=_detail(product), trace_id=_trace_id(request))


@list_router.get(
    "/tasks/plot/{review_id}",
    response_model=ApiResponse[ReviewProductDetail],
)
async def get_plot_task(
    review_id: str,
    request: Request,
    db: AsyncSession = Depends(get_db),
) -> ApiResponse[ReviewProductDetail]:
    product = await _get_review_or_404(db, review_id)
    return ApiResponse(data=_detail(product), trace_id=_trace_id(request))


@list_router.get(
    "/reviews",
    response_model=ApiResponse[ReviewProductListResponse],
)
async def list_reviews(
    request: Request,
    case_id: str | None = Query(None),
    window_id: str | None = Query(None),
    plot_status: str | None = Query(None),
    page: int = Query(1, ge=1),
    page_size: int = Query(20, ge=1, le=100),
    db: AsyncSession = Depends(get_db),
) -> ApiResponse[ReviewProductListResponse]:
    products = await review_product_repo.list_all(
        db, case_id=case_id, window_id=window_id, plot_status=plot_status
    )
    start = (page - 1) * page_size
    paged = products[start : start + page_size]
    return ApiResponse(
        data=ReviewProductListResponse(
            items=[_list_item(product) for product in paged],
            total=len(products),
            page=page,
            page_size=page_size,
        ),
        trace_id=_trace_id(request),
    )


def _build_review_zip(review_root: Path) -> io.BytesIO:
    buffer = io.BytesIO()
    added: set[str] = set()
    with zipfile.ZipFile(buffer, mode="w", compression=zipfile.ZIP_DEFLATED) as archive:
        _add_file(archive, review_root / "review_payload.json", "review_payload.json", added)
        _add_files(
            archive,
            _iter_files(review_root / "images"),
            review_root,
            added,
        )
        _add_file(archive, review_root / "plot_log.txt", "plot_log.txt", added)
        _add_files(archive, review_root.rglob("*.npz"), review_root, added)
    buffer.seek(0)
    return buffer


def _iter_files(path: Path) -> Iterable[Path]:
    if not path.exists():
        return []
    return (item for item in path.rglob("*") if item.is_file())


def _add_files(
    archive: zipfile.ZipFile,
    paths: Iterable[Path],
    review_root: Path,
    added: set[str],
) -> None:
    for path in paths:
        _add_file(archive, path, str(path.relative_to(review_root)), added)


def _add_file(
    archive: zipfile.ZipFile,
    path: Path,
    arcname: str,
    added: set[str],
) -> None:
    if not path.exists() or not path.is_file() or arcname in added:
        return
    archive.write(path, arcname)
    added.add(arcname)
