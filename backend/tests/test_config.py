import json
from pathlib import Path

import pytest

from app.core.config import ConfigLoadError, load_settings, settings


def test_config_loads_product_config_and_grid_definition() -> None:
    assert settings.product_config["max_lead_hours"] == 240
    assert settings.grid_definition.projection == "EPSG:4326"
    assert settings.grid_definition.rows == 501
    assert settings.grid_definition.cols == 821
    assert settings.grid_definition.lon_min == 70.0
    assert settings.grid_definition.lon_max == 111.0
    assert settings.grid_definition.lon_step == 0.05
    assert settings.grid_definition.lat_min == 25.0
    assert settings.grid_definition.lat_max == 50.0
    assert settings.grid_definition.lat_step == 0.05
    assert settings.plot_config.enabled is False


def test_config_missing_required_file_fails_clearly(tmp_path: Path) -> None:
    with pytest.raises(ConfigLoadError, match="product_config.json"):
        load_settings(tmp_path)


def test_config_loads_from_custom_schema_dir(tmp_path: Path) -> None:
    (tmp_path / "product_config.json").write_text(
        json.dumps({"max_lead_hours": 240}),
        encoding="utf-8",
    )

    loaded = load_settings(tmp_path)

    assert loaded.product_config == {"max_lead_hours": 240}
