import pytest
from fastapi.testclient import TestClient

from app.main import app

pytest_plugins = ("tests.fixtures.conftest",)


@pytest.fixture()
def client() -> TestClient:
    return TestClient(app, raise_server_exceptions=False)
