import json
from enum import StrEnum
from pathlib import Path

from app.core.constants import REPO_ROOT

DEFAULT_ERROR_CODES_PATH = REPO_ROOT / "schemas" / "error_codes.json"


class ErrorCodeLoadError(RuntimeError):
    pass


def _load_members(path: Path) -> dict[str, str]:
    if not path.exists():
        raise ErrorCodeLoadError(f"错误码定义文件不存在: {path}")
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        raise ErrorCodeLoadError(f"错误码定义文件不是合法 JSON: {path}") from exc
    if not isinstance(payload, dict):
        raise ErrorCodeLoadError(f"错误码定义文件顶层必须为对象: {path}")

    members: dict[str, str] = {}
    for category, codes in payload.items():
        if not isinstance(codes, list) or not all(isinstance(code, str) for code in codes):
            raise ErrorCodeLoadError(f"错误码分类 {category} 必须是字符串数组")
        for code in codes:
            members[code] = code
    return members


def load_error_code_enum(path: Path = DEFAULT_ERROR_CODES_PATH) -> type[StrEnum]:
    return StrEnum("ErrorCode", _load_members(path))


ErrorCode = load_error_code_enum()

__all__ = ["ErrorCode", "load_error_code_enum"]
