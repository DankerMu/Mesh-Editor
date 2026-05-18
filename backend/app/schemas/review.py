from __future__ import annotations

from datetime import datetime
from enum import StrEnum
from typing import Literal

from pydantic import BaseModel, ConfigDict


class PlotTaskStatus(StrEnum):
    PENDING = "pending"
    RUNNING = "running"
    SUCCESS = "success"
    PARTIAL_SUCCESS = "partial_success"
    FAILED = "failed"
    PERMANENTLY_FAILED = "permanently_failed"
    SUPERSEDED = "superseded"


class MissingField(BaseModel):
    variable_name: str
    level_type: str | None = None
    level_value: int | None = None
    lead_hour: int | None = None
    reason: Literal["file_not_found", "read_error", "dimension_mismatch"]


class ReviewGenerateRequest(BaseModel):
    window_id: str
    version_id: str
    template_id: str


class ReviewGenerateResponse(BaseModel):
    review_id: str
    plot_status: str
    message: str


class ReviewExportRequest(BaseModel):
    review_id: str


class ReviewProductDetail(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    review_id: str
    window_id: str
    version_id: str
    template_id: str
    image_path: str | None = None
    plot_config_path: str | None = None
    plot_input_manifest_path: str | None = None
    plot_code_version: str | None = None
    plot_status: str
    attempt: int
    max_retries: int
    locked_by: str | None = None
    locked_at: datetime | None = None
    next_retry_at: datetime | None = None
    plot_started_at: datetime | None = None
    plot_finished_at: datetime | None = None
    total_panels: int | None = None
    success_panels: int | None = None
    skipped_panels: int | None = None
    missing_fields_json: str | None = None
    error_log_path: str | None = None
    created_at: datetime


class ReviewProductListItem(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    review_id: str
    window_id: str
    version_id: str
    template_id: str
    plot_status: str
    image_path: str | None = None
    total_panels: int | None = None
    success_panels: int | None = None
    skipped_panels: int | None = None
    created_at: datetime


class ReviewProductListResponse(BaseModel):
    items: list[ReviewProductListItem]
    total: int
    page: int
    page_size: int


class ReviewProductVersionItem(ReviewProductListItem):
    version_no: int | None = None
    version_status: str | None = None
    version_created_by: str | None = None
    version_created_at: datetime | None = None
