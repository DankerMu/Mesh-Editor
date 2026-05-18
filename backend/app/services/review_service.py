from __future__ import annotations

import json
import os
from datetime import UTC, timedelta
from pathlib import Path
from typing import Any
from uuid import uuid4

import numpy as np
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import settings
from app.core.constants import DLAT, DLON, LAT_MAX, LAT_MIN, LON_MAX, LON_MIN, NX, NY
from app.core.error_registry import get_error
from app.db.models import EditVersion, ProductWindow
from app.repositories.review_field_repo import review_field_repo
from app.repositories.review_product_repo import review_product_repo
from app.schemas.review import ReviewGenerateResponse
from app.services.review_templates import ReviewTemplate, load_template
from app.storage.path_builder import PathBuilder, path_builder as default_path_builder

IFS_FIELD_META: dict[str, dict[str, Any]] = {
    "z500": {"level_type": "pressure", "level_value": 500, "unit": "gpm"},
    "t850": {"level_type": "pressure", "level_value": 850, "unit": "K"},
    "rh700": {"level_type": "pressure", "level_value": 700, "unit": "%"},
    "u850": {"level_type": "pressure", "level_value": 850, "unit": "m/s"},
    "v850": {"level_type": "pressure", "level_value": 850, "unit": "m/s"},
}


def _error_message(code: str) -> str:
    message, _status = get_error(code)
    return message


def _raise_error(code: str) -> None:
    raise ValueError(_error_message(code))


def resolve_lead_hours(start_lead: int, end_lead: int, policy: list[str]) -> list[int]:
    """Resolve lead hours from start/end/middle sampling policy."""
    results: list[int] = []
    for item in policy:
        if item == "start":
            results.append(start_lead)
        elif item == "end":
            results.append(end_lead)
        elif item == "middle":
            mid = (start_lead + end_lead) // 2
            results.append(mid - (mid % 3))
    return sorted(set(results))


async def generate_review(
    db: AsyncSession,
    window_id: str,
    version_id: str,
    template_id: str,
    user_id: str,
    path_builder: PathBuilder | None = None,
) -> ReviewGenerateResponse:
    service = ReviewService(path_builder=path_builder)
    return await service.generate_review(
        db=db,
        window_id=window_id,
        version_id=version_id,
        template_id=template_id,
        user_id=user_id,
    )


class ReviewService:
    def __init__(self, path_builder: PathBuilder | None = None) -> None:
        self.path_builder = path_builder or default_path_builder

    async def generate_review(
        self,
        db: AsyncSession,
        window_id: str,
        version_id: str,
        template_id: str,
        user_id: str,
    ) -> ReviewGenerateResponse:
        version = await db.get(EditVersion, version_id)
        if version is None:
            _raise_error("VERSION_NOT_FOUND")
        assert version is not None
        if str(version.window_id) != window_id or str(version.status) not in {
            "approved",
            "released",
        }:
            _raise_error("VERSION_STATUS_CONFLICT")

        try:
            template = load_template(template_id)
        except ValueError:
            _raise_error("TEMPLATE_NOT_FOUND")

        window = await db.get(ProductWindow, window_id)
        if window is None:
            _raise_error("WINDOW_NOT_FOUND")
        assert window is not None

        edit_fields, missing_fields = self._build_edit_fields(window, version, template)
        missing_required = [
            field for field in template.required_fields if field not in edit_fields
        ]
        if missing_required:
            _raise_error("REQUIRED_FIELD_MISSING")

        review_id = str(uuid4())
        review_root = self.path_builder.review_root(window_id, review_id)
        review_root.mkdir(parents=True, exist_ok=False)
        images_dir = self.path_builder.review_images_dir(window_id, review_id)
        images_dir.mkdir(parents=True, exist_ok=True)

        review_windows = self._build_review_windows(window, missing_fields)
        ifs_fields = self._resolve_ifs_fields(window, template, missing_fields)
        payload_edit_fields = self._relative_field_paths(edit_fields, review_root)
        payload_ifs_fields = [
            {
                **field,
                "path": self._relative_path(Path(str(field["path"])), review_root),
                "relative_path": self._relative_path(
                    Path(str(field["path"])), review_root
                ),
            }
            for field in ifs_fields
        ]

        await review_product_repo.supersede_existing(
            db, window_id=window_id, version_id=version_id, template_id=template_id
        )

        payload_path = self.path_builder.review_payload_path(window_id, review_id)
        log_path = self.path_builder.review_log_path(window_id, review_id)
        composite_path = images_dir / "review_composite.png"
        payload = self._assemble_payload(
            review_id=review_id,
            window=window,
            version=version,
            template=template,
            edit_fields=payload_edit_fields,
            review_windows=review_windows,
            ifs_fields=payload_ifs_fields,
            missing_fields=missing_fields,
            review_root=review_root,
            payload_path=payload_path,
            log_path=log_path,
            composite_path=composite_path,
            user_id=user_id,
        )
        payload_path.write_text(
            json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8"
        )

        product = await review_product_repo.create(
            db,
            review_id=review_id,
            window_id=window_id,
            version_id=version_id,
            template_id=template_id,
            plot_status="pending",
            max_retries=int(
                settings.product_config.get("plot_queue", {}).get("max_retries", 3)
                if isinstance(settings.product_config.get("plot_queue"), dict)
                else 3
            ),
            plot_config_path=str(payload_path),
            plot_input_manifest_path=str(payload_path),
            error_log_path=str(log_path),
            total_panels=len(template.panels),
        )

        edit_field_records = [
            {
                "variable_name": field_name,
                "source_model": "edit",
                "path": path,
                "unit": self._unit_for_field(field_name),
            }
            for field_name, path in edit_fields.items()
        ]
        for field in [*edit_field_records, *ifs_fields]:
            await review_field_repo.create(
                db,
                field_id=str(uuid4()),
                window_id=window_id,
                version_id=version_id,
                source_model=str(field.get("source_model", "edit")),
                variable_name=str(field["variable_name"]),
                level_type=field.get("level_type"),
                level_value=field.get("level_value"),
                lead_hour=field.get("lead_hour"),
                valid_time=field.get("valid_time"),
                unit=field.get("unit"),
                file_path=str(review_root / str(field["path"])),
            )

        return ReviewGenerateResponse(
            review_id=str(product.review_id),
            plot_status=str(product.plot_status),
            message="复盘绘图任务已创建",
        )

    def _build_edit_fields(
        self,
        window: ProductWindow,
        version: EditVersion,
        template: ReviewTemplate,
    ) -> tuple[dict[str, str], list[dict[str, Any]]]:
        candidates = {
            "qpf_before": window.qpf_before_path,
            "ptype_before": window.ptype_before_path,
            "qpf_after": version.qpf_after_path,
            "ptype_after": version.ptype_after_path,
            "delta_qpf": version.delta_qpf_path,
            "change_ptype": version.change_ptype_path,
        }
        edit_fields: dict[str, str] = {}
        missing_fields: list[dict[str, Any]] = []
        for field_name, raw_path in candidates.items():
            path = Path(str(raw_path)) if raw_path else None
            if path is not None and path.exists():
                edit_fields[field_name] = str(path)
            elif field_name in template.required_fields:
                missing_fields.append(
                    {
                        "variable_name": field_name,
                        "level_type": None,
                        "level_value": None,
                        "lead_hour": None,
                        "reason": "file_not_found",
                        "expected_path": None if path is None else str(path),
                    }
                )
        return edit_fields, missing_fields

    def _build_review_windows(
        self, window: ProductWindow, missing_fields: list[dict[str, Any]]
    ) -> dict[str, Any]:
        case_id = str(window.case_id)
        start_lead = int(window.start_lead)
        end_lead = int(window.end_lead)
        max_lead = int(settings.product_config.get("max_lead_hours", 240))
        windows: dict[str, Any] = {
            "current": {
                "start_lead": start_lead,
                "end_lead": end_lead,
                "qpf_path": window.qpf_before_path,
                "ptype_path": window.ptype_before_path,
            },
            "prev24": None,
            "next24": None,
        }
        if start_lead >= 24:
            windows["prev24"] = self._build_qpf_window_entry(
                case_id=case_id,
                name="prev24",
                start_lead=start_lead - 24,
                end_lead=start_lead,
                missing_fields=missing_fields,
            )
        if end_lead + 24 <= max_lead:
            windows["next24"] = self._build_qpf_window_entry(
                case_id=case_id,
                name="next24",
                start_lead=end_lead,
                end_lead=end_lead + 24,
                missing_fields=missing_fields,
            )
        return windows

    def _build_qpf_window_entry(
        self,
        *,
        case_id: str,
        name: str,
        start_lead: int,
        end_lead: int,
        missing_fields: list[dict[str, Any]],
    ) -> dict[str, Any]:
        start_path = self._tp_file_path(case_id, start_lead)
        end_path = self._tp_file_path(case_id, end_lead)
        qpf_path: str | None = None
        if start_path.exists() and end_path.exists():
            start = np.loadtxt(start_path, delimiter=",", dtype=np.float64)
            end = np.loadtxt(end_path, delimiter=",", dtype=np.float64)
            if start.shape == end.shape:
                output_path = (
                    self.path_builder.case_root(case_id)
                    / "review_windows"
                    / f"{case_id}_{name}_{start_lead:03d}_{end_lead:03d}_qpf.npz"
                )
                output_path.parent.mkdir(parents=True, exist_ok=True)
                np.savez_compressed(output_path, data=(end - start).astype(np.float32))
                qpf_path = str(output_path)
            else:
                missing_fields.append(
                    {
                        "variable_name": f"{name}_qpf",
                        "level_type": "surface",
                        "level_value": None,
                        "lead_hour": end_lead,
                        "reason": "dimension_mismatch",
                        "expected_path": str(end_path),
                    }
                )
        else:
            missing_fields.append(
                {
                    "variable_name": f"{name}_qpf",
                    "level_type": "surface",
                    "level_value": None,
                    "lead_hour": end_lead,
                    "reason": "file_not_found",
                    "expected_path": str(end_path),
                }
            )
        return {
            "start_lead": start_lead,
            "end_lead": end_lead,
            "start_tp_path": str(start_path),
            "end_tp_path": str(end_path),
            "qpf_path": qpf_path,
        }

    def _resolve_ifs_fields(
        self,
        window: ProductWindow,
        template: ReviewTemplate,
        missing_fields: list[dict[str, Any]],
    ) -> list[dict[str, Any]]:
        policy = self._policy_for_template(template)
        leads = resolve_lead_hours(int(window.start_lead), int(window.end_lead), policy)
        ifs_fields: list[dict[str, Any]] = []
        for variable_name in template.optional_fields:
            if variable_name not in IFS_FIELD_META:
                continue
            meta = IFS_FIELD_META[variable_name]
            for lead_hour in leads:
                path = self._find_ifs_field_path(
                    case_id=str(window.case_id),
                    variable_name=variable_name,
                    lead_hour=lead_hour,
                )
                if path is None:
                    missing_fields.append(
                        {
                            "variable_name": variable_name,
                            "level_type": meta["level_type"],
                            "level_value": meta["level_value"],
                            "lead_hour": lead_hour,
                            "reason": "file_not_found",
                            "expected_path": self._expected_ifs_path(
                                str(window.case_id), variable_name, lead_hour
                            ),
                        }
                    )
                    continue
                ifs_fields.append(
                    {
                        "variable_name": variable_name,
                        "source_model": "ifs",
                        "level_type": meta["level_type"],
                        "level_value": meta["level_value"],
                        "lead_hour": lead_hour,
                        "valid_time": self._valid_time(window, lead_hour),
                        "unit": meta["unit"],
                        "path": str(path),
                        "relative_path": str(path),
                    }
                )
        return ifs_fields

    def _assemble_payload(
        self,
        *,
        review_id: str,
        window: ProductWindow,
        version: EditVersion,
        template: ReviewTemplate,
        edit_fields: dict[str, str],
        review_windows: dict[str, Any],
        ifs_fields: list[dict[str, Any]],
        missing_fields: list[dict[str, Any]],
        review_root: Path,
        payload_path: Path,
        log_path: Path,
        composite_path: Path,
        user_id: str,
    ) -> dict[str, Any]:
        metadata = {
            "review_id": review_id,
            "case_id": window.case_id,
            "window_id": window.window_id,
            "version_id": version.version_id,
            "init_time": None,
            "accum_hours": int(window.accum_hours),
            "start_lead": int(window.start_lead),
            "end_lead": int(window.end_lead),
            "grid_id": settings.product_config.get(
                "grid_id", settings.grid_definition.projection
            ),
            "generated_by": user_id,
        }
        template_payload = {
            "template_id": template.template_id,
            "template_name": template.template_name,
            "required_fields": template.required_fields,
            "optional_fields": template.optional_fields,
            "allow_partial_success": template.allow_partial_success,
            "review_time_policy": template.review_time_policy,
            "panels": [
                {"id": panel.id, "type": panel.type, "fields": panel.fields}
                for panel in template.panels
            ],
        }
        return {
            "metadata": metadata,
            "edit_fields": edit_fields,
            "review_windows": review_windows,
            "ifs_fields": ifs_fields,
            "missing_fields": self._json_safe(missing_fields),
            "template": template_payload,
            "output": {
                "review_root": str(review_root),
                "images_dir": str(composite_path.parent),
                "composite_image_path": str(composite_path),
                "plot_log_path": str(log_path),
                "payload_path": str(payload_path),
                "grid_def": {
                    "projection": settings.grid_definition.projection,
                    "extent": [LON_MIN, LON_MAX, LAT_MIN, LAT_MAX],
                    "shape": [NY, NX],
                    "lon_step": DLON,
                    "lat_step": DLAT,
                },
            },
            "plot_task": {
                "review_id": review_id,
                "plot_status": "pending",
                "created_by": user_id,
                "total_panels": len(template.panels),
            },
        }

    def _tp_file_path(self, case_id: str, lead: int) -> Path:
        primary = (
            self.path_builder.data_source_root
            / case_id
            / "tp"
            / (f"{case_id[2:]}.{lead:03d}")
        )
        if primary.exists():
            return primary
        return self.path_builder.tp_file_path(case_id, lead)

    def _find_ifs_field_path(
        self, *, case_id: str, variable_name: str, lead_hour: int
    ) -> Path | None:
        candidates = [
            self.path_builder.data_source_root
            / case_id
            / "ifs"
            / variable_name
            / f"{case_id[2:]}.{lead_hour:03d}.npz",
            self.path_builder.data_source_root
            / case_id
            / "ifs"
            / f"{variable_name}_{lead_hour:03d}.npz",
            self.path_builder.data_source_root
            / case_id
            / f"{variable_name}_{lead_hour:03d}.npz",
        ]
        for path in candidates:
            if path.exists():
                return path
        for path in (self.path_builder.data_source_root / case_id).rglob(
            f"*{variable_name}*{lead_hour:03d}*.npz"
        ):
            if path.is_file():
                return path
        return None

    def _expected_ifs_path(
        self, case_id: str, variable_name: str, lead_hour: int
    ) -> str:
        return str(
            self.path_builder.data_source_root
            / case_id
            / "ifs"
            / variable_name
            / f"{case_id[2:]}.{lead_hour:03d}.npz"
        )

    def _policy_for_template(self, template: ReviewTemplate) -> list[str]:
        raw_policy = template.review_time_policy
        if isinstance(raw_policy, str):
            return [raw_policy]
        return ["middle"]

    def _valid_time(self, window: ProductWindow, lead_hour: int) -> Any:
        init_time = getattr(window, "init_time", None)
        if init_time is None:
            return None
        if init_time.tzinfo is None:
            init_time = init_time.replace(tzinfo=UTC)
        return init_time + timedelta(hours=lead_hour)

    def _unit_for_field(self, field_name: str) -> str | None:
        if "qpf" in field_name:
            return "mm"
        if "ptype" in field_name:
            return "code"
        return None

    def _relative_field_paths(
        self, edit_fields: dict[str, str], review_root: Path
    ) -> dict[str, str]:
        return {
            field_name: self._relative_path(Path(path), review_root)
            for field_name, path in edit_fields.items()
        }

    def _relative_path(self, path: Path, review_root: Path) -> str:
        if not path.is_absolute():
            return str(path)
        try:
            return str(path.relative_to(review_root))
        except ValueError:
            return os.path.relpath(path, review_root)

    def _json_safe(self, value: Any) -> Any:
        if isinstance(value, list):
            return [self._json_safe(item) for item in value]
        if isinstance(value, dict):
            return {key: self._json_safe(item) for key, item in value.items()}
        if hasattr(value, "isoformat"):
            return value.isoformat()
        return value
