from app.core.error_registry import ERROR_CODES, get_error
from app.schemas.error_codes import ErrorCode


def test_error_registry_loads_auth_required_mapping() -> None:
    message, http_status = get_error("AUTH_REQUIRED")

    assert "AUTH_REQUIRED" in ERROR_CODES
    assert message == "需要登录认证"
    assert http_status == 401


def test_error_code_enum_generated_from_schema() -> None:
    assert ErrorCode.AUTH_REQUIRED.value == "AUTH_REQUIRED"
    assert ErrorCode.INTERNAL_ERROR.value == "INTERNAL_ERROR"
