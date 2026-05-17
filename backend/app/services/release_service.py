from __future__ import annotations

import csv
import json
import shutil
from datetime import UTC, datetime
from pathlib import Path
from typing import Any
from uuid import uuid4

import numpy as np
from numpy.typing import NDArray
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.constants import DLAT, DLON, LAT_MAX, LAT_MIN, LON_MIN, NX, NY, PROJECTION
from app.core.error_registry import get_error
from app.core.errors import DomainError
from app.db.models import EditVersion, ProductWindow
from app.repositories.edit_version_repo import EditVersionRepository, edit_version_repo
from app.repositories.release_product_repo import (
    ReleaseProductRepository,
    release_product_repo,
)
from app.storage.path_builder import PathBuilder, path_builder as default_path_builder


def _domain_error(code: str, detail: dict[str, Any] | None = None) -> DomainError:
    message, http_status = get_error(code)
    return DomainError(
        code=code,
        message=message,
        detail=detail or {},
        http_status=http_status,
    )


def npz_to_txt(npz_path: Path, output_path: Path, dtype: str) -> None:
    with np.load(npz_path) as payload:
        data = payload["data"]
    output_path.parent.mkdir(parents=True, exist_ok=True)
    if dtype == "float32":
        np.savetxt(output_path, data, fmt="%.2f", delimiter=",")
    else:
        np.savetxt(output_path, data, fmt="%d", delimiter=",")


def generate_ptype_transition_csv(
    ptype_before: NDArray[Any],
    ptype_after: NDArray[Any],
    changed_mask: NDArray[Any],
    output_path: Path,
) -> None:
    output_path.parent.mkdir(parents=True, exist_ok=True)
    active = changed_mask.astype(bool)
    with output_path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle)
        writer.writerow(["from_ptype", "to_ptype", "grid_count"])
        for from_ptype in range(4):
            for to_ptype in range(4):
                count = int(
                    np.sum(
                        active
                        & (ptype_before.astype(np.uint8) == from_ptype)
                        & (ptype_after.astype(np.uint8) == to_ptype)
                    )
                )
                writer.writerow([from_ptype, to_ptype, count])


def generate_product_manifest(
    version: EditVersion,
    window: ProductWindow,
    release_id: str,
    released_by: str,
    released_at: datetime,
    images: dict[str, str | None],
) -> dict[str, Any]:
    return {
        "release_id": release_id,
        "window_id": str(version.window_id),
        "version_id": str(version.version_id),
        "case_id": str(window.case_id),
        "accum_hours": int(window.accum_hours),
        "start_lead": int(window.start_lead),
        "end_lead": int(window.end_lead),
        "released_by": released_by,
        "released_at": released_at.replace(tzinfo=UTC).isoformat(),
        "fields": {
            "qpf_after_npz": "fields/qpf_after.npz",
            "ptype_after_npz": "fields/ptype_after.npz",
            "qpf_after_txt": "fields/qpf_after.txt",
            "ptype_after_txt": "fields/ptype_after.txt",
        },
        "derived": {
            "delta_qpf": "derived/delta_qpf.npz",
            "change_ptype": "derived/change_ptype.npz",
            "touched_mask": "derived/touched_mask.npz",
            "changed_mask": "derived/changed_mask.npz",
            "version_ptype_transition": "derived/version_ptype_transition.csv",
        },
        "images": images,
        "review": {},
        "grid": {
            "rows": NY,
            "cols": NX,
            "projection": PROJECTION,
            "lon_start": LON_MIN,
            "lat_start": LAT_MIN,
            "lat_end": LAT_MAX,
            "dlon": DLON,
            "dlat": DLAT,
            "row_order": "y_desc_x_asc",
        },
        "config_snapshot_id": None,
    }


class ReleaseService:
    def __init__(
        self,
        versions: EditVersionRepository | None = None,
        releases: ReleaseProductRepository | None = None,
        path_builder: PathBuilder | None = None,
    ) -> None:
        self.versions = versions or edit_version_repo
        self.releases = releases or release_product_repo
        self.path_builder = path_builder or default_path_builder

    async def release(
        self, db: AsyncSession, version_id: str, released_by: str
    ) -> dict[str, Any]:
        version = await self.versions.get(db, version_id)
        if version is None:
            raise _domain_error("VERSION_NOT_FOUND", {"version_id": version_id})
        if version.status != "approved":
            raise _domain_error(
                "VERSION_STATUS_CONFLICT",
                {"version_id": version_id, "status": str(version.status)},
            )

        window = await db.get(ProductWindow, str(version.window_id))
        if window is None:
            raise _domain_error("WINDOW_NOT_FOUND", {"window_id": str(version.window_id)})

        window_id = str(version.window_id)
        release_id = str(uuid4())
        released_at = datetime.now(UTC).replace(tzinfo=None)
        temp_dir = self.path_builder.base_dir / "tmp" / f"release_{release_id}"
        final_dir = self.path_builder.release_root(window_id, version_id)
        renamed = False

        try:
            if final_dir.exists():
                raise _domain_error(
                    "RELEASE_CONFLICT",
                    {"version_id": version_id, "release_path": str(final_dir)},
                )
            temp_dir.mkdir(parents=True, exist_ok=False)

            copied_images = self._build_release_dir(version, temp_dir)
            manifest = generate_product_manifest(
                version=version,
                window=window,
                release_id=release_id,
                released_by=released_by,
                released_at=released_at,
                images=copied_images,
            )
            manifest_path = temp_dir / "product_manifest.json"
            manifest_path.write_text(
                json.dumps(manifest, ensure_ascii=False, indent=2),
                encoding="utf-8",
            )

            final_dir.parent.mkdir(parents=True, exist_ok=True)
            temp_dir.rename(final_dir)
            renamed = True

            active = await self.releases.get_active_by_window(db, window_id)
            if active is not None:
                await self.releases.update_status(
                    db,
                    str(active.release_id),
                    "superseded",
                    superseded_at=released_at,
                )
                await self.versions.update_status(
                    db, str(active.version_id), "superseded"
                )

            release = await self.releases.create(
                db,
                release_id=release_id,
                version_id=version_id,
                window_id=window_id,
                release_status="active",
                product_path=str(final_dir),
                manifest_path=str(final_dir / "product_manifest.json"),
                released_by=released_by,
                released_at=released_at,
            )
            await self.versions.update_status(db, version_id, "released")
        except DomainError:
            if renamed and final_dir.exists():
                shutil.rmtree(final_dir, ignore_errors=True)
            else:
                shutil.rmtree(temp_dir, ignore_errors=True)
            raise
        except Exception as exc:
            if renamed and final_dir.exists():
                shutil.rmtree(final_dir, ignore_errors=True)
            else:
                shutil.rmtree(temp_dir, ignore_errors=True)
            raise _domain_error(
                "RELEASE_CONFLICT",
                {"version_id": version_id, "release_path": str(final_dir)},
            ) from exc

        return {
            "release_id": str(release.release_id),
            "version_id": version_id,
            "window_id": window_id,
            "release_status": str(release.release_status),
            "product_path": str(release.product_path),
            "manifest_path": str(release.manifest_path),
            "released_by": str(release.released_by),
            "released_at": release.released_at.isoformat(),
        }

    def _build_release_dir(
        self, version: EditVersion, temp_dir: Path
    ) -> dict[str, str | None]:
        fields_dir = temp_dir / "fields"
        derived_dir = temp_dir / "derived"
        images_dir = temp_dir / "images"
        fields_dir.mkdir(parents=True)
        derived_dir.mkdir(parents=True)
        images_dir.mkdir(parents=True)

        qpf_after = Path(str(version.qpf_after_path))
        ptype_after = Path(str(version.ptype_after_path))
        delta_qpf = Path(str(version.delta_qpf_path))
        change_ptype = Path(str(version.change_ptype_path))
        touched_mask = Path(str(version.touched_mask_path))
        changed_mask = Path(str(version.changed_mask_path))

        shutil.copy2(qpf_after, fields_dir / "qpf_after.npz")
        shutil.copy2(ptype_after, fields_dir / "ptype_after.npz")
        shutil.copy2(delta_qpf, derived_dir / "delta_qpf.npz")
        shutil.copy2(change_ptype, derived_dir / "change_ptype.npz")
        shutil.copy2(touched_mask, derived_dir / "touched_mask.npz")
        shutil.copy2(changed_mask, derived_dir / "changed_mask.npz")

        npz_to_txt(fields_dir / "qpf_after.npz", fields_dir / "qpf_after.txt", "float32")
        npz_to_txt(
            fields_dir / "ptype_after.npz", fields_dir / "ptype_after.txt", "uint8"
        )

        with np.load(ptype_after) as payload:
            ptype_after_array = payload["data"]
        with np.load(change_ptype) as payload:
            change_ptype_array = payload["data"]
        ptype_before_array = (
            ptype_after_array.astype(np.int16) - change_ptype_array.astype(np.int16)
        ).astype(np.uint8)
        with np.load(changed_mask) as payload:
            changed_mask_array = payload["data"]
        generate_ptype_transition_csv(
            ptype_before_array,
            ptype_after_array,
            changed_mask_array,
            derived_dir / "version_ptype_transition.csv",
        )

        images: dict[str, str | None] = {
            "before_product": None,
            "after_product": None,
            "delta_qpf": None,
            "change_ptype": None,
            "touched_mask": None,
            "changed_mask": None,
        }
        for attr, key, name in [
            ("before_image_path", "before_product", "before_product.png"),
            ("after_image_path", "after_product", "after_product.png"),
            ("delta_qpf_image_path", "delta_qpf", "delta_qpf.png"),
            ("change_ptype_image_path", "change_ptype", "change_ptype.png"),
            ("touched_mask_image_path", "touched_mask", "touched_mask.png"),
            ("changed_mask_image_path", "changed_mask", "changed_mask.png"),
        ]:
            source_value = getattr(version, attr)
            if source_value and Path(str(source_value)).exists():
                shutil.copy2(Path(str(source_value)), images_dir / name)
                images[key] = f"images/{name}"
        return images


release_service = ReleaseService()
