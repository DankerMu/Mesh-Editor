import numpy as np
from numpy.typing import NDArray


def compute_delta_qpf(
    qpf_before: NDArray[np.float32],
    qpf_after: NDArray[np.float32],
) -> NDArray[np.float32]:
    """delta_qpf = qpf_after - qpf_before. Shape preserved."""
    return (qpf_after - qpf_before).astype(np.float32)


def compute_change_ptype(
    ptype_before: NDArray[np.uint8],
    ptype_after: NDArray[np.uint8],
) -> NDArray[np.int8]:
    """change_ptype = ptype_after - ptype_before. 0 means unchanged."""
    return ptype_after.astype(np.int8) - ptype_before.astype(np.int8)


def compute_touched_mask(
    touched_mask_session: NDArray[np.uint8],
) -> NDArray[np.uint8]:
    """Pass through the session's accumulated touched_mask."""
    return touched_mask_session


def compute_changed_mask(
    qpf_before: NDArray[np.float32],
    qpf_after: NDArray[np.float32],
    ptype_before: NDArray[np.uint8],
    ptype_after: NDArray[np.uint8],
    epsilon: float = 0.001,
) -> NDArray[np.uint8]:
    """Return grid points whose final qpf or ptype differs from the base fields."""
    qpf_changed = np.abs(qpf_after - qpf_before) > epsilon
    ptype_changed = ptype_after != ptype_before
    return (qpf_changed | ptype_changed).astype(np.uint8)
