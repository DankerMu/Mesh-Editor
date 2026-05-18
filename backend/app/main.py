import logging
from collections.abc import AsyncIterator, Awaitable, Callable
from contextlib import asynccontextmanager

from fastapi import APIRouter, Depends, FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse, Response
from starlette.middleware.gzip import GZipMiddleware

from app.api.dependencies import get_current_user
from app.api.routes.audit import router as audit_router
from app.api.routes.auth import protected_router as auth_router
from app.api.routes.auth import public_router as auth_public_router
from app.api.routes.config import router as config_router
from app.api.routes.data_scan import router as data_scan_router
from app.api.routes.edit import router as edit_router
from app.api.routes.health import router as health_router
from app.api.routes.reviews import list_router as reviews_list_router
from app.api.routes.reviews import router as review_router
from app.api.routes.session import router as session_router
from app.api.routes.session import window_router as session_window_router
from app.api.routes.templates import router as templates_router
from app.api.routes.users import router as users_router
from app.api.routes.versions import list_router as version_list_router
from app.api.routes.versions import router as version_router
from app.api.routes.windows import router as windows_router
from app.core.error_registry import get_error
from app.core.errors import DomainError
from app.core.logging import (
    configure_logging,
    new_trace_id,
    reset_trace_id,
    set_trace_id,
)
from app.schemas.common import ErrorResponse


configure_logging()
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI) -> AsyncIterator[None]:
    from app.services.review_templates import reload_templates

    try:
        reload_templates()
    except Exception:
        logger.warning("Failed to load persisted review templates, using defaults")
    yield
    from app.db.session import engine

    await engine.dispose()


app = FastAPI(title="Mesh Editor API", version="2.0.0", lifespan=lifespan)
app.add_middleware(GZipMiddleware, minimum_size=1000)
api_router = APIRouter(prefix="/api", dependencies=[Depends(get_current_user)])
public_api_router = APIRouter(prefix="/api")


@app.middleware("http")
async def trace_id_middleware(
    request: Request,
    call_next: Callable[[Request], Awaitable[Response]],
) -> Response:
    trace_id = new_trace_id()
    token = set_trace_id(trace_id)
    request.state.trace_id = trace_id
    try:
        response = await call_next(request)
        response.headers["X-Trace-Id"] = trace_id
        return response
    finally:
        reset_trace_id(token)


@app.exception_handler(DomainError)
async def domain_error_handler(request: Request, exc: DomainError) -> JSONResponse:
    return JSONResponse(
        status_code=exc.http_status,
        content=ErrorResponse(
            code=exc.code,
            message=exc.message,
            detail=exc.detail,
            trace_id=getattr(request.state, "trace_id", ""),
        ).model_dump(),
    )


@app.exception_handler(RequestValidationError)
async def validation_error_handler(
    request: Request, exc: RequestValidationError
) -> JSONResponse:
    message, http_status = get_error("VALIDATION_ERROR")
    return JSONResponse(
        status_code=http_status,
        content=ErrorResponse(
            code="VALIDATION_ERROR",
            message=message,
            detail=exc.errors(),
            trace_id=getattr(request.state, "trace_id", ""),
        ).model_dump(),
    )


@app.exception_handler(Exception)
async def generic_error_handler(request: Request, exc: Exception) -> JSONResponse:
    logger.exception("Unhandled exception")
    message, http_status = get_error("INTERNAL_ERROR")
    return JSONResponse(
        status_code=http_status,
        content=ErrorResponse(
            code="INTERNAL_ERROR",
            message=message,
            detail={},
            trace_id=getattr(request.state, "trace_id", ""),
        ).model_dump(),
    )


public_api_router.include_router(health_router)
public_api_router.include_router(auth_public_router)
api_router.include_router(auth_router)
api_router.include_router(data_scan_router)
api_router.include_router(edit_router)
api_router.include_router(session_router)
api_router.include_router(session_window_router)
api_router.include_router(version_router)
api_router.include_router(version_list_router)
api_router.include_router(review_router)
api_router.include_router(reviews_list_router)
api_router.include_router(windows_router)
api_router.include_router(users_router)
api_router.include_router(audit_router)
api_router.include_router(config_router)
api_router.include_router(templates_router)

app.include_router(public_api_router)
app.include_router(api_router)
