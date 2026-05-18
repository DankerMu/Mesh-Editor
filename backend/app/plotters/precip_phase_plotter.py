from __future__ import annotations

import importlib
from pathlib import Path
from typing import Any

import matplotlib.pyplot as plt
import numpy as np
from matplotlib.colors import BoundaryNorm, ListedColormap
from matplotlib.figure import Figure
from matplotlib.font_manager import FontProperties, fontManager
from matplotlib.patches import Patch
from numpy.typing import NDArray

from app.plotters.version_plotter import DEFAULT_EXTENT

QPF_LEVELS = [0.1, 10.0, 25.0, 50.0, 100.0, 250.0]
QPF_COLORS = ["#a6f28e", "#3db93d", "#5fb6f8", "#0000fe", "#f900fc", "#a80000"]
PTYPE_LABELS = {1: "雨", 2: "雪", 3: "雨夹雪"}


def _get_extent(grid_def: dict[str, Any]) -> tuple[float, float, float, float]:
    extent = grid_def.get("extent", DEFAULT_EXTENT)
    if len(extent) != 4:
        return DEFAULT_EXTENT
    return tuple(float(item) for item in extent)  # type: ignore[return-value]


def _find_cjk_font() -> FontProperties | None:
    preferred_names = (
        "Noto Sans CJK",
        "Noto Sans SC",
        "Source Han Sans",
        "Microsoft YaHei",
        "SimHei",
        "PingFang",
        "Heiti",
        "Songti",
        "WenQuanYi",
    )
    for font in fontManager.ttflist:
        if any(name in font.name for name in preferred_names):
            return FontProperties(fname=font.fname)
    return None


def _plot_boundaries(
    fig: Figure,
    ax: Any,
    boundary_config: dict[str, Any] | None,
) -> None:
    if not boundary_config:
        return

    shapefile = boundary_config.get("province_shapefile")
    if not shapefile or not Path(str(shapefile)).exists():
        return

    try:
        geopandas = importlib.import_module("geopandas")
        gdf = geopandas.read_file(shapefile)
        gdf.boundary.plot(ax=ax, color="black", linewidth=0.4)
        fig.canvas.draw_idle()
    except Exception:
        return


def plot_precip_phase(
    qpf: NDArray[np.float32],
    ptype: NDArray[np.uint8],
    grid_def: dict[str, Any],
    product_type: str,
    metadata: dict[str, Any],
    boundary_config: dict[str, Any] | None,
    output_path: str,
) -> str:
    if qpf is None:
        raise ValueError("qpf must not be None")
    if ptype is None:
        raise ValueError("ptype must not be None")
    if qpf.shape != ptype.shape:
        raise ValueError(
            f"qpf and ptype dimension mismatch: expected {qpf.shape}, actual {ptype.shape}"
        )

    extent = _get_extent(grid_def)
    fig = Figure(figsize=(10, 8))
    try:
        ax = fig.subplots()
        cmap = ListedColormap(QPF_COLORS)
        cmap.set_under("#ffffff00")
        norm = BoundaryNorm([*QPF_LEVELS, 1000.0], cmap.N)
        im = ax.imshow(
            qpf,
            origin="upper",
            extent=extent,
            aspect="auto",
            cmap=cmap,
            norm=norm,
        )

        ptype_cmap = ListedColormap(["#e41a1c", "#377eb8", "#4daf4a"])
        ptype_norm = BoundaryNorm([0.5, 1.5, 2.5, 3.5], ptype_cmap.N)
        ptype_overlay = np.ma.masked_where(ptype == 0, ptype)
        ax.imshow(
            ptype_overlay,
            origin="upper",
            extent=extent,
            aspect="auto",
            cmap=ptype_cmap,
            norm=ptype_norm,
            alpha=0.18,
        )

        font_prop = _find_cjk_font()
        cbar = fig.colorbar(im, ax=ax, label="降水量 (mm)", ticks=QPF_LEVELS)
        if font_prop is not None:
            cbar.ax.yaxis.label.set_fontproperties(font_prop)
            for label in cbar.ax.get_yticklabels():
                label.set_fontproperties(font_prop)

        handles = [
            Patch(facecolor=ptype_cmap(index - 1), edgecolor="none", alpha=0.35, label=label)
            for index, label in PTYPE_LABELS.items()
        ]
        legend = ax.legend(handles=handles, loc="lower right", title="相态")
        if font_prop is not None:
            legend.get_title().set_fontproperties(font_prop)
            for text in legend.get_texts():
                text.set_fontproperties(font_prop)

        _plot_boundaries(fig, ax, boundary_config)

        suffix = "订正前" if product_type == "before" else "订正后"
        title = f"{metadata.get('window_label', '')} {suffix}降水相态".strip()
        ax.set_title(title, fontproperties=font_prop)
        ax.set_xlabel("经度", fontproperties=font_prop)
        ax.set_ylabel("纬度", fontproperties=font_prop)
        fig.savefig(output_path, dpi=150)
        return output_path
    finally:
        plt.close(fig)


def plot_placeholder(message: str, output_path: str) -> str:
    font_prop = _find_cjk_font()
    fig = Figure(figsize=(8, 6), facecolor="#F0F0F0")
    try:
        fig.text(
            0.5,
            0.5,
            message,
            ha="center",
            va="center",
            fontsize=24,
            fontproperties=font_prop,
        )
        fig.savefig(output_path, dpi=100, facecolor=fig.get_facecolor())
        return output_path
    finally:
        plt.close(fig)
