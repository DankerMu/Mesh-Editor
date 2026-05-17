import numpy as np

from app.engines.version_fields import (
    compute_change_ptype,
    compute_changed_mask,
    compute_delta_qpf,
    compute_touched_mask,
)


def test_compute_delta_qpf_basic_subtraction_shape_and_dtype() -> None:
    before = np.array([[1.0, 3.5, 0.0], [2.0, 5.0, 10.0]], dtype=np.float32)
    after = np.array([[2.5, 1.5, 0.0], [2.0, 7.0, 4.0]], dtype=np.float32)

    delta = compute_delta_qpf(before, after)

    assert delta.shape == before.shape
    assert delta.dtype == np.float32
    assert np.allclose(delta, [[1.5, -2.0, 0.0], [0.0, 2.0, -6.0]])


def test_compute_delta_qpf_all_zeros_when_no_change() -> None:
    before = np.array([[1.0, 2.0], [3.0, 4.0]], dtype=np.float32)

    delta = compute_delta_qpf(before, before.copy())

    assert delta.dtype == np.float32
    assert np.all(delta == 0)


def test_compute_change_ptype_basic_difference_dtype_and_negative() -> None:
    before = np.array([[0, 1, 3], [2, 3, 1]], dtype=np.uint8)
    after = np.array([[1, 1, 1], [3, 0, 2]], dtype=np.uint8)

    change = compute_change_ptype(before, after)

    assert change.dtype == np.int8
    assert change.tolist() == [[1, 0, -2], [1, -3, 1]]


def test_compute_change_ptype_all_zeros_when_same() -> None:
    before = np.array([[0, 1], [2, 3]], dtype=np.uint8)

    change = compute_change_ptype(before, before.copy())

    assert change.dtype == np.int8
    assert np.all(change == 0)


def test_compute_touched_mask_passthrough_returns_same_array() -> None:
    touched = np.array([[0, 1, 0], [1, 1, 0]], dtype=np.uint8)

    result = compute_touched_mask(touched)

    assert result is touched
    assert result.dtype == np.uint8


def test_compute_changed_mask_only_qpf_changed_above_epsilon() -> None:
    qpf_before = np.zeros((3, 3), dtype=np.float32)
    qpf_after = qpf_before.copy()
    qpf_after[1, 1] = 0.01
    ptype_before = np.zeros((3, 3), dtype=np.uint8)
    ptype_after = ptype_before.copy()

    changed = compute_changed_mask(qpf_before, qpf_after, ptype_before, ptype_after)

    expected = np.zeros((3, 3), dtype=np.uint8)
    expected[1, 1] = 1
    assert np.array_equal(changed, expected)


def test_compute_changed_mask_only_ptype_changed() -> None:
    qpf_before = np.ones((3, 3), dtype=np.float32)
    qpf_after = qpf_before.copy()
    ptype_before = np.zeros((3, 3), dtype=np.uint8)
    ptype_after = ptype_before.copy()
    ptype_after[0, 2] = 3

    changed = compute_changed_mask(qpf_before, qpf_after, ptype_before, ptype_after)

    expected = np.zeros((3, 3), dtype=np.uint8)
    expected[0, 2] = 1
    assert np.array_equal(changed, expected)


def test_compute_changed_mask_below_epsilon_not_changed() -> None:
    qpf_before = np.ones((3, 3), dtype=np.float32)
    qpf_after = qpf_before.copy()
    qpf_after[2, 2] += 0.0005
    ptype_before = np.zeros((3, 3), dtype=np.uint8)
    ptype_after = ptype_before.copy()

    changed = compute_changed_mask(qpf_before, qpf_after, ptype_before, ptype_after)

    assert np.all(changed == 0)


def test_compute_changed_mask_touched_but_net_zero_not_changed() -> None:
    qpf_before = np.array([[1.0, 5.0], [0.0, 2.0]], dtype=np.float32)
    qpf_after = qpf_before.copy()
    ptype_before = np.array([[0, 1], [0, 2]], dtype=np.uint8)
    ptype_after = ptype_before.copy()

    changed = compute_changed_mask(qpf_before, qpf_after, ptype_before, ptype_after)

    assert np.all(changed == 0)


def test_compute_changed_mask_all_changed() -> None:
    qpf_before = np.zeros((3, 3), dtype=np.float32)
    qpf_after = np.ones((3, 3), dtype=np.float32)
    ptype_before = np.zeros((3, 3), dtype=np.uint8)
    ptype_after = np.ones((3, 3), dtype=np.uint8)

    changed = compute_changed_mask(qpf_before, qpf_after, ptype_before, ptype_after)

    assert changed.dtype == np.uint8
    assert np.all(changed == 1)


def test_compute_changed_mask_no_changes_at_all() -> None:
    qpf_before = np.arange(9, dtype=np.float32).reshape(3, 3)
    ptype_before = np.arange(9, dtype=np.uint8).reshape(3, 3) % 4

    changed = compute_changed_mask(
        qpf_before,
        qpf_before.copy(),
        ptype_before,
        ptype_before.copy(),
    )

    assert changed.dtype == np.uint8
    assert np.all(changed == 0)
