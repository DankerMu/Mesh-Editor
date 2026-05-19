from __future__ import annotations

from datetime import datetime
from typing import Any, Literal, Self

from pydantic import BaseModel, ConfigDict, Field, model_validator


ToolName = Literal["polygon", "line_buffer", "brush_path", "lasso"]
VariableName = Literal["qpf", "ptype"]
OperationType = Literal[
    "set_value",
    "increase",
    "decrease",
    "multiply",
    "clear",
    "ptype_set",
    "set_ptype",
    "screen_clear",
]


class EditParameters(BaseModel):
    value: float | None = None
    delta_mm: float | None = None
    factor: float | None = None
    target_ptype: int | None = None
    only_nonzero: bool = False
    threshold: float | None = None
    smooth_sigma: float = 0


class EditPreviewRequest(BaseModel):
    session_id: str
    tool: ToolName
    variable: VariableName
    operation: OperationType
    mask: dict[str, Any]
    parameters: EditParameters = Field(default_factory=EditParameters)

    @model_validator(mode="after")
    def validate_static_parameter_rules(self) -> Self:
        if (
            self.operation in {"clear", "decrease", "screen_clear"}
            and self.parameters.target_ptype is not None
        ):
            raise ValueError(
                f"target_ptype must not be set when operation={self.operation}"
            )
        return self


class EditPreviewResponse(BaseModel):
    preview_id: str
    affected_grid_count: int
    affected_area_km2: float
    area_mode: str = "spherical_lonlat_cell_area"
    before_stats: dict[str, Any]
    after_stats: dict[str, Any]
    op_ptype_transition: dict[str, int] | None = None
    new_precip_needs_ptype: bool = False
    new_precip_count: int = 0
    warnings: list[dict[str, Any]] = Field(default_factory=list)
    preview_image: str | None = None


class EditApplyRequest(BaseModel):
    session_id: str
    preview_id: str
    target_ptype: int | None = None


class EditApplyResponse(BaseModel):
    operation_id: str
    sequence_no: int
    applied: bool
    can_undo: bool
    can_redo: bool


class UndoRedoRequest(BaseModel):
    session_id: str


class UndoRedoResponse(BaseModel):
    can_undo: bool
    can_redo: bool
    operation_count: int


class OperationItem(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    sequence_no: int
    tool_name: str
    operation_type: str
    variable_name: str
    affected_grid_count: int
    is_undone: int
    created_at: datetime


class OperationListResponse(BaseModel):
    operations: list[OperationItem]
