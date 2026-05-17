from pathlib import Path

import numpy as np

from app.core.config import ProductConfig
from app.engines.qpf_builder import build_qpf_window
from app.storage.path_builder import PathBuilder
from tests.fixtures.conftest import CASE_ID, TEST_SHAPE, write_grid


def test_normal_diff(case_factory, product_config: ProductConfig) -> None:
    case_id, builder = case_factory(leads=[0, 24])

    result = build_qpf_window(case_id, 0, 24, product_config, builder, TEST_SHAPE)

    assert result.qc_status == "pass"
    assert np.allclose(result.qpf, 0.2)
    assert result.negative_count == 0


def test_000_fallback_enabled(tmp_path: Path, product_config: ProductConfig) -> None:
    builder = PathBuilder(
        base_dir=tmp_path / "archive", data_source_root=tmp_path / "source"
    )
    write_grid(builder.tp_file_path(CASE_ID, 24), np.full(TEST_SHAPE, 3.0))

    result = build_qpf_window(CASE_ID, 0, 24, product_config, builder, TEST_SHAPE)

    assert result.qc_status == "pass"
    assert np.allclose(result.qpf, 3.0)
    assert result.manifest["start_lead_fallback"] is True


def test_000_fallback_disabled(tmp_path: Path) -> None:
    config = ProductConfig(allow_zero_start_lead_fallback=False)
    builder = PathBuilder(
        base_dir=tmp_path / "archive", data_source_root=tmp_path / "source"
    )
    write_grid(builder.tp_file_path(CASE_ID, 24), np.full(TEST_SHAPE, 3.0))

    result = build_qpf_window(CASE_ID, 0, 24, config, builder, TEST_SHAPE)

    assert result.qc_status == "fail"
    assert result.manifest["failure_reason"] == "start_tp_missing"


def test_non_zero_start_missing(tmp_path: Path, product_config: ProductConfig) -> None:
    builder = PathBuilder(
        base_dir=tmp_path / "archive", data_source_root=tmp_path / "source"
    )
    write_grid(builder.tp_file_path(CASE_ID, 48), np.full(TEST_SHAPE, 5.0))

    result = build_qpf_window(CASE_ID, 24, 48, product_config, builder, TEST_SHAPE)

    assert result.qc_status == "fail"
    assert result.manifest["failure_reason"] == "start_tp_missing"


def test_end_missing(tmp_path: Path, product_config: ProductConfig) -> None:
    builder = PathBuilder(
        base_dir=tmp_path / "archive", data_source_root=tmp_path / "source"
    )
    write_grid(builder.tp_file_path(CASE_ID, 0), np.zeros(TEST_SHAPE))

    result = build_qpf_window(CASE_ID, 0, 24, product_config, builder, TEST_SHAPE)

    assert result.qc_status == "fail"
    assert result.manifest["failure_reason"] == "end_tp_missing"


def test_negative_detection(tmp_path: Path, product_config: ProductConfig) -> None:
    builder = PathBuilder(
        base_dir=tmp_path / "archive", data_source_root=tmp_path / "source"
    )
    write_grid(builder.tp_file_path(CASE_ID, 0), np.full(TEST_SHAPE, 2.0))
    end = np.full(TEST_SHAPE, 3.0)
    end[2, 3] = 1.25
    write_grid(builder.tp_file_path(CASE_ID, 24), end)

    result = build_qpf_window(CASE_ID, 0, 24, product_config, builder, TEST_SHAPE)

    assert result.qc_status == "warn"
    assert result.negative_count == 1
    assert result.negative_min_value == -0.75
    assert result.negative_abs_max == 0.75
    assert bool(result.negative_mask[2, 3]) is True


def test_nan_propagation(tmp_path: Path, product_config: ProductConfig) -> None:
    builder = PathBuilder(
        base_dir=tmp_path / "archive", data_source_root=tmp_path / "source"
    )
    start = np.zeros(TEST_SHAPE)
    end = np.ones(TEST_SHAPE)
    start[1, 1] = np.nan
    end[2, 2] = np.nan
    write_grid(builder.tp_file_path(CASE_ID, 0), start)
    write_grid(builder.tp_file_path(CASE_ID, 24), end)

    result = build_qpf_window(CASE_ID, 0, 24, product_config, builder, TEST_SHAPE)

    assert result.qc_status == "warn"
    assert result.missing_count == 2
    assert np.isnan(result.qpf[1, 1])
    assert np.isnan(result.qpf[2, 2])


def test_manifest(case_factory, product_config: ProductConfig) -> None:
    case_id, builder = case_factory(leads=[0, 24])

    result = build_qpf_window(case_id, 0, 24, product_config, builder, TEST_SHAPE)

    manifest = result.manifest
    assert manifest["window_id"] == f"{case_id}_ACC24_000_024"
    assert manifest["start_lead"] == 0
    assert manifest["end_lead"] == 24
    assert manifest["start_tp_sha256"]
    assert manifest["end_tp_sha256"]
    assert manifest["qc_status"] == "pass"
    assert isinstance(manifest["built_at"], str)
