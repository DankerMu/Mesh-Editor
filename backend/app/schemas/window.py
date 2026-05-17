from datetime import datetime

from pydantic import BaseModel, ConfigDict, RootModel


class WindowItem(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    window_id: str
    accum_hours: int
    start_lead: int
    end_lead: int
    status: str
    qc_status: str
    negative_count: int
    negative_min_value: float | None
    negative_abs_max: float | None
    missing_count: int
    ptype_missing_leads: list[int] | None
    qpf_before_path: str | None
    ptype_before_path: str | None
    data_ready_at: datetime | None
    updated_at: datetime | None


class WindowListResponse(RootModel[list[WindowItem]]):
    root: list[WindowItem]
