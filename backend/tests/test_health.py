from uuid import UUID

from fastapi.testclient import TestClient


def assert_uuid4(value: str) -> None:
    parsed = UUID(value)
    assert parsed.version == 4


def test_health_returns_success_response(client: TestClient) -> None:
    response = client.get("/api/health")

    assert response.status_code == 200
    body = response.json()
    assert body["code"] == "OK"
    assert body["message"] == "success"
    assert body["data"] == {"status": "healthy"}
    assert_uuid4(body["trace_id"])


def test_health_requires_no_auth(client: TestClient) -> None:
    response = client.get("/api/health")

    assert response.status_code == 200
