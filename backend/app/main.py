import logging
from collections.abc import AsyncIterator, Awaitable, Callable
from contextlib import asynccontextmanager

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse, Response

from app.api.routes.health import router as health_router
from app.core.error_registry import get_error
from app.core.errors import DomainError
from app.core.logging import configure_logging, new_trace_id, reset_trace_id, set_trace_id
from app.schemas.common import ErrorResponse


configure_logging()
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI) -> AsyncIterator[None]:
    yield
    from app.db.session import engine
    await engine.dispose()


app = FastAPI(title="Mesh Editor API", version="2.0.0", lifespan=lifespan)


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
async def validation_error_handler(request: Request, exc: RequestValidationError) -> JSONResponse:
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


app.include_router(health_router, prefix="/api")
