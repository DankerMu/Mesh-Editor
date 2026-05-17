import json
from pathlib import Path

import pytest

from app.core.config import ConfigLoadError, load_settings, settings


def test_config_loads_product_config_and_grid_definition() -> None:
    assert settings.product_config["max_lead_hours"] == 240
    assert settings.product.init_time_zone == "UTC"
    assert settings.product.ptype_qpf_threshold_mm == 0.1
    assert settings.product.allow_zero_start_lead_fallback is True
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
    assert settings.data_source_root == Path("/data/source")


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
    assert loaded.product.init_time_zone == "UTC"


def test_config_loads_data_source_root_from_env(
    tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> None:
    source_root = tmp_path / "source"
    monkeypatch.setenv("DATA_SOURCE_ROOT", str(source_root))
    (tmp_path / "product_config.json").write_text(
        json.dumps(
            {
                "init_time_zone": "UTC",
                "ptype_qpf_threshold_mm": 0.2,
                "allow_zero_start_lead_fallback": False,
            }
        ),
        encoding="utf-8",
    )

    loaded = load_settings(tmp_path)

    assert loaded.data_source_root == source_root
    assert loaded.product.init_time_zone == "UTC"
    assert loaded.product.ptype_qpf_threshold_mm == 0.2
    assert loaded.product.allow_zero_start_lead_fallback is False
