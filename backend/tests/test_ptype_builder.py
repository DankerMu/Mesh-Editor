from pathlib import Path

import numpy as np
import pytest

from app.core.config import ProductConfig
from app.core.errors import DomainError
from app.engines.ptype_builder import build_ptype_window, enumerate_ptype_leads
from app.storage.path_builder import PathBuilder
from tests.fixtures.conftest import CASE_ID, TEST_SHAPE, write_grid


def _builder(tmp_path: Path) -> PathBuilder:
    return PathBuilder(
        base_dir=tmp_path / "archive", data_source_root=tmp_path / "source"
    )


def _write_tp_sequence(builder: PathBuilder, values: dict[int, np.ndarray]) -> None:
    for lead, array in values.items():
        write_grid(builder.tp_file_path(CASE_ID, lead), array)


def _write_ptype(builder: PathBuilder, lead: int, value: int | np.ndarray) -> None:
    array = value if isinstance(value, np.ndarray) else np.full(TEST_SHAPE, value)
    write_grid(builder.ptype_file_path(CASE_ID, lead), array.astype(float))


def test_all_rain(case_factory, product_config: ProductConfig) -> None:
    case_id, builder = case_factory(leads=[0, 3, 6], ptype_value_by_lead={3: 1, 6: 1})

    result = build_ptype_window(case_id, 0, 6, product_config, builder, TEST_SHAPE)

    assert result.qc_status == "pass"
    assert result.effective_lead_count == 2
    assert np.all(result.ptype == 1)


def test_all_snow(case_factory, product_config: ProductConfig) -> None:
    case_id, builder = case_factory(leads=[0, 3, 6], ptype_value_by_lead={3: 2, 6: 2})

    result = build_ptype_window(case_id, 0, 6, product_config, builder, TEST_SHAPE)

    assert result.qc_status == "pass"
    assert np.all(result.ptype == 2)


def test_mixed(case_factory, product_config: ProductConfig) -> None:
    case_id, builder = case_factory(leads=[0, 3, 6], ptype_value_by_lead={3: 1, 6: 2})

    result = build_ptype_window(case_id, 0, 6, product_config, builder, TEST_SHAPE)

    assert result.qc_status == "pass"
    assert np.all(result.ptype == 3)


def test_no_precip(case_factory, product_config: ProductConfig) -> None:
    case_id, builder = case_factory(leads=[0, 3, 6], qpf_step=0.05)

    result = build_ptype_window(case_id, 0, 6, product_config, builder, TEST_SHAPE)

    assert result.qc_status == "pass"
    assert np.all(result.ptype == 0)


def test_boundary_threshold(case_factory, product_config: ProductConfig) -> None:
    case_id, builder = case_factory(leads=[0, 3], qpf_step=0.1)

    result = build_ptype_window(case_id, 0, 3, product_config, builder, TEST_SHAPE)

    assert np.all(result.ptype == 0)


def test_partial_missing(case_factory, product_config: ProductConfig) -> None:
    case_id, builder = case_factory(leads=[0, 3, 6], ptype_missing_leads={6})

    result = build_ptype_window(case_id, 0, 6, product_config, builder, TEST_SHAPE)

    assert result.qc_status == "warn"
    assert result.ptype_missing_leads == [6]
    assert result.effective_lead_count == 1


def test_all_missing(case_factory, product_config: ProductConfig) -> None:
    case_id, builder = case_factory(leads=[0, 3, 6], ptype_missing_leads={3, 6})

    result = build_ptype_window(case_id, 0, 6, product_config, builder, TEST_SHAPE)

    assert result.qc_status == "fail"
    assert result.effective_lead_count == 0
    assert result.ptype_missing_leads == [3, 6]


def test_tp_missing_lead(tmp_path: Path, product_config: ProductConfig) -> None:
    builder = _builder(tmp_path)
    write_grid(builder.tp_file_path(CASE_ID, 0), np.zeros(TEST_SHAPE))
    write_grid(builder.tp_file_path(CASE_ID, 6), np.full(TEST_SHAPE, 0.4))
    _write_ptype(builder, 3, 1)
    _write_ptype(builder, 6, 1)

    result = build_ptype_window(CASE_ID, 0, 6, product_config, builder, TEST_SHAPE)

    assert result.qc_status == "fail"
    assert result.tp_missing_leads == [3, 6]
    assert result.effective_lead_count == 0


def test_nan_exclusion(tmp_path: Path, product_config: ProductConfig) -> None:
    builder = _builder(tmp_path)
    tp0 = np.zeros(TEST_SHAPE)
    tp3 = np.full(TEST_SHAPE, 0.2)
    tp3[0, 0] = np.nan
    _write_tp_sequence(builder, {0: tp0, 3: tp3})
    _write_ptype(builder, 3, 1)

    result = build_ptype_window(CASE_ID, 0, 3, product_config, builder, TEST_SHAPE)

    assert result.qc_status == "warn"
    assert result.step_nan_count == 1
    assert result.ptype[0, 0] == 0
    assert result.ptype[0, 1] == 1


def test_negative_step_exclusion(tmp_path: Path, product_config: ProductConfig) -> None:
    builder = _builder(tmp_path)
    tp0 = np.full(TEST_SHAPE, 0.5)
    tp3 = np.full(TEST_SHAPE, 0.8)
    tp3[1, 1] = 0.4
    _write_tp_sequence(builder, {0: tp0, 3: tp3})
    _write_ptype(builder, 3, 1)

    result = build_ptype_window(CASE_ID, 0, 3, product_config, builder, TEST_SHAPE)

    assert result.qc_status == "warn"
    assert result.step_negative_count == 1
    assert result.ptype[1, 1] == 0
    assert result.ptype[1, 2] == 1


def test_lead_enumeration(product_config: ProductConfig) -> None:
    assert enumerate_ptype_leads(24, 48, product_config) == [
        27,
        30,
        33,
        36,
        39,
        42,
        45,
        48,
    ]
    assert enumerate_ptype_leads(0, 24, product_config) == [3, 6, 9, 12, 15, 18, 21, 24]

    with pytest.raises(DomainError) as exc_info:
        enumerate_ptype_leads(3, 24, product_config)
    assert exc_info.value.code == "VALIDATION_ERROR"


def test_invalid_ptype_excluded(tmp_path: Path, product_config: ProductConfig) -> None:
    builder = _builder(tmp_path)
    _write_tp_sequence(
        builder,
        {
            0: np.zeros(TEST_SHAPE),
            3: np.full(TEST_SHAPE, 0.2),
            6: np.full(TEST_SHAPE, 0.4),
        },
    )
    ptype3 = np.ones(TEST_SHAPE)
    ptype3[0, 0] = 9
    _write_ptype(builder, 3, ptype3)
    _write_ptype(builder, 6, 2)

    result = build_ptype_window(CASE_ID, 0, 6, product_config, builder, TEST_SHAPE)

    assert result.qc_status == "warn"
    assert result.manifest["invalid_ptype_count"] == 1
    assert result.ptype[0, 0] == 2
    assert result.ptype[0, 1] == 3
