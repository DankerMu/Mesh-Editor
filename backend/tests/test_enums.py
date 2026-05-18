from enum import StrEnum

from app.core.enums import Role, load_enums


def test_role_enum_contains_expected_values() -> None:
    assert issubclass(Role, StrEnum)
    assert Role.ADMIN.value == "admin"
    assert Role.REVIEWER.value == "reviewer"
    assert Role.FORECASTER.value == "forecaster"
    assert Role.VIEWER.value == "viewer"


def test_load_enums_returns_dynamic_classes() -> None:
    enums = load_enums()

    assert "Role" in enums
    assert enums["Role"].ADMIN.value == "admin"
    assert enums["EditTool"].LASSO.value == "lasso"
    assert enums["MaskType"].LASSO.value == "lasso"
