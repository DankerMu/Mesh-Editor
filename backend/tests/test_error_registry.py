from app.core.error_registry import ERROR_CODES, get_error
from app.schemas.error_codes import ErrorCode


def test_error_registry_loads_auth_required_mapping() -> None:
    message, http_status = get_error("AUTH_REQUIRED")

    assert "AUTH_REQUIRED" in ERROR_CODES
    assert message == "需要登录认证"
    assert http_status == 401


def test_error_registry_loads_m1_error_codes() -> None:
    expected = {
        "INVALID_CASE_ID",
        "CASE_DIR_NOT_FOUND",
        "PTYPE_INVALID_VALUE",
        "SCAN_ALREADY_RUNNING",
        "SCAN_NOT_FOUND",
    }

    assert expected.issubset(ERROR_CODES)
    assert get_error("INVALID_CASE_ID") == (
        "case_id 格式非法（须为 YYYYMMDDHH，HH=08 或 20）",
        422,
    )
    assert get_error("SCAN_ALREADY_RUNNING") == ("该 case 已有扫描任务运行中", 409)


def test_error_code_enum_generated_from_schema() -> None:
    assert ErrorCode.AUTH_REQUIRED.value == "AUTH_REQUIRED"
    assert ErrorCode.INTERNAL_ERROR.value == "INTERNAL_ERROR"
