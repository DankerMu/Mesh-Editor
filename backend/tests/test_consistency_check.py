import numpy as np

from app.engines.consistency_check import check_qpf_ptype_consistency


def test_check_qpf_ptype_consistency_violation_detected() -> None:
    qpf = np.array([[0.0, 5.0, 2.0], [0.2, 0.0, 10.0]], dtype=np.float32)
    ptype = np.array([[0, 0, 1], [0, 0, 0]], dtype=np.uint8)

    count = check_qpf_ptype_consistency(qpf, ptype)

    assert count == 3


def test_check_qpf_ptype_consistency_no_violations() -> None:
    qpf = np.array([[0.0, 5.0, 2.0], [0.05, 0.0, 10.0]], dtype=np.float32)
    ptype = np.array([[0, 1, 2], [0, 0, 3]], dtype=np.uint8)

    count = check_qpf_ptype_consistency(qpf, ptype)

    assert count == 0


def test_check_qpf_ptype_consistency_threshold_boundary_not_violation() -> None:
    qpf = np.array([[0.1, 0.1001]], dtype=np.float32)
    ptype = np.array([[0, 0]], dtype=np.uint8)

    count = check_qpf_ptype_consistency(qpf, ptype, threshold_mm=0.1)

    assert count == 1


def test_check_qpf_ptype_consistency_all_zeros() -> None:
    qpf = np.zeros((3, 3), dtype=np.float32)
    ptype = np.zeros((3, 3), dtype=np.uint8)

    count = check_qpf_ptype_consistency(qpf, ptype)

    assert count == 0
