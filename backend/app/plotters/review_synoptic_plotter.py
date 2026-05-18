from __future__ import annotations

import importlib
from pathlib import Path
from typing import Any

import matplotlib.pyplot as plt
import numpy as np
from matplotlib.figure import Figure
from numpy.typing import NDArray

def _plot_boundaries(ax: Any, boundary_config: dict[str, Any] | None) -> None:
    if not boundary_config:
        return

    shapefile = boundary_config.get("province_shapefile")
    if not shapefile or not Path(str(shapefile)).exists():
        return

    try:
        geopandas = importlib.import_module("geopandas")
        gdf = geopandas.read_file(shapefile)
        gdf.boundary.plot(ax=ax, color="black", linewidth=0.4)
    except Exception:
        return


def plot_synoptic_basic(
    hgt_data: NDArray[np.float32],
    wind_u: NDArray[np.float32] | None,
    wind_v: NDArray[np.float32] | None,
    lon: NDArray[np.float32],
    lat: NDArray[np.float32],
    level: int,
    init_time: str,
    lead_hour: int,
    boundary_config: dict[str, Any] | None,
    output_path: str,
) -> str:
    if hgt_data is None:
        raise ValueError("hgt_data must not be None")
    if lon is None:
        raise ValueError("lon must not be None")
    if lat is None:
        raise ValueError("lat must not be None")

    expected_shape = (len(lat), len(lon))
    if hgt_data.shape != expected_shape:
        raise ValueError(
            f"hgt_data dimension mismatch: expected {expected_shape}, actual {hgt_data.shape}"
        )
    has_wind = wind_u is not None and wind_v is not None
    if has_wind:
        if wind_u.shape != expected_shape or wind_v.shape != expected_shape:
            raise ValueError(
                "wind field dimension mismatch: "
                f"expected {expected_shape}, actual {wind_u.shape}/{wind_v.shape}"
            )

    fig = Figure(figsize=(10, 8))
    try:
        ax = fig.subplots()
        lon2d, lat2d = np.meshgrid(lon, lat)
        contours = ax.contour(lon2d, lat2d, hgt_data, colors="black", linewidths=0.8)
        ax.clabel(contours, inline=True, fontsize=8, fmt="%d")

        if has_wind:
            step = max(1, min(len(lat), len(lon)) // 20)
            ax.barbs(
                lon2d[::step, ::step],
                lat2d[::step, ::step],
                wind_u[::step, ::step],
                wind_v[::step, ::step],
                length=5,
                linewidth=0.5,
            )

        _plot_boundaries(ax, boundary_config)
        ax.set_xlim(float(lon.min()), float(lon.max()))
        ax.set_ylim(float(lat.min()), float(lat.max()))
        ax.set_xlabel("经度")
        ax.set_ylabel("纬度")
        wind_title = "与风场" if has_wind else ""
        ax.set_title(f"{level}hPa 高度场{wind_title} {init_time} +{lead_hour:03d}h")
        fig.savefig(output_path, dpi=150)
        return output_path
    finally:
        plt.close(fig)
