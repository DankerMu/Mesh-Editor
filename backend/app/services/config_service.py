from __future__ import annotations

import json
from datetime import UTC, datetime
from pathlib import Path
from typing import Any
from uuid import uuid4

from sqlalchemy.ext.asyncio import AsyncSession

from app.core.constants import REPO_ROOT
from app.core.error_registry import get_error
from app.core.errors import DomainError
from app.db.models import AuditLog, ConfigSnapshot
from app.repositories.config_snapshot_repo import (
    ConfigSnapshotRepository,
    config_snapshot_repo,
)


CONFIG_FILE_MAP: dict[str, Path] = {
    "product_config": Path("schemas/product_config.json"),
    "plot_config": Path("schemas/plot_config.json"),
}


def _domain_error(
    code: str, detail: dict[str, Any] | None = None, message: str | None = None
) -> DomainError:
    default_message, http_status = get_error(code)
    return DomainError(
        code=code,
        message=message or default_message,
        detail=detail or {},
        http_status=http_status,
    )


class ConfigService:
    def __init__(
        self, snapshots: ConfigSnapshotRepository | None = None, repo_root: Path = REPO_ROOT
    ) -> None:
        self.snapshots = snapshots or config_snapshot_repo
        self.repo_root = repo_root

    def _config_path(self, config_type: str) -> Path:
        relative_path = CONFIG_FILE_MAP.get(config_type)
        if relative_path is None:
            raise _domain_error("CONFIG_NOT_FOUND", {"config_type": config_type})
        return self.repo_root / relative_path

    def get_config(self, config_type: str) -> dict[str, Any]:
        path = self._config_path(config_type)
        if not path.exists():
            raise _domain_error(
                "CONFIG_NOT_FOUND",
                {"config_type": config_type},
            )
        try:
            payload = json.loads(path.read_text(encoding="utf-8"))
        except json.JSONDecodeError as exc:
            raise _domain_error(
                "CONFIG_VALIDATION_FAILED",
                {"config_type": config_type},
                "配置文件不是合法 JSON",
            ) from exc
        if not isinstance(payload, dict):
            raise _domain_error(
                "CONFIG_VALIDATION_FAILED",
                {"config_type": config_type},
                "配置内容必须是 JSON 对象",
            )
        return payload

    async def update_config(
        self,
        db: AsyncSession,
        config_type: str,
        config_json: dict[str, Any],
        user_id: int,
        username: str,
        ip_address: str | None,
    ) -> ConfigSnapshot:
        if not isinstance(config_json, dict) or not config_json:
            raise _domain_error(
                "CONFIG_VALIDATION_FAILED",
                {"config_type": config_type},
                "配置内容必须是有效的非空 JSON 对象",
            )

        path = self._config_path(config_type)
        payload = json.dumps(config_json, ensure_ascii=False, indent=2, sort_keys=True)

        snapshot_id = str(uuid4())
        created_at = datetime.now(UTC).replace(tzinfo=None)
        try:
            snapshot = await self.snapshots.create(
                db,
                snapshot_id=snapshot_id,
                config_type=config_type,
                config_json=payload,
                changed_by=username,
                created_at=created_at,
            )
            db.add(
                AuditLog(
                    user_id=user_id,
                    username=username,
                    action="config_change",
                    resource_type="config",
                    resource_id=config_type,
                    detail_json=json.dumps(
                        {"snapshot_id": snapshot_id, "config_type": config_type},
                        ensure_ascii=False,
                    ),
                    ip_address=ip_address,
                    created_at=created_at,
                )
            )
            await db.flush()
            await db.commit()
        except Exception:
            await db.rollback()
            raise

        try:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(payload + "\n", encoding="utf-8")
        except OSError as exc:
            raise _domain_error(
                "FILE_WRITE_FAILED",
                {"config_type": config_type},
            ) from exc

        await db.refresh(snapshot)
        return snapshot


config_service = ConfigService()
