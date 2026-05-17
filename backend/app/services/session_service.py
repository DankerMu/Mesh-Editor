from __future__ import annotations

import json
from datetime import UTC, datetime, timedelta
from pathlib import Path
from typing import Any
from uuid import uuid4

import numpy as np
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.constants import NX, NY
from app.core.error_registry import get_error
from app.core.errors import DomainError
from app.db.models import AppUser, AuditLog, EditSession, ProductWindow
from app.repositories.product_window_repo import (
    ProductWindowRepository,
    product_window_repo,
)
from app.repositories.session_repository import SessionRepository, session_repo
from app.storage.path_builder import PathBuilder, path_builder as default_path_builder

SESSION_FIELDS = {
    "qpf_before",
    "qpf_after",
    "ptype_before",
    "ptype_after",
    "touched_mask",
    "changed_mask",
    "invalid_mask",
}
WINDOW_FIELDS = {"qpf_before", "ptype_before", "invalid_mask"}
QPF_FIELDS = {"qpf_before", "qpf_after"}
PTYPE_FIELDS = {"ptype_before", "ptype_after"}
MASK_FIELDS = {"touched_mask", "changed_mask", "invalid_mask"}
GRID_SHAPE = (NY, NX)


def _domain_error(
    code: str,
    detail: dict[str, Any] | None = None,
    http_status: int | None = None,
) -> DomainError:
    message, default_status = get_error(code)
    return DomainError(
        code=code,
        message=message,
        detail=detail,
        http_status=http_status if http_status is not None else default_status,
    )


def _field_dtype(field_name: str) -> np.dtype[Any]:
    if field_name in QPF_FIELDS:
        return np.dtype("float32")
    return np.dtype("uint8")


def _field_variable(field_name: str) -> str:
    if field_name.startswith("qpf"):
        return "qpf"
    if field_name.startswith("ptype"):
        return "ptype"
    return field_name


def _field_headers(field_name: str, byte_length: int) -> dict[str, str]:
    return {
        "X-Grid-Rows": str(NY),
        "X-Grid-Cols": str(NX),
        "X-Grid-Dtype": str(_field_dtype(field_name)),
        "X-Grid-Order": "C",
        "X-Grid-Byte-Length": str(byte_length),
        "X-Grid-Variable": _field_variable(field_name),
    }


class EditSessionService:
    def __init__(
        self,
        sessions: SessionRepository | None = None,
        windows: ProductWindowRepository | None = None,
        path_builder: PathBuilder | None = None,
    ) -> None:
        self.sessions = sessions or session_repo
        self.windows = windows or product_window_repo
        self.path_builder = path_builder or default_path_builder

    async def create_session(
        self, db: AsyncSession, window_id: str, user: AppUser
    ) -> EditSession:
        window = await db.get(ProductWindow, window_id)
        if window is None:
            raise _domain_error("WINDOW_NOT_FOUND", {"window_id": window_id})

        if window.status not in {"available", "partial"}:
            raise _domain_error(
                "WINDOW_NOT_EDITABLE",
                {"window_id": window_id, "status": window.status},
            )

        if user.role not in {"admin", "reviewer", "forecaster"}:
            raise _domain_error("PERMISSION_DENIED", {"role": user.role})

        await self.expire_stale_sessions(db)
        existing = await self.sessions.get_active_by_window(db, window_id)
        if existing is not None:
            if int(existing.user_id) == int(user.id):
                return existing
            raise _domain_error(
                "WINDOW_LOCKED",
                {
                    "window_id": window_id,
                    "session_id": existing.session_id,
                    "locked_by": existing.user_id,
                },
            )

        session_id = str(uuid4())
        session = await self.sessions.create(
            db,
            session_id=session_id,
            window_id=window_id,
            user_id=int(user.id),
            base_version_id=None,
            status="editing",
        )
        self._initialize_working_directory(session_id, window_id)
        await self._write_audit_log(db, session, user)
        await db.commit()
        await db.refresh(session)
        return session

    async def load_session(self, db: AsyncSession, session_id: str) -> dict[str, Any]:
        session = await self._get_editing_session(db, session_id)
        return {
            "session_id": session.session_id,
            "window_id": session.window_id,
            "base_version_id": session.base_version_id,
            "status": session.status,
            "grid_rows": NY,
            "grid_cols": NX,
            "operation_count": 0,
            "can_undo": False,
            "can_redo": False,
            "field_urls": {
                field: f"/api/session/{session.session_id}/field/{field}"
                for field in sorted(SESSION_FIELDS)
            },
            "before_image": None,
            "after_image": None,
        }

    async def get_field_data(
        self, db: AsyncSession, session_id: str, field_name: str
    ) -> tuple[bytes, dict[str, str]]:
        self._validate_field(field_name, SESSION_FIELDS)
        await self._get_editing_session(db, session_id)
        return self._read_field_file(
            self.path_builder.session_root(session_id) / f"{field_name}.npy",
            field_name,
        )

    async def get_window_field_data(
        self, db: AsyncSession, window_id: str, field_name: str
    ) -> tuple[bytes, dict[str, str]]:
        self._validate_field(field_name, WINDOW_FIELDS)
        window = await db.get(ProductWindow, window_id)
        if window is None:
            raise _domain_error("WINDOW_NOT_FOUND", {"window_id": window_id})
        if window.status not in {"available", "partial"}:
            raise _domain_error(
                "WINDOW_NOT_EDITABLE",
                {"window_id": window_id, "status": window.status},
            )
        return self._read_field_file(
            self.path_builder.window_root(window_id) / "original" / f"{field_name}.npy",
            field_name,
        )

    async def expire_stale_sessions(
        self, db: AsyncSession, max_age_hours: int = 24
    ) -> int:
        cutoff = datetime.now(UTC).replace(tzinfo=None) - timedelta(hours=max_age_hours)
        result = await db.execute(
            select(EditSession).where(
                EditSession.status == "editing",
                EditSession.updated_at < cutoff,
            )
        )
        sessions = list(result.scalars().all())
        for session in sessions:
            session.status = "expired"
            db.add(session)
        await db.flush()
        return len(sessions)

    async def _get_editing_session(
        self, db: AsyncSession, session_id: str
    ) -> EditSession:
        session = await self.sessions.get_by_id(db, session_id)
        if session is None:
            raise _domain_error("SESSION_NOT_FOUND", {"session_id": session_id})
        if session.status == "expired":
            raise _domain_error(
                "SESSION_EXPIRED", {"session_id": session_id}, http_status=410
            )
        if session.status != "editing":
            raise _domain_error(
                "SESSION_NOT_EDITING",
                {"session_id": session_id, "status": session.status},
            )
        return session

    def _initialize_working_directory(self, session_id: str, window_id: str) -> None:
        session_dir = self.path_builder.session_root(session_id)
        original_dir = self.path_builder.window_root(window_id) / "original"
        session_dir.mkdir(parents=True, exist_ok=True)

        qpf_before = self._load_array(original_dir / "qpf_before.npy", "qpf_before")
        ptype_before = self._load_array(
            original_dir / "ptype_before.npy", "ptype_before"
        )
        invalid_mask_path = original_dir / "invalid_mask.npy"
        if invalid_mask_path.exists():
            invalid_mask = self._load_array(invalid_mask_path, "invalid_mask")
        else:
            invalid_mask = np.zeros(GRID_SHAPE, dtype=np.uint8)

        np.save(
            session_dir / "qpf_before.npy", qpf_before.astype(np.float32, copy=False)
        )
        np.save(
            session_dir / "qpf_after.npy", qpf_before.astype(np.float32, copy=False)
        )
        np.save(
            session_dir / "ptype_before.npy", ptype_before.astype(np.uint8, copy=False)
        )
        np.save(
            session_dir / "ptype_after.npy", ptype_before.astype(np.uint8, copy=False)
        )
        np.save(
            session_dir / "invalid_mask.npy", invalid_mask.astype(np.uint8, copy=False)
        )
        np.save(session_dir / "touched_mask.npy", np.zeros(GRID_SHAPE, dtype=np.uint8))
        np.save(session_dir / "changed_mask.npy", np.zeros(GRID_SHAPE, dtype=np.uint8))

    def _load_array(self, path: Path, field_name: str) -> np.ndarray[Any, Any]:
        if not path.exists():
            raise _domain_error(
                "FIELD_NOT_AVAILABLE",
                {"field_name": field_name, "path": str(path)},
            )
        array = np.load(path)
        if array.shape != GRID_SHAPE:
            raise _domain_error(
                "GRID_SHAPE_MISMATCH",
                {
                    "field_name": field_name,
                    "expected": list(GRID_SHAPE),
                    "actual": list(array.shape),
                },
            )
        return array.astype(_field_dtype(field_name), copy=False)

    def _read_field_file(
        self, path: Path, field_name: str
    ) -> tuple[bytes, dict[str, str]]:
        array = self._load_array(path, field_name)
        data = np.ascontiguousarray(array).tobytes(order="C")
        return data, _field_headers(field_name, len(data))

    def _validate_field(self, field_name: str, valid_fields: set[str]) -> None:
        if field_name not in valid_fields:
            raise _domain_error(
                "VALIDATION_ERROR",
                {
                    "field_name": field_name,
                    "valid_fields": sorted(valid_fields),
                },
            )

    async def _write_audit_log(
        self, db: AsyncSession, session: EditSession, user: AppUser
    ) -> None:
        db.add(
            AuditLog(
                user_id=int(user.id),
                username=str(user.username),
                action="session_start",
                resource_type="session",
                resource_id=str(session.session_id),
                detail_json=json.dumps(
                    {"window_id": session.window_id}, ensure_ascii=False
                ),
            )
        )
        await db.flush()


session_service = EditSessionService()
