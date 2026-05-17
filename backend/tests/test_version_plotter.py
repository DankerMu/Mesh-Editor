import numpy as np

from app.core.constants import NX, NY
from app.plotters.version_plotter import (
    plot_change_ptype,
    plot_delta_qpf,
    plot_mask,
    plot_precip_product,
)

PNG_HEADER = b"\x89PNG"


def _assert_valid_png(data: bytes) -> None:
    assert isinstance(data, bytes)
    assert len(data) > 0
    assert data.startswith(PNG_HEADER)


def test_plot_precip_product_produces_valid_png() -> None:
    rng = np.random.default_rng(42)
    qpf = rng.uniform(0, 50, size=(NY, NX)).astype(np.float32)
    ptype = rng.integers(0, 4, size=(NY, NX), dtype=np.uint8)

    png = plot_precip_product(qpf, ptype)

    _assert_valid_png(png)


def test_plot_delta_qpf_produces_valid_png() -> None:
    rng = np.random.default_rng(42)
    delta = rng.uniform(-20, 20, size=(NY, NX)).astype(np.float32)

    png = plot_delta_qpf(delta)

    _assert_valid_png(png)


def test_plot_change_ptype_produces_valid_png() -> None:
    rng = np.random.default_rng(42)
    change = rng.integers(-3, 4, size=(NY, NX), dtype=np.int8)

    png = plot_change_ptype(change)

    _assert_valid_png(png)


def test_plot_mask_produces_valid_png() -> None:
    rng = np.random.default_rng(42)
    mask = rng.integers(0, 2, size=(NY, NX), dtype=np.uint8)

    png = plot_mask(mask)

    _assert_valid_png(png)
