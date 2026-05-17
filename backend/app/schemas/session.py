from datetime import datetime

from pydantic import BaseModel, ConfigDict


class SessionStartRequest(BaseModel):
    window_id: str


class SessionStartResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    session_id: str
    window_id: str
    base_version_id: str | None
    status: str
    created_at: datetime


class SessionLoadResponse(BaseModel):
    session_id: str
    window_id: str
    base_version_id: str | None
    status: str
    grid_rows: int = 501
    grid_cols: int = 821
    operation_count: int = 0
    can_undo: bool = False
    can_redo: bool = False
    field_urls: dict[str, str]
    before_image: str | None = None
    after_image: str | None = None
