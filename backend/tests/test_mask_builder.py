import numpy as np
import pytest
from shapely.geometry import LineString

from app.core.constants import NX, NY
from app.services.edit_engine.mask_builder import (
    MaskError,
    brush_path_to_mask,
    lasso_to_mask,
    line_buffer_to_mask,
    polygon_to_mask,
    smooth_mask,
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


def test_lasso_to_mask_valid_closed_region() -> None:
    coordinates = [
        [80.0, 30.0],
        [80.05, 30.0],
        [80.1, 30.01],
        [80.15, 30.03],
        [80.2, 30.07],
        [80.22, 30.12],
        [80.21, 30.18],
        [80.17, 30.22],
        [80.1, 30.24],
        [80.03, 30.23],
        [79.98, 30.2],
        [79.95, 30.15],
        [79.94, 30.08],
        [79.96, 30.03],
        [80.0, 30.0],
    ]

    mask = lasso_to_mask(coordinates, _valid_mask())

    assert int(np.count_nonzero(mask)) > 0


def test_lasso_to_mask_few_points_after_simplification() -> None:
    with pytest.raises(MaskError) as exc_info:
        lasso_to_mask([[80.0, 30.0], [80.001, 30.001], [80.002, 30.002]], _valid_mask())

    assert exc_info.value.code == "MASK_INVALID_GEOMETRY"


def test_lasso_to_mask_self_intersecting_fixed_by_buffer() -> None:
    mask = lasso_to_mask(
        [
            [80.0, 30.0],
            [80.4, 30.0],
            [80.2, 30.2],
            [80.4, 30.4],
            [80.0, 30.4],
            [80.2, 30.2],
            [80.0, 30.0],
        ],
        _valid_mask(),
    )

    lower_lobe = mask[100:104, 200:209]
    upper_lobe = mask[105:109, 200:209]
    assert int(np.count_nonzero(lower_lobe)) > 0
    assert int(np.count_nonzero(upper_lobe)) > 0


def test_lasso_to_mask_rejects_oversized_coordinates() -> None:
    coords = [[80.0 + i * 0.001, 30.0 + (i % 2) * 0.1] for i in range(10001)]
    with pytest.raises(MaskError) as exc_info:
        lasso_to_mask(coords, _valid_mask())

    assert exc_info.value.code == "MASK_INVALID_GEOMETRY"


def test_lasso_to_mask_large_point_count_simplified() -> None:
    bottom = [[80.0 + index * 0.002, 30.0] for index in range(101)]
    right = [[80.2, 30.0 + index * 0.002] for index in range(1, 101)]
    top = [[80.2 - index * 0.002, 30.2] for index in range(1, 101)]
    left = [[80.0, 30.2 - index * 0.002] for index in range(1, 101)]
    coordinates = bottom + right + top + left + [[80.0, 30.0]]

    simplified = LineString(coordinates).simplify(0.01, preserve_topology=False)
    mask = lasso_to_mask(coordinates, _valid_mask())

    assert len(simplified.coords) < len(coordinates)
    assert int(np.count_nonzero(mask)) > 0


def test_lasso_to_mask_boundary_clamp() -> None:
    mask = lasso_to_mask(
        [[69.9, 24.9], [70.2, 24.9], [70.2, 25.2], [70.0, 25.2], [69.9, 25.1]],
        _valid_mask(),
    )

    assert bool(mask[0, 0]) is True


def test_lasso_to_mask_empty_after_valid_mask() -> None:
    with pytest.raises(MaskError) as exc_info:
        lasso_to_mask(
            [[80.0, 30.0], [80.2, 30.0], [80.2, 30.2], [80.0, 30.2]],
            _valid_mask(False),
        )

    assert exc_info.value.code == "MASK_EMPTY"


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


def test_smooth_mask_sigma_zero_returns_original() -> None:
    mask = np.zeros((8, 8), dtype=bool)
    mask[2:6, 2:6] = True

    result = smooth_mask(mask, 0, np.zeros_like(mask, dtype=bool))

    assert np.array_equal(result, mask)


def test_smooth_mask_valid_sigma_smooths_edges() -> None:
    mask = np.zeros((24, 24), dtype=bool)
    mask[6:18, 6:18] = True

    result = smooth_mask(mask, 1.0, np.ones_like(mask, dtype=bool))

    assert int(np.count_nonzero(result)) > 0
    assert not np.array_equal(result, mask)


def test_smooth_mask_sigma_below_range() -> None:
    mask = np.zeros((8, 8), dtype=bool)
    mask[3:5, 3:5] = True

    with pytest.raises(MaskError) as exc_info:
        smooth_mask(mask, 0.3, np.ones_like(mask, dtype=bool))

    assert exc_info.value.code == "SMOOTH_SIGMA_OUT_OF_RANGE"


def test_smooth_mask_sigma_above_range() -> None:
    mask = np.zeros((8, 8), dtype=bool)
    mask[3:5, 3:5] = True

    with pytest.raises(MaskError) as exc_info:
        smooth_mask(mask, 6.0, np.ones_like(mask, dtype=bool))

    assert exc_info.value.code == "SMOOTH_SIGMA_OUT_OF_RANGE"


def test_smooth_mask_empty_after_smooth() -> None:
    mask = np.zeros((16, 16), dtype=bool)
    mask[8, 8] = True
    valid_mask = np.zeros_like(mask, dtype=bool)
    valid_mask[8, 8] = True

    with pytest.raises(MaskError) as exc_info:
        smooth_mask(mask, 5.0, valid_mask)

    assert exc_info.value.code == "MASK_EMPTY"
