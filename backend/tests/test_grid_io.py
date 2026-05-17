from pathlib import Path

import numpy as np
import pytest

from app.core.errors import DomainError
from app.engines.grid_io import read_txt_grid
from tests.fixtures.conftest import TEST_SHAPE, write_grid


def test_read_normal(tmp_path: Path) -> None:
    path = tmp_path / "tp_003.txt"
    write_grid(path, np.full(TEST_SHAPE, 1.5))

    result = read_txt_grid(path, expected_shape=TEST_SHAPE)

    assert result.shape == TEST_SHAPE
    assert result.dtype == "float"
    assert result.nan_count == 0
    assert result.invalid_count == 0
    assert np.allclose(result.array, 1.5)


def test_wrong_shape(tmp_path: Path) -> None:
    path = tmp_path / "wrong.txt"
    write_grid(path, np.ones((2, 3)))

    with pytest.raises(DomainError) as exc_info:
        read_txt_grid(path, expected_shape=TEST_SHAPE)

    assert exc_info.value.code == "GRID_SHAPE_MISMATCH"


def test_file_not_found(tmp_path: Path) -> None:
    with pytest.raises(DomainError) as exc_info:
        read_txt_grid(tmp_path / "missing.txt", expected_shape=TEST_SHAPE)

    assert exc_info.value.code == "FILE_NOT_FOUND"


def test_nan_values(tmp_path: Path) -> None:
    path = tmp_path / "nan.txt"
    data = np.ones(TEST_SHAPE)
    data[1, 2] = np.nan
    write_grid(path, data)

    result = read_txt_grid(path, expected_shape=TEST_SHAPE)

    assert result.nan_count == 1
    assert bool(result.missing_mask[1, 2]) is True
    assert np.isnan(result.array[1, 2])


def test_invalid_ptype(tmp_path: Path) -> None:
    path = tmp_path / "ptype_003.txt"
    data = np.ones(TEST_SHAPE)
    data[0, 0] = 9
    data[0, 1] = 1.5
    write_grid(path, data)

    result = read_txt_grid(path, dtype="int", expected_shape=TEST_SHAPE)

    assert result.invalid_count == 2
    assert bool(result.invalid_mask[0, 0]) is True
    assert bool(result.invalid_mask[0, 1]) is True
