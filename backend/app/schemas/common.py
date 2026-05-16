from typing import Generic, TypeVar

from pydantic import BaseModel, Field


T = TypeVar("T")


class ApiResponse(BaseModel, Generic[T]):
    code: str = "OK"
    message: str = "success"
    data: T | None = None
    trace_id: str


class ErrorResponse(BaseModel):
    code: str
    message: str
    detail: dict | list = Field(default_factory=dict)
    trace_id: str
