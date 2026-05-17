import numpy as np

from app.services.edit_engine.edit_ops import (
    EditContext,
    apply_qpf_decrease,
    apply_qpf_increase,
    apply_qpf_set_value,
)
from app.services.edit_engine.replay import compute_can_undo_redo, replay_operations


def _base() -> tuple[np.ndarray, np.ndarray, np.ndarray, np.ndarray, np.ndarray]:
    qpf = np.array([[0, 2, 5]], dtype=np.float32)
    ptype = np.array([[0, 1, 2]], dtype=np.uint8)
    valid = np.ones(qpf.shape, dtype=bool)
    left = np.array([[True, True, False]], dtype=bool)
    right = np.array([[False, True, True]], dtype=bool)
    return qpf, ptype, valid, left, right


def test_replay_operations_sequential_matches_individual_applies() -> None:
    qpf, ptype, valid, left, right = _base()
    operations = [
        {"sequence_no": 1, "operation_type": "increase", "parameters_json": {"delta_mm": 3}, "operation_mask": left},
        {"sequence_no": 2, "operation_type": "set_value", "parameters_json": {"value": 10}, "operation_mask": right},
        {"sequence_no": 3, "operation_type": "decrease", "parameters_json": {"delta_mm": 2}, "operation_mask": right},
    ]

    replay_qpf, replay_ptype, _ = replay_operations(operations, qpf, ptype, valid)
    first = apply_qpf_increase(EditContext(qpf, ptype, left, valid), 3)
    second = apply_qpf_set_value(EditContext(first.qpf_after, first.ptype_after, right, valid), 10)
    third = apply_qpf_decrease(EditContext(second.qpf_after, second.ptype_after, right, valid), 2)

    assert np.array_equal(replay_qpf, third.qpf_after)
    assert np.array_equal(replay_ptype, third.ptype_after)


def test_undo_marks_undone_and_replays_remaining() -> None:
    qpf, ptype, valid, left, right = _base()
    operations = [
        {"sequence_no": 1, "operation_type": "increase", "parameters_json": {"delta_mm": 3}, "operation_mask": left, "is_undone": 0},
        {"sequence_no": 2, "operation_type": "set_value", "parameters_json": {"value": 10}, "operation_mask": right, "is_undone": 1},
    ]

    replay_qpf, _, _ = replay_operations(operations, qpf, ptype, valid)

    assert np.allclose(replay_qpf, [[3, 5, 5]])


def test_redo_restores_undone_operation() -> None:
    qpf, ptype, valid, left, right = _base()
    operations = [
        {"sequence_no": 1, "operation_type": "increase", "parameters_json": {"delta_mm": 3}, "operation_mask": left, "is_undone": 0},
        {"sequence_no": 2, "operation_type": "set_value", "parameters_json": {"value": 10}, "operation_mask": right, "is_undone": 0},
    ]

    replay_qpf, _, _ = replay_operations(operations, qpf, ptype, valid)

    assert np.allclose(replay_qpf, [[3, 10, 10]])


def test_new_operation_discards_redo_stack_by_input_operations() -> None:
    qpf, ptype, valid, left, right = _base()
    operations_after_discard = [
        {"sequence_no": 1, "operation_type": "increase", "parameters_json": {"delta_mm": 3}, "operation_mask": left, "is_undone": 0},
        {"sequence_no": 3, "operation_type": "decrease", "parameters_json": {"delta_mm": 1}, "operation_mask": right, "is_undone": 0},
    ]

    replay_qpf, _, _ = replay_operations(operations_after_discard, qpf, ptype, valid)

    assert np.allclose(replay_qpf, [[3, 4, 4]])


def test_touched_mask_not_rolled_back_by_undo() -> None:
    qpf, ptype, valid, left, right = _base()
    operations = [
        {"sequence_no": 1, "operation_type": "increase", "parameters_json": {"delta_mm": 3}, "operation_mask": left, "is_undone": 0},
        {"sequence_no": 2, "operation_type": "set_value", "parameters_json": {"value": 10}, "operation_mask": right, "is_undone": 1},
    ]

    _, _, touched = replay_operations(operations, qpf, ptype, valid)

    assert np.array_equal(touched, left | right)


def test_can_undo_can_redo_flags() -> None:
    assert compute_can_undo_redo([]) == (False, False)
    assert compute_can_undo_redo([{"is_undone": 0}]) == (True, False)
    assert compute_can_undo_redo([{"is_undone": 0}, {"is_undone": 1}]) == (True, True)
