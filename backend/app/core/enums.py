import json
from enum import StrEnum
from pathlib import Path
from typing import Any

from app.core.constants import REPO_ROOT

DEFAULT_ENUMS_PATH = REPO_ROOT / "schemas" / "enums.json"


class EnumLoadError(RuntimeError):
    pass


def _class_name(name: str) -> str:
    return "".join(part.capitalize() for part in name.split("_"))


def _member_name(value: Any) -> str:
    text = str(value)
    if text.isidentifier() and not text[0].isdigit():
        return text.upper()
    return f"VALUE_{text}".replace("-", "_").replace(".", "_").upper()


def load_enums(path: Path = DEFAULT_ENUMS_PATH) -> dict[str, type[StrEnum]]:
    if not path.exists():
        raise EnumLoadError(f"枚举定义文件不存在: {path}")
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        raise EnumLoadError(f"枚举定义文件不是合法 JSON: {path}") from exc
    if not isinstance(payload, dict):
        raise EnumLoadError(f"枚举定义文件顶层必须为对象: {path}")

    enum_classes: dict[str, type[StrEnum]] = {}
    for enum_name, values in payload.items():
        if not isinstance(values, list) or not values:
            raise EnumLoadError(f"枚举 {enum_name} 必须是非空数组")
        members = {_member_name(value): str(value) for value in values}
        enum_classes[_class_name(enum_name)] = StrEnum(_class_name(enum_name), members)
    return enum_classes


_generated = load_enums()
globals().update(_generated)

__all__ = ["load_enums", *_generated.keys()]
