from __future__ import annotations

from datetime import UTC, datetime
from typing import Any

from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.models import ProductWindow


class ProductWindowRepository:
    async def create_or_update(self, db: AsyncSession, **kwargs: Any) -> ProductWindow:
        window_id = str(kwargs["window_id"])
        existing = await db.get(ProductWindow, window_id)
        now = datetime.now(UTC)
        if existing is not None:
            for key, value in kwargs.items():
                setattr(existing, key, value)
            setattr(existing, "updated_at", now)
            db.add(existing)
            await db.flush()
            await db.refresh(existing)
            return existing

        if kwargs.get("data_ready_at") is None and kwargs.get("status") in {
            "available",
            "partial",
        }:
            kwargs["data_ready_at"] = now
        product_window = ProductWindow(**kwargs)
        db.add(product_window)
        await db.flush()
        await db.refresh(product_window)
        return product_window

    async def list_by_case_id(
        self,
        db: AsyncSession,
        case_id: str,
        accum_hours: int | None = None,
        status: str | None = None,
    ) -> list[ProductWindow]:
        statement = select(ProductWindow).where(ProductWindow.case_id == case_id)
        if accum_hours is not None:
            statement = statement.where(ProductWindow.accum_hours == accum_hours)
        if status is not None:
            statement = statement.where(ProductWindow.status == status)
        statement = statement.order_by(
            ProductWindow.accum_hours,
            ProductWindow.start_lead,
            ProductWindow.end_lead,
        )
        result = await db.execute(statement)
        return list(result.scalars().all())

    async def count_by_status(self, db: AsyncSession, case_id: str) -> dict[str, int]:
        result = await db.execute(
            select(ProductWindow.status, func.count())
            .where(ProductWindow.case_id == case_id)
            .group_by(ProductWindow.status)
        )
        return {str(status): int(count) for status, count in result.all()}


product_window_repo = ProductWindowRepository()
