from uuid import UUID

from fastapi import Body
from fastapi.testclient import TestClient
from pydantic import BaseModel

from app.core.errors import DomainError
from app.main import app


class Payload(BaseModel):
    name: str


@app.get("/tests/domain-error")
async def raise_domain_error() -> None:
    raise DomainError(
        code="CASE_NOT_FOUND",
        message="案例未找到",
        detail={"case_id": "missing"},
        http_status=404,
    )


@app.post("/tests/validation-error")
async def require_payload(payload: Payload = Body(...)) -> dict[str, str]:
    return {"name": payload.name}


@app.get("/tests/runtime-error")
async def raise_runtime_error() -> None:
    raise RuntimeError("sensitive stack detail")


def assert_uuid4(value: str) -> None:
    parsed = UUID(value)
    assert parsed.version == 4


def test_domain_error_handler(client: TestClient) -> None:
    response = client.get("/tests/domain-error")

    assert response.status_code == 404
    body = response.json()
    assert set(body) == {"code", "message", "detail", "trace_id"}
    assert body["code"] == "CASE_NOT_FOUND"
    assert body["message"] == "案例未找到"
    assert body["detail"] == {"case_id": "missing"}
    assert_uuid4(body["trace_id"])


def test_validation_error_handler(client: TestClient) -> None:
    response = client.post("/tests/validation-error", json={})

    assert response.status_code == 422
    body = response.json()
    assert set(body) == {"code", "message", "detail", "trace_id"}
    assert body["code"] == "VALIDATION_ERROR"
    assert body["message"] == "请求参数校验失败"
    assert isinstance(body["detail"], list)
    assert_uuid4(body["trace_id"])


def test_unhandled_exception_handler_hides_exception_details(client: TestClient) -> None:
    response = client.get("/tests/runtime-error")

    assert response.status_code == 500
    body = response.json()
    assert set(body) == {"code", "message", "detail", "trace_id"}
    assert body["code"] == "INTERNAL_ERROR"
    assert body["message"] == "未分类内部错误"
    assert body["detail"] == {}
    assert "sensitive stack detail" not in response.text
    assert_uuid4(body["trace_id"])


def test_success_and_error_responses_have_trace_id(client: TestClient) -> None:
    success = client.get("/api/health").json()
    error = client.get("/tests/domain-error").json()

    assert_uuid4(success["trace_id"])
    assert_uuid4(error["trace_id"])
    assert success["trace_id"] != error["trace_id"]
