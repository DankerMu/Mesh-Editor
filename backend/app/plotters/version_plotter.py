import io

import numpy as np
from matplotlib.backends.backend_agg import FigureCanvasAgg
from matplotlib.figure import Figure
from numpy.typing import NDArray

DEFAULT_EXTENT = (70.0, 111.0, 25.0, 50.0)


def _render_png(fig: Figure) -> bytes:
    canvas = FigureCanvasAgg(fig)
    buf = io.BytesIO()
    canvas.print_png(buf)
    return buf.getvalue()


def plot_precip_product(
    qpf: NDArray[np.float32],
    ptype: NDArray[np.uint8],
    extent: tuple[float, float, float, float] = DEFAULT_EXTENT,
) -> bytes:
    """Plot precipitation product and return PNG bytes."""
    fig = Figure(figsize=(10, 8))
    ax = fig.subplots()
    im = ax.imshow(
        qpf,
        origin="upper",
        extent=extent,
        aspect="auto",
        cmap="YlGnBu",
        vmin=0,
        vmax=50,
    )
    ptype_overlay = np.ma.masked_where(ptype == 0, ptype)
    ax.imshow(
        ptype_overlay,
        origin="upper",
        extent=extent,
        aspect="auto",
        cmap="Set1",
        vmin=1,
        vmax=3,
        alpha=0.18,
    )
    fig.colorbar(im, ax=ax, label="QPF (mm)")
    ax.set_title("降水产品")
    ax.set_xlabel("经度")
    ax.set_ylabel("纬度")
    return _render_png(fig)


def plot_delta_qpf(
    delta: NDArray[np.float32],
    extent: tuple[float, float, float, float] = DEFAULT_EXTENT,
) -> bytes:
    """Plot delta QPF with a diverging colormap and return PNG bytes."""
    fig = Figure(figsize=(10, 8))
    ax = fig.subplots()
    vmax = max(float(np.abs(delta).max()), 1.0)
    im = ax.imshow(
        delta,
        origin="upper",
        extent=extent,
        aspect="auto",
        cmap="RdBu_r",
        vmin=-vmax,
        vmax=vmax,
    )
    fig.colorbar(im, ax=ax, label="ΔQPF (mm)")
    ax.set_title("降水变化量")
    ax.set_xlabel("经度")
    ax.set_ylabel("纬度")
    return _render_png(fig)


def plot_change_ptype(
    change: NDArray[np.int8],
    extent: tuple[float, float, float, float] = DEFAULT_EXTENT,
) -> bytes:
    """Plot precipitation type changes and return PNG bytes."""
    fig = Figure(figsize=(10, 8))
    ax = fig.subplots()
    im = ax.imshow(
        change,
        origin="upper",
        extent=extent,
        aspect="auto",
        cmap="coolwarm",
        vmin=-3,
        vmax=3,
    )
    fig.colorbar(im, ax=ax, label="相态变化")
    ax.set_title("相态变化")
    ax.set_xlabel("经度")
    ax.set_ylabel("纬度")
    return _render_png(fig)


def plot_mask(
    mask: NDArray[np.uint8],
    extent: tuple[float, float, float, float] = DEFAULT_EXTENT,
    title: str = "掩膜",
) -> bytes:
    """Plot binary mask and return PNG bytes."""
    fig = Figure(figsize=(10, 8))
    ax = fig.subplots()
    ax.imshow(
        mask,
        origin="upper",
        extent=extent,
        aspect="auto",
        cmap="Greys",
        vmin=0,
        vmax=1,
    )
    ax.set_title(title)
    ax.set_xlabel("经度")
    ax.set_ylabel("纬度")
    return _render_png(fig)
