import io

import matplotlib

matplotlib.use("Agg")

import matplotlib.pyplot as plt
import numpy as np
from numpy.typing import NDArray

DEFAULT_EXTENT = (70.0, 111.0, 25.0, 50.0)


def plot_precip_product(
    qpf: NDArray[np.float32],
    ptype: NDArray[np.uint8],
    extent: tuple[float, float, float, float] = DEFAULT_EXTENT,
) -> bytes:
    """Plot precipitation product and return PNG bytes."""
    fig, ax = plt.subplots(figsize=(10, 8))
    im = ax.imshow(
        qpf,
        origin="lower",
        extent=extent,
        aspect="auto",
        cmap="YlGnBu",
        vmin=0,
        vmax=50,
    )
    ptype_overlay = np.ma.masked_where(ptype == 0, ptype)
    ax.imshow(
        ptype_overlay,
        origin="lower",
        extent=extent,
        aspect="auto",
        cmap="Set1",
        vmin=1,
        vmax=3,
        alpha=0.18,
    )
    plt.colorbar(im, ax=ax, label="QPF (mm)")
    ax.set_title("降水产品")
    ax.set_xlabel("经度")
    ax.set_ylabel("纬度")
    buf = io.BytesIO()
    fig.savefig(buf, format="png", dpi=100, bbox_inches="tight")
    plt.close(fig)
    return buf.getvalue()


def plot_delta_qpf(
    delta: NDArray[np.float32],
    extent: tuple[float, float, float, float] = DEFAULT_EXTENT,
) -> bytes:
    """Plot delta QPF with a diverging colormap and return PNG bytes."""
    fig, ax = plt.subplots(figsize=(10, 8))
    vmax = max(float(np.abs(delta).max()), 1.0)
    im = ax.imshow(
        delta,
        origin="lower",
        extent=extent,
        aspect="auto",
        cmap="RdBu_r",
        vmin=-vmax,
        vmax=vmax,
    )
    plt.colorbar(im, ax=ax, label="ΔQPF (mm)")
    ax.set_title("降水变化量")
    ax.set_xlabel("经度")
    ax.set_ylabel("纬度")
    buf = io.BytesIO()
    fig.savefig(buf, format="png", dpi=100, bbox_inches="tight")
    plt.close(fig)
    return buf.getvalue()


def plot_change_ptype(
    change: NDArray[np.int8],
    extent: tuple[float, float, float, float] = DEFAULT_EXTENT,
) -> bytes:
    """Plot precipitation type changes and return PNG bytes."""
    fig, ax = plt.subplots(figsize=(10, 8))
    im = ax.imshow(
        change,
        origin="lower",
        extent=extent,
        aspect="auto",
        cmap="coolwarm",
        vmin=-3,
        vmax=3,
    )
    plt.colorbar(im, ax=ax, label="相态变化")
    ax.set_title("相态变化")
    ax.set_xlabel("经度")
    ax.set_ylabel("纬度")
    buf = io.BytesIO()
    fig.savefig(buf, format="png", dpi=100, bbox_inches="tight")
    plt.close(fig)
    return buf.getvalue()


def plot_mask(
    mask: NDArray[np.uint8],
    extent: tuple[float, float, float, float] = DEFAULT_EXTENT,
    title: str = "掩膜",
) -> bytes:
    """Plot binary mask and return PNG bytes."""
    fig, ax = plt.subplots(figsize=(10, 8))
    ax.imshow(
        mask,
        origin="lower",
        extent=extent,
        aspect="auto",
        cmap="Greys",
        vmin=0,
        vmax=1,
    )
    ax.set_title(title)
    ax.set_xlabel("经度")
    ax.set_ylabel("纬度")
    buf = io.BytesIO()
    fig.savefig(buf, format="png", dpi=100, bbox_inches="tight")
    plt.close(fig)
    return buf.getvalue()
