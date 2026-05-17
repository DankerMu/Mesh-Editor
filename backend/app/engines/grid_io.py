from dataclasses import dataclass
from pathlib import Path

import numpy as np
from numpy.typing import NDArray

from app.core.constants import NX, NY
from app.core.error_registry import get_error
from app.core.errors import DomainError

PTYPE_VALID_VALUES = {0, 1, 2, 3}


@dataclass(frozen=True)
class GridReadResult:
    array: NDArray[np.float64] | NDArray[np.integer]
    path: Path
    shape: tuple[int, int]
    dtype: str
    nan_count: int
    invalid_count: int
    missing_mask: NDArray[np.bool_]
    invalid_mask: NDArray[np.bool_]


def _raise_domain_error(code: str, detail: dict[str, object]) -> None:
    message, http_status = get_error(code)
    raise DomainError(
        code=code,
        message=message,
        detail=detail,
        http_status=http_status,
    )


def read_txt_grid(
    path: Path,
    dtype: str = "float",
    expected_shape: tuple[int, int] = (NY, NX),
) -> GridReadResult:
    grid_path = Path(path)
    if not grid_path.exists():
        _raise_domain_error("FILE_NOT_FOUND", {"path": str(grid_path)})

    raw = np.loadtxt(grid_path, delimiter=",", dtype=np.float64)
    if raw.shape != expected_shape:
        _raise_domain_error(
            "GRID_SHAPE_MISMATCH",
            {
                "path": str(grid_path),
                "expected_shape": expected_shape,
                "actual_shape": raw.shape,
            },
        )

    missing_mask = np.isnan(raw)
    invalid_mask = np.zeros(raw.shape, dtype=bool)
    array: NDArray[np.float64] | NDArray[np.integer]

    if dtype == "float":
        array = raw.astype(np.float64, copy=False)
    elif dtype == "int":
        finite_mask = np.isfinite(raw)
        integer_like_mask = finite_mask & (raw == np.floor(raw))
        valid_value_mask = integer_like_mask & np.isin(raw, list(PTYPE_VALID_VALUES))
        invalid_mask = ~missing_mask & ~valid_value_mask
        int_raw = np.where(valid_value_mask, raw, -1)
        array = int_raw.astype(int, copy=False)
    else:
        raise ValueError(f"不支持的网格 dtype: {dtype!r}")

    return GridReadResult(
        array=array,
        path=grid_path,
        shape=raw.shape,
        dtype=dtype,
        nan_count=int(missing_mask.sum()),
        invalid_count=int(invalid_mask.sum()),
        missing_mask=missing_mask,
        invalid_mask=invalid_mask,
    )
