from __future__ import annotations

import json
import math
import traceback
from datetime import UTC, datetime, timedelta
from pathlib import Path
from typing import Any, Callable

import numpy as np
from PIL import Image
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import settings
from app.core.constants import LAT_MAX, LAT_MIN, LON_MAX, LON_MIN
from app.core.error_registry import get_error
from app.db.models import ReviewProduct
from app.plotters.precip_phase_plotter import plot_placeholder, plot_precip_phase
from app.plotters.review_synoptic_plotter import plot_synoptic_basic
from app.plotters.version_plotter import (
    DEFAULT_EXTENT,
    plot_change_ptype,
    plot_delta_qpf,
)
from app.repositories.review_product_repo import review_product_repo
from app.storage.path_builder import PathBuilder, path_builder as default_path_builder

PanelPlotter = Callable[..., Any]

PANEL_DISPATCH: dict[str, PanelPlotter] = {
    "precip_phase": plot_precip_phase,
    "delta_qpf": plot_delta_qpf,
    "change_ptype": plot_change_ptype,
    "circulation": plot_synoptic_basic,
}

REQUIRED_EDIT_FIELDS = {
    "qpf_before",
    "ptype_before",
    "qpf_after",
    "ptype_after",
    "delta_qpf",
    "change_ptype",
}


def _error_message(code: str) -> str:
    message, _status = get_error(code)
    return message


class PlotTaskService:
    def __init__(self, path_builder: PathBuilder | None = None) -> None:
        self.path_builder = path_builder or default_path_builder
        queue_config = settings.product_config.get("plot_queue", {})
        if not isinstance(queue_config, dict):
            queue_config = {}
        self.max_concurrent = int(queue_config.get("max_concurrent", 2))
        self.max_retries = int(queue_config.get("max_retries", 3))
        self.retry_delay_seconds = int(queue_config.get("retry_delay_seconds", 30))
        self.task_timeout_seconds = int(queue_config.get("task_timeout_seconds", 300))

    async def claim_task(
        self, db: AsyncSession, worker_id: str
    ) -> ReviewProduct | None:
        return await review_product_repo.claim_task(db, worker_id, self.max_concurrent)

    async def complete_task(
        self,
        db: AsyncSession,
        review_id: str,
        status: str,
        **results: Any,
    ) -> ReviewProduct | None:
        product = await review_product_repo.get_by_id(db, review_id)
        if product is None:
            return None

        final_status = status
        extra = {key: value for key, value in results.items() if value is not None}
        now = datetime.now(UTC)
        if status == "failed":
            if int(product.attempt) >= self.max_retries:
                final_status = "permanently_failed"
                extra["next_retry_at"] = None
            else:
                extra["next_retry_at"] = now + timedelta(
                    seconds=self.retry_delay_seconds
                )
        extra["locked_by"] = None
        extra["locked_at"] = None
        extra["plot_finished_at"] = now
        return await review_product_repo.update_status(
            db, review_id, final_status, **extra
        )

    async def recover_stale_tasks(self, db: AsyncSession) -> None:
        stale_tasks = await review_product_repo.list_stale_tasks(
            db, self.task_timeout_seconds
        )
        now = datetime.now(UTC)
        for task in stale_tasks:
            if int(task.attempt) >= self.max_retries:
                await review_product_repo.update_status(
                    db,
                    str(task.review_id),
                    "permanently_failed",
                    locked_by=None,
                    locked_at=None,
                    plot_finished_at=now,
                )
            else:
                await review_product_repo.update_status(
                    db,
                    str(task.review_id),
                    "failed",
                    locked_by=None,
                    locked_at=None,
                    next_retry_at=now + timedelta(seconds=self.retry_delay_seconds),
                    plot_finished_at=now,
                )

    async def recover_retryable_failed_tasks(self, db: AsyncSession) -> None:
        now = datetime.now(UTC)
        result = await db.execute(
            select(ReviewProduct).where(
                ReviewProduct.plot_status == "failed",
                ReviewProduct.next_retry_at.is_not(None),
                ReviewProduct.next_retry_at <= now,
            )
        )
        for task in result.scalars().all():
            await review_product_repo.update_status(
                db,
                str(task.review_id),
                "pending",
                locked_by=None,
                locked_at=None,
                next_retry_at=None,
            )

    async def execute_task(
        self, db: AsyncSession, review_id: str
    ) -> ReviewProduct | None:
        product = await review_product_repo.get_by_id(db, review_id)
        if product is None:
            raise ValueError(_error_message("PLOT_TASK_NOT_FOUND"))

        logs: list[str] = []
        panel_paths: list[Path] = []
        success_panels = 0
        skipped_panels = 0
        missing_fields: list[dict[str, Any]] = []
        log_path = self.path_builder.review_log_path(
            str(product.window_id), str(product.review_id)
        )
        image_path: Path | None = None

        try:
            self._log(logs, "task start")
            payload = self._load_payload(product)
            log_path = Path(
                payload.get("output", {}).get("plot_log_path")
                or self.path_builder.review_log_path(
                    str(product.window_id), str(product.review_id)
                )
            )
            images_dir = Path(
                payload.get("output", {}).get("images_dir")
                or self.path_builder.review_images_dir(
                    str(product.window_id), str(product.review_id)
                )
            )
            images_dir.mkdir(parents=True, exist_ok=True)
            await review_product_repo.update_status(
                db,
                review_id,
                str(product.plot_status),
                plot_started_at=datetime.now(UTC),
            )
            self._log(logs, "load payload")

            review_root = Path(
                payload.get("output", {}).get("review_root")
                or self.path_builder.review_root(
                    str(product.window_id), str(product.review_id)
                )
            )
            fields = self._field_map(payload, review_root)
            missing_fields.extend(payload.get("missing_fields", []))
            required_fields = set(
                payload.get("template", {}).get("required_fields", [])
            )
            missing_required = [
                field_name for field_name in required_fields if field_name not in fields
            ]
            if missing_required:
                for field_name in missing_required:
                    self._append_missing(
                        missing_fields,
                        variable_name=field_name,
                        reason="file_not_found",
                    )
                raise ValueError(
                    f"必需复盘字段缺失: {', '.join(sorted(missing_required))}"
                )

            panels = payload.get("template", {}).get("panels", [])
            for index, panel in enumerate(panels, start=1):
                panel_id = str(panel["id"])
                panel_type = str(panel["type"])
                panel_fields = [str(item) for item in panel.get("fields", [])]
                output_path = images_dir / f"{index:02d}_{panel_id}.png"
                missing_for_panel = [
                    field_name
                    for field_name in panel_fields
                    if field_name not in fields
                ]
                required_missing = [
                    field_name
                    for field_name in missing_for_panel
                    if field_name in REQUIRED_EDIT_FIELDS
                    or field_name in required_fields
                ]
                if required_missing:
                    raise ValueError(
                        f"panel {panel_id} 缺少必需字段: {', '.join(required_missing)}"
                    )
                if missing_for_panel:
                    plot_placeholder(
                        self._placeholder_message(missing_for_panel, missing_fields),
                        str(output_path),
                    )
                    panel_paths.append(output_path)
                    skipped_panels += 1
                    self._log(
                        logs,
                        f"panel {panel_id} skipped: missing {','.join(missing_for_panel)}",
                    )
                    continue

                self._plot_panel(
                    panel_type=panel_type,
                    panel_id=panel_id,
                    fields=fields,
                    panel_fields=panel_fields,
                    payload=payload,
                    output_path=output_path,
                    review_root=review_root,
                )
                panel_paths.append(output_path)
                success_panels += 1
                self._log(logs, f"panel {panel_id} success")

            image_path = Path(
                payload.get("output", {}).get("composite_image_path")
                or images_dir / "review_composite.png"
            )
            self._generate_composite(panel_paths, image_path)
            self._log(logs, "composite success")
            status = "partial_success" if skipped_panels > 0 else "success"
            self._log(logs, f"task {status}")
            self._write_log(log_path, logs)
            return await self.complete_task(
                db,
                review_id,
                status,
                image_path=str(image_path),
                total_panels=len(panels),
                success_panels=success_panels,
                skipped_panels=skipped_panels,
                missing_fields_json=json.dumps(
                    missing_fields, ensure_ascii=False, default=str
                ),
                error_log_path=str(log_path),
            )
        except Exception:
            self._log(logs, traceback.format_exc())
            self._log(logs, "task failed")
            self._write_log(log_path, logs)
            return await self.complete_task(
                db,
                review_id,
                "failed",
                image_path=None if image_path is None else str(image_path),
                success_panels=success_panels,
                skipped_panels=skipped_panels,
                missing_fields_json=json.dumps(
                    missing_fields, ensure_ascii=False, default=str
                ),
                error_log_path=str(log_path),
            )

    def _load_payload(self, product: ReviewProduct) -> dict[str, Any]:
        candidates = [
            self.path_builder.review_payload_path(
                str(product.window_id), str(product.review_id)
            ),
            Path(str(product.plot_config_path)) if product.plot_config_path else None,
            (
                Path(str(product.plot_input_manifest_path))
                if product.plot_input_manifest_path
                else None
            ),
        ]
        for path in candidates:
            if path is not None and path.exists():
                return json.loads(path.read_text(encoding="utf-8"))
        raise FileNotFoundError("review_payload.json not found")

    def _field_map(
        self, payload: dict[str, Any], review_root: Path
    ) -> dict[str, dict[str, Any]]:
        fields: dict[str, dict[str, Any]] = {}
        for name, field in payload.get("edit_fields", {}).items():
            field_payload = (
                {"variable_name": str(name), "path": str(field)}
                if isinstance(field, str)
                else dict(field)
            )
            if self._field_path(field_payload, review_root).exists():
                fields[str(name)] = field_payload
        for field in payload.get("ifs_fields", []):
            name = str(field["variable_name"])
            if self._field_path(field, review_root).exists():
                fields[name] = dict(field)
        return fields

    def _plot_panel(
        self,
        *,
        panel_type: str,
        panel_id: str,
        fields: dict[str, dict[str, Any]],
        panel_fields: list[str],
        payload: dict[str, Any],
        output_path: Path,
        review_root: Path,
    ) -> None:
        plotter = PANEL_DISPATCH.get(panel_type)
        if plotter is None:
            raise ValueError(f"未知面板类型: {panel_type}")

        grid_def = payload.get("output", {}).get("grid_def", {})
        metadata = payload.get("metadata", {})
        if panel_type == "precip_phase":
            qpf_name = panel_fields[0]
            ptype_name = panel_fields[1]
            plotter(
                qpf=self._load_array(fields[qpf_name], review_root).astype(np.float32),
                ptype=self._load_array(fields[ptype_name], review_root).astype(
                    np.uint8
                ),
                grid_def=grid_def,
                product_type="before" if "before" in panel_id else "after",
                metadata=metadata,
                boundary_config=None,
                output_path=str(output_path),
            )
            return

        if panel_type == "delta_qpf":
            png = plotter(
                self._load_array(fields[panel_fields[0]], review_root).astype(
                    np.float32
                ),
                self._extent(grid_def),
            )
            output_path.write_bytes(png)
            return

        if panel_type == "change_ptype":
            png = plotter(
                self._load_array(fields[panel_fields[0]], review_root).astype(np.int8),
                self._extent(grid_def),
            )
            output_path.write_bytes(png)
            return

        if panel_type == "circulation":
            hgt_field = fields["z500"]
            hgt = self._load_array(hgt_field, review_root).astype(np.float32)
            wind_u = (
                self._load_array(fields["u850"], review_root).astype(np.float32)
                if "u850" in fields
                else None
            )
            wind_v = (
                self._load_array(fields["v850"], review_root).astype(np.float32)
                if "v850" in fields
                else None
            )
            lat = np.linspace(LAT_MIN, LAT_MAX, hgt.shape[0], dtype=np.float32)
            lon = np.linspace(LON_MIN, LON_MAX, hgt.shape[1], dtype=np.float32)
            plotter(
                hgt_data=hgt,
                wind_u=wind_u,
                wind_v=wind_v,
                lon=lon,
                lat=lat,
                level=int(hgt_field.get("level_value") or 500),
                init_time=str(metadata.get("case_id", "")),
                lead_hour=int(
                    hgt_field.get("lead_hour") or metadata.get("end_lead", 0)
                ),
                boundary_config=None,
                output_path=str(output_path),
            )
            return

        raise ValueError(f"未知面板类型: {panel_type}")

    def _load_array(
        self, field: dict[str, Any], review_root: Path | None = None
    ) -> np.ndarray[Any, Any]:
        path = self._field_path(field, review_root)
        with np.load(path) as payload:
            if "data" in payload:
                return np.asarray(payload["data"])
            if len(payload.files) == 1:
                return np.asarray(payload[payload.files[0]])
            for key in ("qpf", "ptype", "mask"):
                if key in payload:
                    return np.asarray(payload[key])
        raise ValueError(f"npz 文件缺少可用数组: {path}")

    def _field_path(
        self, field: dict[str, Any], review_root: Path | None = None
    ) -> Path:
        raw = field.get("path") or field.get("relative_path") or field.get("file_path")
        if raw is None:
            return Path("__missing__")
        path = Path(str(raw))
        if path.is_absolute() or review_root is None:
            return path
        return review_root / path

    def _generate_composite(self, panel_paths: list[Path], output_path: Path) -> None:
        if not panel_paths:
            plot_placeholder("无可用复盘面板", str(output_path))
            return
        images = [Image.open(path).convert("RGB") for path in panel_paths]
        try:
            width = max(image.width for image in images)
            height = max(image.height for image in images)
            columns = min(3, max(1, math.ceil(math.sqrt(len(images)))))
            rows = math.ceil(len(images) / columns)
            composite = Image.new("RGB", (columns * width, rows * height), "white")
            for index, image in enumerate(images):
                resized = image.resize((width, height))
                x = (index % columns) * width
                y = (index // columns) * height
                composite.paste(resized, (x, y))
            output_path.parent.mkdir(parents=True, exist_ok=True)
            composite.save(output_path, format="PNG")
        finally:
            for image in images:
                image.close()

    def _extent(self, grid_def: dict[str, Any]) -> tuple[float, float, float, float]:
        extent = grid_def.get("extent", DEFAULT_EXTENT)
        if not isinstance(extent, list | tuple) or len(extent) != 4:
            return DEFAULT_EXTENT
        return tuple(float(item) for item in extent)  # type: ignore[return-value]

    def _placeholder_message(
        self, field_names: list[str], missing_fields: list[dict[str, Any]]
    ) -> str:
        field_name = field_names[0]
        for item in missing_fields:
            if item.get("variable_name") == field_name:
                level = item.get("level_value")
                lead = item.get("lead_hour")
                level_text = "" if level is None else f" {level}hPa"
                lead_text = "" if lead is None else f" +{int(lead):03d}h"
                return f"数据缺失：{field_name}{level_text}{lead_text}"
        return f"数据缺失：{', '.join(field_names)}"

    def _append_missing(
        self,
        missing_fields: list[dict[str, Any]],
        *,
        variable_name: str,
        reason: str,
    ) -> None:
        if any(item.get("variable_name") == variable_name for item in missing_fields):
            return
        missing_fields.append(
            {
                "variable_name": variable_name,
                "level_type": None,
                "level_value": None,
                "lead_hour": None,
                "reason": reason,
            }
        )

    def _log(self, logs: list[str], message: str) -> None:
        logs.append(f"[{datetime.now(UTC).isoformat()}] {message}")

    def _write_log(self, path: Path, logs: list[str]) -> None:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text("\n".join(logs) + "\n", encoding="utf-8")
