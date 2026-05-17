from fastapi import APIRouter, Depends, Request, Response
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.dependencies import require_role
from app.core.logging import get_trace_id
from app.db.models import AppUser
from app.db.session import get_db
from app.schemas.common import ApiResponse
from app.schemas.session import (
    SessionLoadResponse,
    SessionStartRequest,
    SessionStartResponse,
)
from app.services.session_service import session_service

router = APIRouter(prefix="/session", tags=["session"])
window_router = APIRouter(tags=["session"])


@router.post(
    "/start",
    response_model=ApiResponse[SessionStartResponse],
)
async def start_session(
    payload: SessionStartRequest,
    request: Request,
    db: AsyncSession = Depends(get_db),
    current_user: AppUser = Depends(require_role("admin", "reviewer", "forecaster")),
) -> ApiResponse[SessionStartResponse]:
    session = await session_service.create_session(db, payload.window_id, current_user)
    return ApiResponse(
        data=SessionStartResponse.model_validate(session),
        trace_id=get_trace_id() or str(getattr(request.state, "trace_id", "")),
    )


@router.get(
    "/{session_id}/load",
    response_model=ApiResponse[SessionLoadResponse],
)
async def load_session(
    session_id: str,
    request: Request,
    db: AsyncSession = Depends(get_db),
) -> ApiResponse[SessionLoadResponse]:
    data = await session_service.load_session(db, session_id)
    return ApiResponse(
        data=SessionLoadResponse.model_validate(data),
        trace_id=get_trace_id() or str(getattr(request.state, "trace_id", "")),
    )


@router.get("/{session_id}/field/{field_name}")
async def get_session_field(
    session_id: str,
    field_name: str,
    db: AsyncSession = Depends(get_db),
) -> Response:
    data, headers = await session_service.get_field_data(db, session_id, field_name)
    return Response(
        content=data,
        media_type="application/octet-stream",
        headers=headers,
    )


@window_router.get("/window/{window_id}/field/{field_name}")
async def get_window_field(
    window_id: str,
    field_name: str,
    db: AsyncSession = Depends(get_db),
) -> Response:
    data, headers = await session_service.get_window_field_data(
        db, window_id, field_name
    )
    return Response(
        content=data,
        media_type="application/octet-stream",
        headers=headers,
    )
