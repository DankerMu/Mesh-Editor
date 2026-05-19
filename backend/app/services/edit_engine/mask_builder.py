from __future__ import annotations

from collections.abc import Sequence
from typing import Any

import numpy as np
from scipy.ndimage import gaussian_filter
from shapely import covers, points  # type: ignore[import-untyped]
from shapely.geometry import LineString, MultiPolygon, Polygon  # type: ignore[import-untyped]
from shapely.prepared import prep  # type: ignore[import-untyped]

from app.core.constants import DLAT, DLON, LAT_MAX, LAT_MIN, LON_MAX, LON_MIN, NX, NY

GRID_SHAPE = (NY, NX)
GRID_RES = DLON


class MaskError(Exception):
    def __init__(self, code: str, detail: str = "") -> None:
        self.code = code
        self.detail = detail
        super().__init__(detail or code)


def polygon_to_mask(
    coordinates: list[list[float]], valid_mask: np.ndarray
) -> np.ndarray:
    if len(coordinates) < 3:
        raise MaskError("MASK_INVALID_GEOMETRY", "polygon 至少需要 3 个顶点")

    polygon = Polygon(_clamp_points(coordinates))
    if polygon.is_empty or not polygon.is_valid or polygon.area <= 0:
        raise MaskError("MASK_INVALID_GEOMETRY", "polygon 几何无效")

    return _finalize_mask(_geometry_to_mask(polygon), valid_mask)


def lasso_to_mask(
    coordinates: list[list[float]], valid_mask: np.ndarray
) -> np.ndarray:
    if len(coordinates) < 3:
        raise MaskError("MASK_INVALID_GEOMETRY", "lasso 至少需要 3 个轨迹点")
    MAX_LASSO_POINTS = 10000
    if len(coordinates) > MAX_LASSO_POINTS:
        raise MaskError(
            "MASK_INVALID_GEOMETRY", f"lasso 轨迹点数超出上限 {MAX_LASSO_POINTS}"
        )

    try:
        simplified_line = LineString(coordinates).simplify(
            0.01, preserve_topology=False
        )
    except Exception as exc:
        raise MaskError("MASK_INVALID_GEOMETRY", "lasso 几何无效") from exc

    simplified = list(simplified_line.coords)
    clamped = _clamp_points(simplified)
    if clamped and clamped[0] != clamped[-1]:
        clamped.append(clamped[0])

    if len(set(clamped[:-1] if clamped[:1] == clamped[-1:] else clamped)) < 3:
        raise MaskError("MASK_INVALID_GEOMETRY", "lasso 简化后少于 3 个顶点")

    polygon = Polygon(clamped)
    if polygon.is_empty:
        raise MaskError("MASK_INVALID_GEOMETRY", "lasso 几何无效")

    geometry: Polygon | MultiPolygon = polygon
    if not geometry.is_valid:
        fixed = geometry.buffer(0)
        if fixed.is_empty or not fixed.is_valid or not isinstance(fixed, (Polygon, MultiPolygon)):
            raise MaskError("MASK_INVALID_GEOMETRY", "lasso 自交叉修复失败")
        geometry = fixed

    if geometry.is_empty or geometry.area <= 0:
        raise MaskError("MASK_INVALID_GEOMETRY", "lasso 几何无效")

    return _finalize_mask(_geometry_to_mask(geometry), valid_mask)


def line_buffer_to_mask(
    coordinates: list[list[float]], width_grid: float, valid_mask: np.ndarray
) -> np.ndarray:
    if len(coordinates) < 2 or width_grid <= 0:
        raise MaskError("MASK_INVALID_GEOMETRY", "line_buffer 参数无效")

    line = LineString(_clamp_points(coordinates))
    if line.is_empty or line.length <= 0:
        raise MaskError("MASK_INVALID_GEOMETRY", "line_buffer 几何无效")

    buffered = line.buffer(float(width_grid) * GRID_RES)
    if buffered.is_empty:
        raise MaskError("MASK_INVALID_GEOMETRY", "line_buffer 缓冲区为空")

    return _finalize_mask(_geometry_to_mask(buffered), valid_mask)


def brush_path_to_mask(
    points_: list[list[float]], radius_grid: float, valid_mask: np.ndarray
) -> np.ndarray:
    if len(points_) < 1 or radius_grid <= 0:
        raise MaskError("MASK_INVALID_GEOMETRY", "brush_path 参数无效")

    mask = np.zeros(GRID_SHAPE, dtype=bool)
    radius = float(radius_grid)
    radius_sq = radius * radius

    for lon, lat in _clamp_points(points_):
        col = (lon - LON_MIN) / DLON
        row = (lat - LAT_MIN) / DLAT
        row_min = max(0, int(np.floor(row - radius)))
        row_max = min(NY - 1, int(np.ceil(row + radius)))
        col_min = max(0, int(np.floor(col - radius)))
        col_max = min(NX - 1, int(np.ceil(col + radius)))

        row_indices = np.arange(row_min, row_max + 1, dtype=np.float64)[:, None]
        col_indices = np.arange(col_min, col_max + 1, dtype=np.float64)[None, :]
        local_mask = (row_indices - row) ** 2 + (col_indices - col) ** 2 <= radius_sq
        mask[row_min : row_max + 1, col_min : col_max + 1] |= local_mask

    return _finalize_mask(mask, valid_mask)


def smooth_mask(mask: np.ndarray, sigma: float, valid_mask: np.ndarray) -> np.ndarray:
    if sigma == 0:
        return mask
    import math
    if not math.isfinite(sigma) or sigma < 0.5 or sigma > 5.0:
        raise MaskError("SMOOTH_SIGMA_OUT_OF_RANGE", "sigma 必须在 0.5 到 5.0 之间")

    float_mask = mask.astype(np.float64)
    smoothed = gaussian_filter(float_mask, sigma=sigma)
    result = smoothed >= 0.5
    return _finalize_mask(result, valid_mask)


def _clamp_points(coordinates: Sequence[Sequence[float]]) -> list[tuple[float, float]]:
    return [
        (
            min(max(float(point[0]), LON_MIN), LON_MAX),
            min(max(float(point[1]), LAT_MIN), LAT_MAX),
        )
        for point in coordinates
    ]


def _geometry_to_mask(geometry: Any) -> np.ndarray:
    minx, miny, maxx, maxy = geometry.bounds
    col_min = max(0, int(np.floor((minx - LON_MIN) / DLON)))
    col_max = min(NX - 1, int(np.ceil((maxx - LON_MIN) / DLON)))
    row_min = max(0, int(np.floor((miny - LAT_MIN) / DLAT)))
    row_max = min(NY - 1, int(np.ceil((maxy - LAT_MIN) / DLAT)))

    mask = np.zeros(GRID_SHAPE, dtype=bool)
    if row_min > row_max or col_min > col_max:
        return mask

    lon_values = LON_MIN + np.arange(col_min, col_max + 1, dtype=np.float64) * DLON
    lat_values = LAT_MIN + np.arange(row_min, row_max + 1, dtype=np.float64) * DLAT
    lon_grid, lat_grid = np.meshgrid(lon_values, lat_values)

    try:
        covered = np.asarray(covers(geometry, points(lon_grid, lat_grid)), dtype=bool)
    except Exception:
        covered = _prepared_covers(geometry, lon_grid, lat_grid)

    mask[row_min : row_max + 1, col_min : col_max + 1] = covered
    return mask


def _prepared_covers(
    geometry: Any, lon_grid: np.ndarray, lat_grid: np.ndarray
) -> np.ndarray:
    prepared = prep(geometry)
    result = np.zeros(lon_grid.shape, dtype=bool)
    flat_lon = lon_grid.ravel()
    flat_lat = lat_grid.ravel()
    flat_result = result.ravel()
    for index, (lon, lat) in enumerate(zip(flat_lon, flat_lat, strict=True)):
        flat_result[index] = prepared.covers(points(float(lon), float(lat)))
    return result


def _finalize_mask(mask: np.ndarray, valid_mask: np.ndarray) -> np.ndarray:
    result = np.asarray(mask, dtype=bool) & np.asarray(valid_mask, dtype=bool)
    if not bool(np.any(result)):
        raise MaskError("MASK_EMPTY", "选区与有效网格无交集")
    return result
