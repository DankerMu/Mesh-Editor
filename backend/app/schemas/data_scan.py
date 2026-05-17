from datetime import datetime
from typing import Any

from pydantic import BaseModel, ConfigDict


class ScanRequest(BaseModel):
    case_id: str


class ScanResponse(BaseModel):
    scan_id: str
    status: str


class ScanStatusResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    scan_id: str
    case_id: str
    status: str
    scan_started_at: datetime
    scan_finished_at: datetime | None
    tp_files_found: int
    ptype_files_found: int
    windows_created: int
    windows_updated: int
    errors_json: list[dict[str, Any]] | None
