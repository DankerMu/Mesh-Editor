from __future__ import annotations

from datetime import datetime
from typing import Any

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.models import ReleaseProduct


class ReleaseProductRepository:
    async def create(self, db: AsyncSession, **kwargs: Any) -> ReleaseProduct:
        release = ReleaseProduct(**kwargs)
        db.add(release)
        await db.flush()
        await db.refresh(release)
        return release

    async def get_active_by_window(
        self, db: AsyncSession, window_id: str
    ) -> ReleaseProduct | None:
        result = await db.execute(
            select(ReleaseProduct).where(
                ReleaseProduct.window_id == window_id,
                ReleaseProduct.release_status == "active",
            )
        )
        return result.scalars().first()

    async def update_status(
        self,
        db: AsyncSession,
        release_id: str,
        status: str,
        superseded_at: datetime | None = None,
    ) -> None:
        release = await db.get(ReleaseProduct, release_id)
        if release is None:
            return
        release.release_status = status  # type: ignore[assignment]
        release.superseded_at = superseded_at  # type: ignore[assignment]
        db.add(release)
        await db.flush()


release_product_repo = ReleaseProductRepository()
