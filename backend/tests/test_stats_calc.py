import math

import numpy as np

from app.core.constants import DLAT, DLON, LAT_MIN
from app.services.edit_engine.stats_calc import compute_ptype_transition, compute_stats


def test_compute_stats_full_field_assertion() -> None:
    before = np.array([[1, 2, 3, 4, 5]], dtype=np.float32)
    after = np.array([[10, 10, 10, 10, 10]], dtype=np.float32)
    mask = np.ones(before.shape, dtype=bool)
    lat_indices = np.zeros(before.shape, dtype=np.int64)

    stats = compute_stats(before, after, mask, mask, lat_indices)

    assert stats["before"] == {"min": 1.0, "max": 5.0, "mean": 3.0, "sum": 15.0, "count": 5}
    assert stats["after"] == {"min": 10.0, "max": 10.0, "mean": 10.0, "sum": 50.0, "count": 5}
    assert stats["increase_sum"] == 35.0
    assert stats["decrease_sum"] == 0.0
    assert stats["max_increase"] == 9.0
    assert stats["max_decrease"] == 0.0


def test_compute_stats_increase_and_decrease_sums() -> None:
    before = np.array([[5, 10, 15]], dtype=np.float32)
    after = np.array([[2, 7, 12]], dtype=np.float32)
    mask = np.ones(before.shape, dtype=bool)

    stats = compute_stats(before, after, mask, mask, np.zeros(before.shape, dtype=np.int64))

    assert stats["increase_sum"] == 0.0
    assert stats["decrease_sum"] == 9.0
    assert stats["max_decrease"] == 3.0


def test_compute_stats_empty_mask() -> None:
    field = np.array([[1, 2]], dtype=np.float32)
    stats = compute_stats(
        field,
        field,
        np.zeros(field.shape, dtype=bool),
        np.ones(field.shape, dtype=bool),
        np.zeros(field.shape, dtype=np.int64),
    )

    assert stats["before"]["count"] == 0
    assert stats["after"]["sum"] == 0.0
    assert stats["area_km2"] == 0.0


def test_area_km2_latitude_weighted_calculation() -> None:
    before = np.zeros((2, 2), dtype=np.float32)
    after = np.ones((2, 2), dtype=np.float32)
    mask = np.ones(before.shape, dtype=bool)
    lat_indices = np.array([[200, 200], [201, 201]], dtype=np.int64)

    stats = compute_stats(before, after, mask, mask, lat_indices)
    expected = 0.0
    for lat_index in [200, 200, 201, 201]:
        lat_rad = math.radians(LAT_MIN + lat_index * DLAT)
        expected += (DLON * 111.32 * math.cos(lat_rad)) * (DLAT * 110.574)

    assert math.isclose(float(stats["area_km2"]), expected, rel_tol=1e-12)


def test_compute_ptype_transition_16_keys_and_counts() -> None:
    before = np.array([[0, 1, 1, 2]], dtype=np.uint8)
    after = np.array([[0, 1, 2, 2]], dtype=np.uint8)
    transition = compute_ptype_transition(before, after, np.ones(before.shape, dtype=bool))

    assert len(transition) == 16
    assert transition["0_to_0"] == 1
    assert transition["1_to_1"] == 1
    assert transition["1_to_2"] == 1
    assert transition["2_to_2"] == 1
    assert transition["3_to_3"] == 0


def test_compute_ptype_transition_no_change_scenario() -> None:
    before = np.array([[0, 1, 1, 2]], dtype=np.uint8)
    transition = compute_ptype_transition(before, before, np.ones(before.shape, dtype=bool))

    assert transition["0_to_0"] == 1
    assert transition["1_to_1"] == 2
    assert transition["2_to_2"] == 1
    assert sum(value for key, value in transition.items() if key[:1] != key[-1:]) == 0
