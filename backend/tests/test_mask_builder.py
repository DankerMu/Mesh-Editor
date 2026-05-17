import numpy as np
import pytest

from app.core.constants import NX, NY
from app.services.edit_engine.mask_builder import (
    MaskError,
    brush_path_to_mask,
    line_buffer_to_mask,
    polygon_to_mask,
)


def _valid_mask(value: bool = True) -> np.ndarray:
    return np.full((NY, NX), value, dtype=bool)


def test_polygon_to_mask_valid_and_boundary_included() -> None:
    mask = polygon_to_mask([[80, 30], [80.1, 30], [80.1, 30.1], [80, 30.1]], _valid_mask())

    assert bool(mask[100, 200]) is True
    assert bool(mask[102, 202]) is True


def test_polygon_to_mask_clamps_out_of_bounds() -> None:
    mask = polygon_to_mask([[69, 24], [70.1, 25], [70, 25.1]], _valid_mask())

    assert bool(mask[0, 0]) is True


def test_polygon_to_mask_requires_three_vertices() -> None:
    with pytest.raises(MaskError) as exc_info:
        polygon_to_mask([[80, 30], [81, 31]], _valid_mask())

    assert exc_info.value.code == "MASK_INVALID_GEOMETRY"


def test_polygon_to_mask_empty_after_valid_mask_intersection() -> None:
    with pytest.raises(MaskError) as exc_info:
        polygon_to_mask([[80, 30], [80.1, 30], [80, 30.1]], _valid_mask(False))

    assert exc_info.value.code == "MASK_EMPTY"


def test_line_buffer_to_mask_valid_line() -> None:
    mask = line_buffer_to_mask([[80, 30], [80.2, 30]], 1, _valid_mask())

    assert bool(mask[100, 200]) is True
    assert bool(mask[100, 204]) is True


def test_line_buffer_to_mask_requires_two_points() -> None:
    with pytest.raises(MaskError) as exc_info:
        line_buffer_to_mask([[80, 30]], 1, _valid_mask())

    assert exc_info.value.code == "MASK_INVALID_GEOMETRY"


def test_line_buffer_to_mask_requires_positive_width() -> None:
    with pytest.raises(MaskError) as exc_info:
        line_buffer_to_mask([[80, 30], [81, 30]], 0, _valid_mask())

    assert exc_info.value.code == "MASK_INVALID_GEOMETRY"


def test_brush_path_to_mask_valid_brush() -> None:
    mask = brush_path_to_mask([[80, 30]], 2, _valid_mask())

    assert bool(mask[100, 200]) is True
    assert bool(mask[100, 202]) is True
    assert bool(mask[103, 200]) is False


def test_brush_path_to_mask_requires_points() -> None:
    with pytest.raises(MaskError) as exc_info:
        brush_path_to_mask([], 2, _valid_mask())

    assert exc_info.value.code == "MASK_INVALID_GEOMETRY"


def test_brush_path_to_mask_requires_positive_radius() -> None:
    with pytest.raises(MaskError) as exc_info:
        brush_path_to_mask([[80, 30]], 0, _valid_mask())

    assert exc_info.value.code == "MASK_INVALID_GEOMETRY"


def test_mask_valid_mask_intersection_works_for_all_types() -> None:
    valid_mask = _valid_mask(False)
    valid_mask[100, 200] = True

    polygon = polygon_to_mask([[80, 30], [80.1, 30], [80, 30.1]], valid_mask)
    line = line_buffer_to_mask([[80, 30], [80.2, 30]], 1, valid_mask)
    brush = brush_path_to_mask([[80, 30]], 2, valid_mask)

    assert int(np.count_nonzero(polygon)) == 1
    assert int(np.count_nonzero(line)) == 1
    assert int(np.count_nonzero(brush)) == 1


def test_mask_empty_when_all_invalid_for_all_types() -> None:
    for fn, args in [
        (polygon_to_mask, ([[80, 30], [80.1, 30], [80, 30.1]], _valid_mask(False))),
        (line_buffer_to_mask, ([[80, 30], [80.2, 30]], 1, _valid_mask(False))),
        (brush_path_to_mask, ([[80, 30]], 2, _valid_mask(False))),
    ]:
        with pytest.raises(MaskError) as exc_info:
            fn(*args)
        assert exc_info.value.code == "MASK_EMPTY"
