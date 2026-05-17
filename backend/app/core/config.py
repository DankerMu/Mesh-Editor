import json
import os
from pathlib import Path
from typing import Any

from pydantic import BaseModel, ConfigDict

from app.core.constants import REPO_ROOT

DEFAULT_SCHEMAS_DIR = REPO_ROOT / "schemas"


class ConfigLoadError(RuntimeError):
    pass


class GridDefinition(BaseModel):
    projection: str = "EPSG:4326"
    lon_min: float = 70.0
    lon_max: float = 111.0
    lon_step: float = 0.05
    cols: int = 821
    lat_min: float = 25.0
    lat_max: float = 50.0
    lat_step: float = 0.05
    rows: int = 501


class PlotConfig(BaseModel):
    enabled: bool = False
    description: str = "M0 placeholder; implemented in plotting milestones"


class StorageConfig(BaseModel):
    base_dir: Path = REPO_ROOT / "data"


class ProductConfig(BaseModel):
    model_config = ConfigDict(extra="allow")

    init_time_zone: str = "UTC"
    ptype_qpf_threshold_mm: float = 0.1
    allow_zero_start_lead_fallback: bool = True


class Settings(BaseModel):
    model_config = ConfigDict(arbitrary_types_allowed=True)

    product_config: dict[str, Any]
    product: ProductConfig
    grid_definition: GridDefinition
    plot_config: PlotConfig
    storage: StorageConfig
    data_source_root: Path


def _load_json(path: Path) -> dict[str, Any]:
    if not path.exists():
        raise ConfigLoadError(f"必需配置文件不存在: {path}")
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        raise ConfigLoadError(f"配置文件不是合法 JSON: {path}") from exc
    if not isinstance(payload, dict):
        raise ConfigLoadError(f"配置文件顶层必须为对象: {path}")
    return payload


def load_settings(schemas_dir: Path = DEFAULT_SCHEMAS_DIR) -> Settings:
    product_config = _load_json(schemas_dir / "product_config.json")
    data_source_root = Path(os.environ.get("DATA_SOURCE_ROOT", "/data/source"))
    return Settings(
        product_config=product_config,
        product=ProductConfig.model_validate(product_config),
        grid_definition=GridDefinition(),
        plot_config=PlotConfig(),
        storage=StorageConfig(),
        data_source_root=data_source_root,
    )


settings = load_settings()
