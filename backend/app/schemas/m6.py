from __future__ import annotations

from datetime import date, datetime
from typing import Literal

from pydantic import BaseModel, ConfigDict, Field


UserRole = Literal["admin", "reviewer", "forecaster", "viewer"]
AuditAction = Literal[
    "login",
    "session_start",
    "session_discard",
    "save",
    "version_save",
    "submit",
    "version_submit",
    "approve",
    "reject",
    "version_review",
    "release",
    "version_release",
    "scan",
    "export",
    "config_change",
    "user_manage",
    "task_retry",
    "review_generate",
]


class UserCreateRequest(BaseModel):
    username: str = Field(..., min_length=3, max_length=64)
    display_name: str = Field(..., min_length=1, max_length=64)
    role: UserRole
    password: str = Field(..., min_length=6, max_length=128)


class UserUpdateRequest(BaseModel):
    display_name: str | None = Field(None, min_length=1, max_length=64)
    role: UserRole | None = None
    is_active: bool | None = None


class UserResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    username: str
    display_name: str
    role: str
    is_active: bool
    created_at: datetime
    updated_at: datetime
    last_login_at: datetime | None = None


class UserListResponse(BaseModel):
    items: list[UserResponse]
    total: int
    page: int
    page_size: int


class AuditLogQuery(BaseModel):
    user_id: int | None = None
    action: AuditAction | None = None
    resource_type: str | None = None
    start_date: datetime | None = None
    end_date: datetime | None = None
    page: int = Field(1, ge=1)
    page_size: int = Field(20, ge=1, le=100)


class AuditLogResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    user_id: int | None = None
    username: str
    action: str
    resource_type: str | None = None
    resource_id: str | None = None
    detail_json: str | None = None
    ip_address: str | None = None
    created_at: datetime


class AuditLogListResponse(BaseModel):
    items: list[AuditLogResponse]
    total: int
    page: int
    page_size: int


class ConfigSnapshotResponse(BaseModel):
    snapshot_id: str
    config_type: str
    changed_by: str | None = None
    created_at: datetime


class ConfigSnapshotUpdateResponse(ConfigSnapshotResponse):
    pass


class ConfigHistoryResponse(BaseModel):
    items: list[ConfigSnapshotResponse]
    total: int


class ReviewTemplatePanelResponse(BaseModel):
    id: str
    type: str
    fields: list[str]


class ReviewTemplateSummaryResponse(BaseModel):
    template_id: str
    template_name: str
    required_fields: list[str]
    optional_fields: list[str]
    allow_partial_success: bool
    review_time_policy: str
    panel_count: int


class ReviewTemplateDetailResponse(BaseModel):
    template_id: str
    template_name: str
    required_fields: list[str]
    optional_fields: list[str]
    allow_partial_success: bool
    panels: list[ReviewTemplatePanelResponse]
    review_time_policy: str


class ReviewTemplatePanelUpdateRequest(BaseModel):
    id: str = Field(..., min_length=1)
    type: str = Field(..., min_length=1)
    fields: list[str] = Field(...)


class ReviewTemplateUpdateRequest(BaseModel):
    template_name: str = Field(..., min_length=1)
    required_fields: list[str] = Field(...)
    optional_fields: list[str] = Field(default_factory=list)
    allow_partial_success: bool
    panels: list[ReviewTemplatePanelUpdateRequest] = Field(...)
    review_time_policy: str = Field(..., min_length=1)


class ReviewTemplateUpdateResponse(BaseModel):
    template_id: str
    snapshot_id: str


class StorageBreakdownItem(BaseModel):
    type: str
    size_bytes: int
    size_gb: float
    file_count: int


class StorageSummaryResponse(BaseModel):
    total_bytes: int
    free_bytes: int
    used_bytes: int
    total_gb: float
    used_gb: float
    free_gb: float
    breakdown: list[StorageBreakdownItem]
    last_scan_at: datetime


class TaskCountsResponse(BaseModel):
    pending: int = 0
    running: int = 0
    success: int = 0
    partial_success: int = 0
    failed: int = 0
    permanently_failed: int = 0
    superseded: int = 0


class FailedTaskItem(BaseModel):
    review_id: str
    window_id: str
    plot_status: str
    error_summary: str | None = None
    failed_at: datetime | None = None


class TaskSummaryResponse(BaseModel):
    counts: TaskCountsResponse
    recent_failed: list[FailedTaskItem]


class TaskRetryResponse(BaseModel):
    review_id: str
    plot_status: str


class OperationStatsResponse(BaseModel):
    period: dict[str, str]
    total_sessions: int
    total_operations: int
    total_versions_saved: int
    total_versions_released: int
    by_accum_hours: dict[str, dict[str, int]]
    by_tool: dict[str, int]
    by_operation: dict[str, int]


class TopTransitionItem(BaseModel):
    transition: str
    count: int
    label: str


class PtypeTransitionStatsResponse(BaseModel):
    period: dict[str, str]
    total_operations_with_transitions: int
    matrix: dict[str, int]
    top_transitions: list[TopTransitionItem]


StatsExportFormat = Literal["csv"]
StatsExportInclude = Literal["operations", "ptype_transitions", "version_summary"]


class StatsExportRequest(BaseModel):
    start_date: date
    end_date: date
    format: StatsExportFormat = "csv"
    include: list[StatsExportInclude] = Field(..., min_length=1)
