import numpy as np
from numpy.typing import NDArray


def check_qpf_ptype_consistency(
    qpf_after: NDArray[np.float32],
    ptype_after: NDArray[np.uint8],
    threshold_mm: float = 0.1,
) -> int:
    """Return count of qpf points above threshold without precipitation type."""
    violations = (qpf_after > threshold_mm) & (ptype_after == 0)
    return int(np.sum(violations))
