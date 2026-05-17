from __future__ import annotations

from datetime import datetime
from typing import Literal, Self

from pydantic import BaseModel, ConfigDict, model_validator


class VersionSaveRequest(BaseModel):
    session_id: str
    generate_review: bool = True


class VersionSaveResponse(BaseModel):
    session_id: str
    version_id: str
    before_image: str | None = None
    after_image: str | None = None
    review_image: str | None = None


class VersionSubmitRequest(BaseModel):
    version_id: str


class VersionReviewRequest(BaseModel):
    version_id: str
    action: Literal["approve", "reject"]
    comment: str | None = None

    @model_validator(mode="after")
    def validate_reject_requires_comment(self) -> Self:
        if self.action == "reject" and not self.comment:
            raise ValueError("审核退回时必须填写意见")
        return self


class VersionReleaseRequest(BaseModel):
    version_id: str


class VersionListItem(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    version_id: str
    window_id: str
    version_no: int
    base_version_id: str | None = None
    status: str
    has_images: bool = False
    created_by: str | None = None
    created_at: datetime


class VersionDetail(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    version_id: str
    window_id: str
    version_no: int
    base_version_id: str | None = None
    session_id: str | None = None
    status: str
    before_image_path: str | None = None
    after_image_path: str | None = None
    delta_qpf_image_path: str | None = None
    change_ptype_image_path: str | None = None
    touched_mask_image_path: str | None = None
    changed_mask_image_path: str | None = None
    review_image_path: str | None = None
    created_by: str | None = None
    created_at: datetime
