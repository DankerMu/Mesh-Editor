import matplotlib.pyplot as plt
import numpy as np
import pytest

from app.plotters.precip_phase_plotter import plot_placeholder, plot_precip_phase
from app.plotters.review_synoptic_plotter import plot_synoptic_basic
from app.services.review_templates import ReviewTemplate, load_template


def _assert_png_file(path: str) -> None:
    with open(path, "rb") as fp:
        header = fp.read(4)
    assert header == b"\x89PNG"


def test_plot_precip_phase_smoke(tmp_path) -> None:
    rng = np.random.default_rng(42)
    qpf = rng.uniform(0, 50, size=(821, 501)).astype(np.float32)
    ptype = rng.integers(0, 4, size=(821, 501), dtype=np.uint8)
    output_path = tmp_path / "precip_phase.png"

    result = plot_precip_phase(
        qpf=qpf,
        ptype=ptype,
        grid_def={"extent": (70.0, 111.0, 25.0, 50.0), "shape": (821, 501)},
        product_type="before",
        metadata={"case_id": "2026051608", "window_label": "24-48h"},
        boundary_config=None,
        output_path=str(output_path),
    )

    assert result == str(output_path)
    assert output_path.exists()
    assert output_path.stat().st_size > 0
    _assert_png_file(str(output_path))


def test_plot_precip_phase_missing_boundary_still_produces_png(tmp_path) -> None:
    qpf = np.ones((20, 30), dtype=np.float32)
    ptype = np.ones((20, 30), dtype=np.uint8)
    output_path = tmp_path / "missing_boundary.png"

    plot_precip_phase(
        qpf=qpf,
        ptype=ptype,
        grid_def={"extent": (70.0, 111.0, 25.0, 50.0), "shape": (20, 30)},
        product_type="after",
        metadata={"window_label": "测试窗口"},
        boundary_config={"province_shapefile": "/nonexistent/path.shp"},
        output_path=str(output_path),
    )

    assert output_path.exists()
    assert output_path.stat().st_size > 0
    _assert_png_file(str(output_path))


def test_plot_precip_phase_dimension_mismatch_raises(tmp_path) -> None:
    with pytest.raises(ValueError, match="dimension mismatch"):
        plot_precip_phase(
            qpf=np.zeros((10, 10), dtype=np.float32),
            ptype=np.zeros((5, 5), dtype=np.uint8),
            grid_def={"extent": (70.0, 111.0, 25.0, 50.0), "shape": (10, 10)},
            product_type="before",
            metadata={},
            boundary_config=None,
            output_path=str(tmp_path / "bad.png"),
        )


def test_plot_precip_phase_qpf_none_raises(tmp_path) -> None:
    with pytest.raises(ValueError, match="qpf must not be None"):
        plot_precip_phase(
            qpf=None,
            ptype=np.zeros((5, 5), dtype=np.uint8),
            grid_def={"extent": (70.0, 111.0, 25.0, 50.0), "shape": (5, 5)},
            product_type="before",
            metadata={},
            boundary_config=None,
            output_path=str(tmp_path / "bad.png"),
        )


def test_plot_placeholder(tmp_path) -> None:
    output_path = tmp_path / "placeholder.png"

    result = plot_placeholder(message="数据缺失：rh 700hPa +072h", output_path=str(output_path))

    assert result == str(output_path)
    assert output_path.exists()
    assert output_path.stat().st_size > 0
    _assert_png_file(str(output_path))


def test_plot_precip_phase_figure_cleanup(tmp_path) -> None:
    before = len(plt.get_fignums())

    plot_precip_phase(
        qpf=np.ones((10, 10), dtype=np.float32),
        ptype=np.ones((10, 10), dtype=np.uint8),
        grid_def={"extent": (70.0, 111.0, 25.0, 50.0), "shape": (10, 10)},
        product_type="before",
        metadata={},
        boundary_config=None,
        output_path=str(tmp_path / "cleanup.png"),
    )

    assert len(plt.get_fignums()) == before


def test_plot_synoptic_basic_smoke(tmp_path) -> None:
    lon = np.linspace(70.0, 111.0, 80, dtype=np.float32)
    lat = np.linspace(25.0, 50.0, 50, dtype=np.float32)
    lon2d, lat2d = np.meshgrid(lon, lat)
    hgt_data = (5600.0 + lon2d * 2.0 + lat2d * 3.0).astype(np.float32)
    wind_u = np.full((50, 80), 8.0, dtype=np.float32)
    wind_v = np.full((50, 80), -2.0, dtype=np.float32)
    output_path = tmp_path / "synoptic.png"

    plot_synoptic_basic(
        hgt_data=hgt_data,
        wind_u=wind_u,
        wind_v=wind_v,
        lon=lon,
        lat=lat,
        level=500,
        init_time="2026051608",
        lead_hour=48,
        boundary_config=None,
        output_path=str(output_path),
    )

    assert output_path.exists()
    assert output_path.stat().st_size > 0
    _assert_png_file(str(output_path))


def test_plot_synoptic_basic_height_only(tmp_path) -> None:
    lon = np.linspace(70.0, 111.0, 20, dtype=np.float32)
    lat = np.linspace(25.0, 50.0, 10, dtype=np.float32)
    hgt_data = np.arange(200, dtype=np.float32).reshape(10, 20)
    output_path = tmp_path / "height_only.png"

    plot_synoptic_basic(
        hgt_data=hgt_data,
        wind_u=None,
        wind_v=None,
        lon=lon,
        lat=lat,
        level=500,
        init_time="2026051608",
        lead_hour=48,
        boundary_config=None,
        output_path=str(output_path),
    )

    assert output_path.exists()
    assert output_path.stat().st_size > 0


def test_plot_synoptic_basic_hgt_none_raises(tmp_path) -> None:
    with pytest.raises(ValueError, match="hgt_data must not be None"):
        plot_synoptic_basic(
            hgt_data=None,
            wind_u=None,
            wind_v=None,
            lon=np.linspace(70.0, 111.0, 20, dtype=np.float32),
            lat=np.linspace(25.0, 50.0, 10, dtype=np.float32),
            level=500,
            init_time="2026051608",
            lead_hour=48,
            boundary_config=None,
            output_path=str(tmp_path / "bad.png"),
        )


def test_plot_synoptic_basic_shape_mismatch_raises(tmp_path) -> None:
    with pytest.raises(ValueError, match="dimension mismatch"):
        plot_synoptic_basic(
            hgt_data=np.zeros((10, 10), dtype=np.float32),
            wind_u=None,
            wind_v=None,
            lon=np.linspace(70.0, 111.0, 20, dtype=np.float32),
            lat=np.linspace(25.0, 50.0, 10, dtype=np.float32),
            level=500,
            init_time="2026051608",
            lead_hour=48,
            boundary_config=None,
            output_path=str(tmp_path / "bad.png"),
        )


def test_plot_synoptic_basic_figure_cleanup(tmp_path) -> None:
    before = len(plt.get_fignums())

    plot_synoptic_basic(
        hgt_data=np.arange(100, dtype=np.float32).reshape(10, 10),
        wind_u=None,
        wind_v=None,
        lon=np.linspace(70.0, 111.0, 10, dtype=np.float32),
        lat=np.linspace(25.0, 50.0, 10, dtype=np.float32),
        level=500,
        init_time="2026051608",
        lead_hour=48,
        boundary_config=None,
        output_path=str(tmp_path / "cleanup_synoptic.png"),
    )

    assert len(plt.get_fignums()) == before


def test_load_template_snow_phase_review_v1() -> None:
    template = load_template("snow_phase_review_v1")

    assert isinstance(template, ReviewTemplate)
    assert template.template_id == "snow_phase_review_v1"
    assert template.template_name == "雨雪相态复盘 V1"
    assert len(template.required_fields) == 6
    assert len(template.panels) == 5
    assert template.allow_partial_success is True


def test_load_template_nonexistent_raises() -> None:
    with pytest.raises(ValueError, match="复盘模板不存在"):
        load_template("nonexistent")


def test_template_panels_map_to_known_plotter_types() -> None:
    known_types = {"precip_phase", "delta_qpf", "change_ptype", "circulation"}
    template = load_template("snow_phase_review_v1")

    assert {panel.type for panel in template.panels} <= known_types
