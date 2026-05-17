from __future__ import annotations

import json
import logging
import shutil
from pathlib import Path
from typing import Any, Callable

import numpy as np
from numpy.typing import NDArray
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import settings
from app.core.constants import NX, NY
from app.core.error_registry import get_error
from app.core.errors import DomainError
from app.db.models import EditOperation
from app.engines.consistency_check import check_qpf_ptype_consistency
from app.engines.version_fields import (
    compute_change_ptype,
    compute_changed_mask,
    compute_delta_qpf,
    compute_touched_mask,
)
from app.plotters.version_plotter import (
    plot_change_ptype,
    plot_delta_qpf,
    plot_mask,
    plot_precip_product,
)
from app.repositories.edit_operation_repo import (
    EditOperationRepository,
    edit_operation_repo,
)
from app.repositories.edit_version_repo import EditVersionRepository, edit_version_repo
from app.repositories.session_repository import SessionRepository, session_repo
from app.storage.path_builder import PathBuilder, path_builder as default_path_builder

logger = logging.getLogger(__name__)

GRID_SHAPE = (NY, NX)
SessionFields = dict[str, NDArray[Any]]
PlotFunction = Callable[..., bytes]


def _domain_error(code: str, detail: dict[str, Any] | None = None) -> DomainError:
    message, http_status = get_error(code)
    return DomainError(
        code=code,
        message=message,
        detail=detail or {},
        http_status=http_status,
    )


class VersionService:
    def __init__(
        self,
        versions: EditVersionRepository | None = None,
        sessions: SessionRepository | None = None,
        operations: EditOperationRepository | None = None,
        path_builder: PathBuilder | None = None,
        precip_plotter: PlotFunction = plot_precip_product,
        delta_qpf_plotter: PlotFunction = plot_delta_qpf,
        change_ptype_plotter: PlotFunction = plot_change_ptype,
        mask_plotter: PlotFunction = plot_mask,
    ) -> None:
        self.versions = versions or edit_version_repo
        self.sessions = sessions or session_repo
        self.operations = operations or edit_operation_repo
        self.path_builder = path_builder or default_path_builder
        self.precip_plotter = precip_plotter
        self.delta_qpf_plotter = delta_qpf_plotter
        self.change_ptype_plotter = change_ptype_plotter
        self.mask_plotter = mask_plotter

    async def save_version(
        self, db: AsyncSession, session_id: str, generate_review: bool = True
    ) -> dict[str, Any]:
        session = await self.sessions.get_by_id(db, session_id)
        if session is None:
            raise _domain_error("SESSION_NOT_FOUND", {"session_id": session_id})
        if session.status != "editing":
            raise _domain_error(
                "SESSION_STATUS_INVALID",
                {"session_id": session_id, "status": str(session.status)},
            )

        window_id = str(session.window_id)
        latest = await self.versions.get_latest_for_window(db, window_id)
        latest_version_id = None if latest is None else str(latest.version_id)
        base_version_id = (
            None if session.base_version_id is None else str(session.base_version_id)
        )
        if base_version_id != latest_version_id:
            raise _domain_error(
                "VERSION_BASE_OUTDATED",
                {
                    "session_id": session_id,
                    "base_version_id": base_version_id,
                    "latest_version_id": latest_version_id,
                },
            )

        fields = self._load_session_fields(session_id)
        qpf_before = fields["qpf_before"].astype(np.float32, copy=False)
        qpf_after = fields["qpf_after"].astype(np.float32, copy=False)
        ptype_before = fields["ptype_before"].astype(np.uint8, copy=False)
        ptype_after = fields["ptype_after"].astype(np.uint8, copy=False)
        touched_mask_session = fields["touched_mask"].astype(np.uint8, copy=False)

        consistency_violation_count = check_qpf_ptype_consistency(
            qpf_after,
            ptype_after,
            threshold_mm=float(settings.product.ptype_qpf_threshold_mm),
        )
        delta_qpf = compute_delta_qpf(qpf_before, qpf_after)
        change_ptype = compute_change_ptype(ptype_before, ptype_after)
        touched_mask = compute_touched_mask(touched_mask_session).astype(
            np.uint8, copy=False
        )
        changed_mask = compute_changed_mask(
            qpf_before,
            qpf_after,
            ptype_before,
            ptype_after,
            epsilon=float(settings.product_config.get("qpf_change_epsilon", 0.001)),
        )

        version_no = await self.versions.get_max_version_no(db, window_id) + 1
        version_id = f"{window_id}_v{version_no:03d}"
        version_dir = self.path_builder.version_root(window_id, version_id)
        if version_dir.exists():
            raise _domain_error(
                "VERSION_STATUS_CONFLICT",
                {"version_id": version_id, "path": str(version_dir)},
            )

        paths: dict[str, Path] = {
            "qpf_after_path": version_dir / "qpf_after.npz",
            "ptype_after_path": version_dir / "ptype_after.npz",
            "delta_qpf_path": version_dir / "delta_qpf.npz",
            "change_ptype_path": version_dir / "change_ptype.npz",
            "touched_mask_path": version_dir / "touched_mask.npz",
            "changed_mask_path": version_dir / "changed_mask.npz",
        }
        image_paths: dict[str, Path | None] = {
            "before_image_path": None,
            "after_image_path": None,
            "delta_qpf_image_path": None,
            "change_ptype_image_path": None,
            "touched_mask_image_path": None,
            "changed_mask_image_path": None,
            "review_image_path": None,
        }

        try:
            version_dir.mkdir(parents=True, exist_ok=False)
            np.savez_compressed(paths["qpf_after_path"], data=qpf_after)
            np.savez_compressed(paths["ptype_after_path"], data=ptype_after)
            np.savez_compressed(paths["delta_qpf_path"], data=delta_qpf)
            np.savez_compressed(paths["change_ptype_path"], data=change_ptype)
            np.savez_compressed(paths["touched_mask_path"], data=touched_mask)
            np.savez_compressed(paths["changed_mask_path"], data=changed_mask)

            operations = await self.operations.query_by_session(
                db, session_id, include_undone=False
            )
            self._write_operations_jsonl(version_dir / "operations.jsonl", operations)

            try:
                image_paths.update(
                    self._generate_images(
                        window_id=window_id,
                        version_id=version_id,
                        qpf_before=qpf_before,
                        ptype_before=ptype_before,
                        qpf_after=qpf_after,
                        ptype_after=ptype_after,
                        delta_qpf=delta_qpf,
                        change_ptype=change_ptype,
                        touched_mask=touched_mask,
                        changed_mask=changed_mask,
                        generate_review=generate_review,
                    )
                )
            except Exception:
                logger.warning("version image generation failed", exc_info=True)

            version = await self.versions.create(
                db,
                version_id=version_id,
                window_id=window_id,
                version_no=version_no,
                base_version_id=base_version_id,
                session_id=session_id,
                status="draft",
                created_by=str(session.user_id) if session.user_id is not None else None,
                **{key: str(path) for key, path in paths.items()},
                **{
                    key: None if path is None else str(path)
                    for key, path in image_paths.items()
                },
            )
            session.status = "saved"  # type: ignore[assignment]
            if hasattr(session, "saved_version_id"):
                session.saved_version_id = version_id  # type: ignore[attr-defined]
            db.add(session)
            await db.flush()
        except DomainError:
            shutil.rmtree(version_dir, ignore_errors=True)
            raise
        except Exception as exc:
            shutil.rmtree(version_dir, ignore_errors=True)
            raise _domain_error(
                "VERSION_SAVE_FAILED",
                {"session_id": session_id, "version_id": version_id},
            ) from exc

        return {
            "session_id": session_id,
            "version_id": str(version.version_id),
            "version_no": int(version.version_no),
            "status": str(version.status),
            "consistency_violation_count": consistency_violation_count,
            "warnings": (
                [
                    {
                        "code": "CONSISTENCY_VIOLATION",
                        "count": consistency_violation_count,
                    }
                ]
                if consistency_violation_count > 0
                else []
            ),
            "before_image": version.before_image_path,
            "after_image": version.after_image_path,
            "review_image": version.review_image_path,
        }

    async def submit(self, db: AsyncSession, version_id: str) -> dict[str, Any]:
        version = await self.versions.get(db, version_id)
        if version is None:
            raise _domain_error("VERSION_NOT_FOUND", {"version_id": version_id})
        if version.status != "draft":
            raise _domain_error(
                "VERSION_STATUS_CONFLICT",
                {"version_id": version_id, "status": str(version.status)},
            )

        await self.versions.update_status(db, version_id, "submitted")
        await db.refresh(version)
        return {"version_id": version_id, "status": str(version.status)}

    def _load_session_fields(self, session_id: str) -> SessionFields:
        session_dir = self.path_builder.session_root(session_id)
        return {
            "qpf_before": self._load_session_array(session_dir, "qpf_before", np.float32),
            "qpf_after": self._load_session_array(session_dir, "qpf_after", np.float32),
            "ptype_before": self._load_session_array(
                session_dir, "ptype_before", np.uint8
            ),
            "ptype_after": self._load_session_array(session_dir, "ptype_after", np.uint8),
            "touched_mask": self._load_session_array(
                session_dir, "touched_mask", np.uint8
            ),
        }

    def _load_session_array(
        self, session_dir: Path, field_name: str, dtype: type[np.generic]
    ) -> NDArray[Any]:
        npz_path = session_dir / f"{field_name}.npz"
        npy_path = session_dir / f"{field_name}.npy"
        if npz_path.exists():
            with np.load(npz_path) as payload:
                array = payload["data"] if "data" in payload else payload[payload.files[0]]
        elif npy_path.exists():
            array = np.load(npy_path)
        else:
            raise _domain_error(
                "FIELD_NOT_AVAILABLE",
                {"field_name": field_name, "session_dir": str(session_dir)},
            )

        if array.shape != GRID_SHAPE:
            raise _domain_error(
                "GRID_SHAPE_MISMATCH",
                {
                    "field_name": field_name,
                    "expected": list(GRID_SHAPE),
                    "actual": list(array.shape),
                },
            )
        return array.astype(dtype, copy=False)

    def _write_operations_jsonl(
        self, output_path: Path, operations: list[EditOperation]
    ) -> None:
        with output_path.open("w", encoding="utf-8") as handle:
            for operation in operations:
                handle.write(
                    json.dumps(
                        {
                            "operation_id": operation.operation_id,
                            "session_id": operation.session_id,
                            "window_id": operation.window_id,
                            "sequence_no": operation.sequence_no,
                            "tool_name": operation.tool_name,
                            "variable_name": operation.variable_name,
                            "operation_type": operation.operation_type,
                            "parameters_json": operation.parameters_json,
                            "mask_geometry_json": operation.mask_geometry_json,
                            "mask_raster_path": operation.mask_raster_path,
                            "before_stats_json": operation.before_stats_json,
                            "after_stats_json": operation.after_stats_json,
                            "op_ptype_transition_json": operation.op_ptype_transition_json,
                            "is_undone": operation.is_undone,
                            "created_at": (
                                operation.created_at.isoformat()
                                if operation.created_at is not None
                                else None
                            ),
                        },
                        ensure_ascii=False,
                    )
                    + "\n"
                )

    def _generate_images(
        self,
        *,
        window_id: str,
        version_id: str,
        qpf_before: NDArray[np.float32],
        ptype_before: NDArray[np.uint8],
        qpf_after: NDArray[np.float32],
        ptype_after: NDArray[np.uint8],
        delta_qpf: NDArray[np.float32],
        change_ptype: NDArray[np.int8],
        touched_mask: NDArray[np.uint8],
        changed_mask: NDArray[np.uint8],
        generate_review: bool,
    ) -> dict[str, Path | None]:
        images_dir = self.path_builder.version_images_dir(window_id, version_id)
        images_dir.mkdir(parents=True, exist_ok=True)
        outputs: dict[str, Path | None] = {
            "before_image_path": images_dir / "before_product.png",
            "after_image_path": images_dir / "after_product.png",
            "delta_qpf_image_path": images_dir / "delta_qpf.png",
            "change_ptype_image_path": images_dir / "change_ptype.png",
            "touched_mask_image_path": images_dir / "touched_mask.png",
            "changed_mask_image_path": images_dir / "changed_mask.png",
            "review_image_path": None,
        }
        (outputs["before_image_path"]).write_bytes(  # type: ignore[union-attr]
            self.precip_plotter(qpf_before, ptype_before)
        )
        (outputs["after_image_path"]).write_bytes(  # type: ignore[union-attr]
            self.precip_plotter(qpf_after, ptype_after)
        )
        (outputs["delta_qpf_image_path"]).write_bytes(  # type: ignore[union-attr]
            self.delta_qpf_plotter(delta_qpf)
        )
        (outputs["change_ptype_image_path"]).write_bytes(  # type: ignore[union-attr]
            self.change_ptype_plotter(change_ptype)
        )
        (outputs["touched_mask_image_path"]).write_bytes(  # type: ignore[union-attr]
            self.mask_plotter(touched_mask, title="触达格点")
        )
        (outputs["changed_mask_image_path"]).write_bytes(  # type: ignore[union-attr]
            self.mask_plotter(changed_mask, title="变化格点")
        )
        if generate_review:
            outputs["review_image_path"] = None
        return outputs


version_service = VersionService()
