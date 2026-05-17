import numpy as np
import pytest

from app.services.edit_engine.edit_ops import (
    EditContext,
    EditOpError,
    apply_ptype_set,
    apply_qpf_clear,
    apply_qpf_decrease,
    apply_qpf_increase,
    apply_qpf_multiply,
    apply_qpf_set_value,
    apply_screen_clear,
    enforce_qpf_ptype_consistency,
)


def _ctx(
    qpf: list[list[float]] | None = None,
    ptype: list[list[int]] | None = None,
    operation_mask: np.ndarray | None = None,
    valid_mask: np.ndarray | None = None,
) -> EditContext:
    qpf_array = np.array(qpf or [[0, 2, 5], [1, 0.05, 10]], dtype=np.float32)
    ptype_array = np.array(ptype or [[0, 1, 2], [1, 3, 2]], dtype=np.uint8)
    mask = (
        operation_mask
        if operation_mask is not None
        else np.ones(qpf_array.shape, dtype=bool)
    )
    valid = valid_mask if valid_mask is not None else np.ones(qpf_array.shape, dtype=bool)
    return EditContext(qpf_array, ptype_array, mask, valid)


def test_set_value_normal() -> None:
    result = apply_qpf_set_value(_ctx(), 5)

    assert np.all(result.qpf_after == 5)
    assert result.affected_grid_count == 6
    assert result.new_precip_needs_ptype is True
    assert result.new_precip_count == 1


def test_set_value_zero_clears_ptype() -> None:
    result = apply_qpf_set_value(_ctx(), 0)

    assert np.all(result.qpf_after == 0)
    assert np.all(result.ptype_after == 0)


def test_set_value_rejects_negative_value() -> None:
    with pytest.raises(EditOpError) as exc_info:
        apply_qpf_set_value(_ctx(), -1)

    assert exc_info.value.code == "INVALID_OPERATION_PARAM"


def test_increase_normal() -> None:
    result = apply_qpf_increase(_ctx(qpf=[[0, 2, 5]]), 3)

    assert np.allclose(result.qpf_after, [[3, 5, 8]])


def test_increase_only_nonzero() -> None:
    result = apply_qpf_increase(_ctx(qpf=[[0, 0.1, 2]], ptype=[[0, 0, 1]]), 2, True)

    assert np.allclose(result.qpf_after, [[0, 0.1, 4]])
    assert result.affected_grid_count == 1


def test_decrease_clamps_to_zero_and_clears_ptype() -> None:
    result = apply_qpf_decrease(_ctx(qpf=[[1, 5, 10]], ptype=[[1, 2, 3]]), 3)

    assert np.allclose(result.qpf_after, [[0, 2, 7]])
    assert result.ptype_after[0, 0] == 0


def test_multiply_normal_and_zero_clear() -> None:
    result = apply_qpf_multiply(_ctx(qpf=[[2, 4, 6]], ptype=[[1, 2, 3]]), 1.5)
    cleared = apply_qpf_multiply(_ctx(qpf=[[2, 4, 6]], ptype=[[1, 2, 3]]), 0)

    assert np.allclose(result.qpf_after, [[3, 6, 9]])
    assert np.all(cleared.qpf_after == 0)
    assert np.all(cleared.ptype_after == 0)


def test_multiply_rejects_negative_factor() -> None:
    with pytest.raises(EditOpError) as exc_info:
        apply_qpf_multiply(_ctx(), -1)

    assert exc_info.value.code == "INVALID_OPERATION_PARAM"


def test_clear_sets_qpf_and_ptype_to_zero() -> None:
    result = apply_qpf_clear(_ctx())

    assert np.all(result.qpf_after == 0)
    assert np.all(result.ptype_after == 0)


def test_ptype_set_only_where_qpf_exceeds_threshold() -> None:
    result = apply_ptype_set(_ctx(qpf=[[0, 5, 10]], ptype=[[0, 1, 1]]), 2)

    assert result.ptype_after.tolist() == [[0, 2, 2]]


def test_ptype_set_validation() -> None:
    with pytest.raises(EditOpError) as exc_info:
        apply_ptype_set(_ctx(), 9)

    assert exc_info.value.code == "INVALID_OPERATION_PARAM"


def test_screen_clear_clears_weak_precip_only() -> None:
    result = apply_screen_clear(
        _ctx(qpf=[[0.1, 0.3, 2, 5]], ptype=[[1, 1, 2, 3]]), threshold=0.5
    )

    assert np.allclose(result.qpf_after, [[0, 0, 2, 5]])
    assert result.ptype_after.tolist() == [[0, 0, 2, 3]]


def test_consistency_corrects_invalid_ptype_and_warns() -> None:
    qpf = np.array([[0, 5]], dtype=np.float32)
    ptype = np.array([[1, 5]], dtype=np.uint8)
    ptype_after, warnings = enforce_qpf_ptype_consistency(
        qpf, ptype, 0.1, np.ones(qpf.shape, dtype=bool)
    )

    assert ptype_after.tolist() == [[0, 0]]
    assert {"code": "PTYPE_AUTO_CLEARED", "count": 1} in warnings
    assert {"code": "PTYPE_INVALID_CORRECTED", "count": 1} in warnings


def test_new_precip_detection() -> None:
    result = apply_qpf_set_value(_ctx(qpf=[[0, 1]], ptype=[[0, 1]]), 5)

    assert result.new_precip_needs_ptype is True
    assert result.new_precip_count == 1


def test_invalid_mask_exclusion_keeps_points_unchanged() -> None:
    valid_mask = np.array([[True, False]], dtype=bool)
    result = apply_qpf_set_value(
        _ctx(qpf=[[1, 1]], ptype=[[1, 1]], valid_mask=valid_mask), 5
    )

    assert result.qpf_after.tolist() == [[5, 1]]
    assert result.ptype_after.tolist() == [[1, 1]]
    assert result.affected_grid_count == 1
