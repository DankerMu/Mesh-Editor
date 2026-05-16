from fastapi import APIRouter

from app.core.logging import get_trace_id
from app.schemas.common import ApiResponse


router = APIRouter()


@router.get("/health", response_model=ApiResponse[dict[str, str]])
async def health() -> ApiResponse[dict[str, str]]:
    return ApiResponse(data={"status": "healthy"}, trace_id=get_trace_id())
