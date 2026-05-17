from __future__ import annotations

from typing import Any

from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.models import EditVersion


class EditVersionRepository:
    async def create(self, db: AsyncSession, **kwargs: Any) -> EditVersion:
        version = EditVersion(**kwargs)
        db.add(version)
        await db.flush()
        await db.refresh(version)
        return version

    async def get(self, db: AsyncSession, version_id: str) -> EditVersion | None:
        return await db.get(EditVersion, version_id)

    async def list(
        self,
        db: AsyncSession,
        window_id: str | None = None,
        status: str | None = None,
        created_by: str | None = None,
    ) -> list[EditVersion]:
        statement = select(EditVersion)
        if window_id is not None:
            statement = statement.where(EditVersion.window_id == window_id)
        if status is not None:
            statement = statement.where(EditVersion.status == status)
        if created_by is not None:
            statement = statement.where(EditVersion.created_by == created_by)
        statement = statement.order_by(EditVersion.version_no.desc())
        result = await db.execute(statement)
        return list(result.scalars().all())

    async def get_latest_for_window(
        self, db: AsyncSession, window_id: str
    ) -> EditVersion | None:
        result = await db.execute(
            select(EditVersion)
            .where(EditVersion.window_id == window_id)
            .order_by(EditVersion.version_no.desc())
            .limit(1)
        )
        return result.scalars().first()

    async def update_status(
        self, db: AsyncSession, version_id: str, status: str
    ) -> None:
        version = await self.get(db, version_id)
        if version is None:
            return
        version.status = status  # type: ignore[assignment]
        db.add(version)
        await db.flush()

    async def get_max_version_no(self, db: AsyncSession, window_id: str) -> int:
        result = await db.execute(
            select(func.max(EditVersion.version_no)).where(
                EditVersion.window_id == window_id
            )
        )
        return int(result.scalar_one() or 0)


edit_version_repo = EditVersionRepository()
