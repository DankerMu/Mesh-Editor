import json
from dataclasses import dataclass
from pathlib import Path

from app.core.constants import REPO_ROOT

DEFAULT_ERROR_CODES_PATH = REPO_ROOT / "schemas" / "error_codes.json"


class ErrorRegistryLoadError(RuntimeError):
    pass


@dataclass(frozen=True)
class ErrorInfo:
    message: str
    http_status: int


ERROR_INFO: dict[str, ErrorInfo] = {
    "AUTH_REQUIRED": ErrorInfo("需要登录认证", 401),
    "TOKEN_EXPIRED": ErrorInfo("登录已过期", 401),
    "PERMISSION_DENIED": ErrorInfo("权限不足", 403),
    "USER_DISABLED": ErrorInfo("用户已被禁用", 403),
    "CASE_NOT_FOUND": ErrorInfo("案例未找到", 404),
    "WINDOW_NOT_FOUND": ErrorInfo("窗口未找到", 404),
    "WINDOW_NOT_EDITABLE": ErrorInfo("window 状态不可编辑", 409),
    "WINDOW_LOCKED": ErrorInfo("window 被其他 session 锁定", 409),
    "GRID_SHAPE_MISMATCH": ErrorInfo("网格 shape 不匹配", 422),
    "FILE_NOT_FOUND": ErrorInfo("文件未找到", 404),
    "DIMENSION_MISMATCH": ErrorInfo("变量维度不一致", 422),
    "QPF_NEGATIVE_WARNING": ErrorInfo("qpf 差分存在负值，按配置决定是否阻止", 409),
    "INVALID_CASE_ID": ErrorInfo(
        "case_id 格式非法（须为 YYYYMMDDHH，HH=08 或 20）", 422
    ),
    "CASE_DIR_NOT_FOUND": ErrorInfo("数据源目录未找到", 404),
    "PTYPE_INVALID_VALUE": ErrorInfo("ptype 文件含非法值（仅允许 0/1/2/3）", 422),
    "SCAN_ALREADY_RUNNING": ErrorInfo("该 case 已有扫描任务运行中", 409),
    "SCAN_NOT_FOUND": ErrorInfo("扫描记录未找到", 404),
    "SESSION_NOT_FOUND": ErrorInfo("会话未找到", 404),
    "SESSION_NOT_EDITING": ErrorInfo("session 不是 editing", 409),
    "SESSION_EXPIRED": ErrorInfo("session 过期", 410),
    "PREVIEW_NOT_FOUND": ErrorInfo("预览未找到", 404),
    "PREVIEW_EXPIRED": ErrorInfo("preview 过期或被覆盖", 409),
    "PREVIEW_CONFLICT": ErrorInfo("preview 已被 apply 或被新 preview 覆盖", 409),
    "PREVIEW_SESSION_MISMATCH": ErrorInfo("preview 不属于 session", 400),
    "MASK_INVALID_GEOMETRY": ErrorInfo("mask 几何参数非法", 422),
    "MASK_OUT_OF_BOUNDS": ErrorInfo("mask 超出网格范围", 422),
    "MASK_EMPTY": ErrorInfo(
        "mask 栅格化后无有效格点（mask 本身为空、全部落在 invalid_mask 上、或经 only_nonzero 过滤后无有效格点）",
        422,
    ),
    "INVALID_OPERATION_PARAM": ErrorInfo("编辑参数非法", 422),
    "INVALID_PTYPE": ErrorInfo("ptype 非 0/1/2/3", 422),
    "NEW_PRECIP_NEEDS_PTYPE": ErrorInfo(
        "新增降水落区未指定 target_ptype（见 docs/14 §14.8.1）", 422
    ),
    "NOTHING_TO_UNDO": ErrorInfo("没有可撤销的编辑操作", 409),
    "NOTHING_TO_REDO": ErrorInfo("没有可重做的编辑操作", 409),
    "FIELD_NOT_AVAILABLE": ErrorInfo(
        "请求的字段数据不可用（preview 过期或尚未生成）", 404
    ),
    "VERSION_NOT_FOUND": ErrorInfo("版本未找到", 404),
    "VERSION_STATUS_CONFLICT": ErrorInfo("当前状态不允许该操作", 409),
    "VERSION_BASE_OUTDATED": ErrorInfo("session 基线不是最新版本", 409),
    "RELEASE_CONFLICT": ErrorInfo("发布冲突", 409),
    "VERSION_SAVE_FAILED": ErrorInfo("版本保存失败", 500),
    "CONSISTENCY_VIOLATION": ErrorInfo(
        "保存时检测到 qpf > threshold 且 ptype == 0 的格点（见 docs/14 §14.8.1）",
        422,
    ),
    "REVIEW_NOT_FOUND": ErrorInfo("复盘产品未找到", 404),
    "TEMPLATE_NOT_FOUND": ErrorInfo("模板未找到", 404),
    "TEMPLATE_VALIDATION_FAILED": ErrorInfo("模板配置非法", 422),
    "PLOT_TASK_NOT_FOUND": ErrorInfo("绘图任务未找到", 404),
    "PLOT_TASK_TIMEOUT": ErrorInfo("绘图超时", 500),
    "PLOT_TASK_FAILED": ErrorInfo("绘图失败", 500),
    "REQUIRED_FIELD_MISSING": ErrorInfo("必需复盘字段缺失", 422),
    "CONFIG_NOT_FOUND": ErrorInfo("配置未找到", 404),
    "CONFIG_VALIDATION_FAILED": ErrorInfo("配置校验失败", 422),
    "FILE_WRITE_FAILED": ErrorInfo("文件写入失败", 500),
    "STORAGE_NOT_ENOUGH": ErrorInfo("存储空间不足", 507),
    "INTERNAL_ERROR": ErrorInfo("服务器内部错误", 500),
    "VALIDATION_ERROR": ErrorInfo("请求参数验证失败", 422),
}


def load_error_codes(path: Path = DEFAULT_ERROR_CODES_PATH) -> set[str]:
    if not path.exists():
        raise ErrorRegistryLoadError(f"错误码定义文件不存在: {path}")
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        raise ErrorRegistryLoadError(f"错误码定义文件不是合法 JSON: {path}") from exc
    if not isinstance(payload, dict):
        raise ErrorRegistryLoadError(f"错误码定义文件顶层必须为对象: {path}")

    codes: set[str] = set()
    for category, values in payload.items():
        if not isinstance(values, list) or not all(
            isinstance(item, str) for item in values
        ):
            raise ErrorRegistryLoadError(f"错误码分类 {category} 必须是字符串数组")
        codes.update(values)

    missing_mapping = sorted(codes - ERROR_INFO.keys())
    if missing_mapping:
        raise ErrorRegistryLoadError(
            f"错误码缺少中文文案或 HTTP 状态: {', '.join(missing_mapping)}"
        )
    return codes


ERROR_CODES = load_error_codes()


def get_error(code: str) -> tuple[str, int]:
    info = ERROR_INFO.get(code)
    if info is None:
        info = ERROR_INFO["INTERNAL_ERROR"]
    return info.message, info.http_status
