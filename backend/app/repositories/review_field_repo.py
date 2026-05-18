from __future__ import annotations

from typing import Any

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.models import ReviewField


class ReviewFieldRepository:
    async def create(self, db: AsyncSession, **kwargs: Any) -> ReviewField:
        field = ReviewField(**kwargs)
        db.add(field)
        await db.flush()
        await db.refresh(field)
        return field

    async def list_by_window(
        self, db: AsyncSession, window_id: str
    ) -> list[ReviewField]:
        result = await db.execute(
            select(ReviewField)
            .where(ReviewField.window_id == window_id)
            .order_by(ReviewField.created_at)
        )
        return list(result.scalars().all())

    async def list_by_version(
        self, db: AsyncSession, version_id: str
    ) -> list[ReviewField]:
        result = await db.execute(
            select(ReviewField)
            .where(ReviewField.version_id == version_id)
            .order_by(ReviewField.created_at)
        )
        return list(result.scalars().all())


review_field_repo = ReviewFieldRepository()
