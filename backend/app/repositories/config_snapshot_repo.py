from __future__ import annotations

from datetime import datetime

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.models import ConfigSnapshot


class ConfigSnapshotRepository:
    async def create(
        self,
        db: AsyncSession,
        snapshot_id: str,
        config_type: str,
        config_json: str,
        changed_by: str | None,
        created_at: datetime,
    ) -> ConfigSnapshot:
        snapshot = ConfigSnapshot(
            snapshot_id=snapshot_id,
            config_type=config_type,
            config_json=config_json,
            changed_by=changed_by,
            created_at=created_at,
        )
        db.add(snapshot)
        await db.flush()
        await db.refresh(snapshot)
        return snapshot

    async def list_by_type(
        self, db: AsyncSession, config_type: str, limit: int = 20
    ) -> list[ConfigSnapshot]:
        result = await db.execute(
            select(ConfigSnapshot)
            .where(ConfigSnapshot.config_type == config_type)
            .order_by(ConfigSnapshot.created_at.desc(), ConfigSnapshot.snapshot_id.desc())
            .limit(limit)
        )
        return list(result.scalars().all())

    async def get_latest(
        self, db: AsyncSession, config_type: str
    ) -> ConfigSnapshot | None:
        result = await db.execute(
            select(ConfigSnapshot)
            .where(ConfigSnapshot.config_type == config_type)
            .order_by(ConfigSnapshot.created_at.desc(), ConfigSnapshot.snapshot_id.desc())
            .limit(1)
        )
        return result.scalar_one_or_none()


config_snapshot_repo = ConfigSnapshotRepository()
