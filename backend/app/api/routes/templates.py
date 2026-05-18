from __future__ import annotations

import json
from datetime import UTC, datetime
from typing import Any
from uuid import uuid4

from fastapi import APIRouter, Depends, Request
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.dependencies import get_current_user, require_role
from app.core.error_registry import get_error
from app.core.errors import DomainError
from app.core.logging import get_trace_id
from app.db.models import AppUser, AuditLog
from app.db.session import get_db
from app.repositories.config_snapshot_repo import config_snapshot_repo
from app.schemas.common import ApiResponse
from app.schemas.m6 import (
    ReviewTemplateDetailResponse,
    ReviewTemplatePanelResponse,
    ReviewTemplateSummaryResponse,
    ReviewTemplateUpdateRequest,
    ReviewTemplateUpdateResponse,
)
from app.services.review_templates import (
    ReviewTemplate,
    list_templates,
    load_template,
    save_template,
    template_to_dict,
)


router = APIRouter(prefix="/templates", tags=["Templates"])


def _trace_id(request: Request) -> str:
    return get_trace_id() or str(getattr(request.state, "trace_id", ""))


def _domain_error(
    code: str, detail: dict[str, Any] | None = None, message: str | None = None
) -> DomainError:
    default_message, http_status = get_error(code)
    return DomainError(
        code=code,
        message=message or default_message,
        detail=detail or {},
        http_status=http_status,
    )


def _template_or_404(template_id: str) -> ReviewTemplate:
    try:
        return load_template(template_id)
    except ValueError as exc:
        raise _domain_error("TEMPLATE_NOT_FOUND", {"template_id": template_id}) from exc


def _summary(template: ReviewTemplate) -> ReviewTemplateSummaryResponse:
    return ReviewTemplateSummaryResponse(
        template_id=template.template_id,
        template_name=template.template_name,
        required_fields=template.required_fields,
        optional_fields=template.optional_fields,
        allow_partial_success=template.allow_partial_success,
        review_time_policy=template.review_time_policy,
        panel_count=len(template.panels),
    )


def _detail(template: ReviewTemplate) -> ReviewTemplateDetailResponse:
    return ReviewTemplateDetailResponse(
        template_id=template.template_id,
        template_name=template.template_name,
        required_fields=template.required_fields,
        optional_fields=template.optional_fields,
        allow_partial_success=template.allow_partial_success,
        panels=[
            ReviewTemplatePanelResponse(
                id=panel.id,
                type=panel.type,
                fields=panel.fields,
            )
            for panel in template.panels
        ],
        review_time_policy=template.review_time_policy,
    )


def _validate_template_payload(payload: ReviewTemplateUpdateRequest) -> None:
    if not payload.required_fields:
        raise _domain_error("VALIDATION_ERROR", message="required_fields 不能为空")
    if any(not field for field in payload.required_fields):
        raise _domain_error("VALIDATION_ERROR", message="required_fields 必须是字符串数组")
    if any(not field for field in payload.optional_fields):
        raise _domain_error("VALIDATION_ERROR", message="optional_fields 必须是字符串数组")
    if not payload.panels:
        raise _domain_error("VALIDATION_ERROR", message="panels 不能为空")

    panel_ids: set[str] = set()
    allowed_fields = set(payload.required_fields) | set(payload.optional_fields)
    for panel in payload.panels:
        if panel.id in panel_ids:
            raise _domain_error("VALIDATION_ERROR", message="panels 中存在重复的 id")
        panel_ids.add(panel.id)
        if not panel.fields:
            raise _domain_error("VALIDATION_ERROR", message="panel fields 不能为空")
        for field in panel.fields:
            if field not in allowed_fields:
                raise _domain_error(
                    "VALIDATION_ERROR",
                    {"field": field, "panel_id": panel.id},
                    f"panel 字段 {field} 未在 required_fields 或 optional_fields 中定义",
                )


@router.get("", response_model=ApiResponse[list[ReviewTemplateSummaryResponse]])
async def get_templates(
    request: Request,
    _: AppUser = Depends(get_current_user),
) -> ApiResponse[list[ReviewTemplateSummaryResponse]]:
    return ApiResponse(
        message="查询成功",
        data=[_summary(template) for template in list_templates()],
        trace_id=_trace_id(request),
    )


@router.get("/{template_id}", response_model=ApiResponse[ReviewTemplateDetailResponse])
async def get_template(
    template_id: str,
    request: Request,
    _: AppUser = Depends(get_current_user),
) -> ApiResponse[ReviewTemplateDetailResponse]:
    return ApiResponse(
        message="查询成功",
        data=_detail(_template_or_404(template_id)),
        trace_id=_trace_id(request),
    )


@router.put("/{template_id}", response_model=ApiResponse[ReviewTemplateUpdateResponse])
async def update_template(
    template_id: str,
    payload: ReviewTemplateUpdateRequest,
    request: Request,
    db: AsyncSession = Depends(get_db),
    current_user: AppUser = Depends(require_role("admin")),
) -> ApiResponse[ReviewTemplateUpdateResponse]:
    _template_or_404(template_id)
    _validate_template_payload(payload)

    template_payload = payload.model_dump()
    template_payload["template_id"] = template_id
    template = save_template(template_id, template_payload)
    snapshot_id = str(uuid4())
    created_at = datetime.now(UTC).replace(tzinfo=None)
    config_json = json.dumps(template_to_dict(template), ensure_ascii=False, indent=2)
    try:
        await config_snapshot_repo.create(
            db,
            snapshot_id=snapshot_id,
            config_type="template_config",
            config_json=config_json,
            changed_by=str(current_user.username),
            created_at=created_at,
        )
        db.add(
            AuditLog(
                user_id=int(current_user.id),
                username=str(current_user.username),
                action="config_change",
                resource_type="template",
                resource_id=template_id,
                detail_json=json.dumps(
                    {"snapshot_id": snapshot_id, "template_id": template_id},
                    ensure_ascii=False,
                ),
                ip_address=request.client.host if request.client else None,
                created_at=created_at,
            )
        )
        await db.flush()
        await db.commit()
    except Exception:
        await db.rollback()
        raise
    return ApiResponse(
        message="模板更新成功",
        data=ReviewTemplateUpdateResponse(
            template_id=template_id,
            snapshot_id=snapshot_id,
        ),
        trace_id=_trace_id(request),
    )
