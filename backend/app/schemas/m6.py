from __future__ import annotations

from datetime import datetime
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
