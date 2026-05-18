from __future__ import annotations

from datetime import UTC, datetime, timedelta
from typing import Any

from sqlalchemy import func, select, update
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.models import EditVersion, ProductWindow, ReviewProduct

TERMINAL_PLOT_STATUSES = {
    "success",
    "partial_success",
    "permanently_failed",
    "superseded",
}


class ReviewProductRepository:
    async def create(self, db: AsyncSession, **kwargs: Any) -> ReviewProduct:
        product = ReviewProduct(**kwargs)
        db.add(product)
        await db.flush()
        await db.refresh(product)
        return product

    async def get_by_id(
        self, db: AsyncSession, review_id: str
    ) -> ReviewProduct | None:
        return await db.get(ReviewProduct, review_id)

    async def list_by_case(
        self, db: AsyncSession, case_id: str
    ) -> list[ReviewProduct]:
        result = await db.execute(
            select(ReviewProduct)
            .join(ProductWindow, ReviewProduct.window_id == ProductWindow.window_id)
            .where(ProductWindow.case_id == case_id)
            .order_by(ReviewProduct.created_at.desc())
        )
        return list(result.scalars().all())

    async def list_by_window(
        self, db: AsyncSession, window_id: str
    ) -> list[ReviewProduct]:
        result = await db.execute(
            select(ReviewProduct)
            .where(ReviewProduct.window_id == window_id)
            .order_by(ReviewProduct.created_at.desc())
        )
        return list(result.scalars().all())

    async def list_by_window_with_versions(
        self, db: AsyncSession, window_id: str
    ) -> list[ReviewProduct]:
        result = await db.execute(
            select(
                ReviewProduct,
                EditVersion.version_no,
                EditVersion.status,
                EditVersion.created_by,
                EditVersion.created_at,
            )
            .join(EditVersion, ReviewProduct.version_id == EditVersion.version_id)
            .where(ReviewProduct.window_id == window_id)
            .order_by(ReviewProduct.created_at.desc())
        )
        products: list[ReviewProduct] = []
        for product, version_no, version_status, created_by, version_created_at in (
            result.all()
        ):
            product.version_no = version_no
            product.version_status = version_status
            product.version_created_by = created_by
            product.version_created_at = version_created_at
            products.append(product)
        return products

    async def list_all(
        self,
        db: AsyncSession,
        case_id: str | None = None,
        window_id: str | None = None,
        plot_status: str | None = None,
    ) -> list[ReviewProduct]:
        statement = select(ReviewProduct)
        if case_id is not None:
            statement = statement.join(
                ProductWindow, ReviewProduct.window_id == ProductWindow.window_id
            ).where(ProductWindow.case_id == case_id)
        if window_id is not None:
            statement = statement.where(ReviewProduct.window_id == window_id)
        if plot_status is not None:
            statement = statement.where(ReviewProduct.plot_status == plot_status)
        result = await db.execute(statement.order_by(ReviewProduct.created_at.desc()))
        return list(result.scalars().all())

    async def update_status(
        self,
        db: AsyncSession,
        review_id: str,
        status: str,
        **extra: Any,
    ) -> ReviewProduct | None:
        product = await self.get_by_id(db, review_id)
        if product is None:
            return None
        product.plot_status = status  # type: ignore[assignment]
        for key, value in extra.items():
            setattr(product, key, value)
        db.add(product)
        await db.flush()
        await db.refresh(product)
        return product

    async def claim_task(
        self, db: AsyncSession, worker_id: str, max_concurrent: int = 2
    ) -> ReviewProduct | None:
        if await self.count_running(db) >= max_concurrent:
            return None

        result = await db.execute(
            select(ReviewProduct)
            .where(
                ReviewProduct.plot_status == "pending",
                ReviewProduct.locked_by.is_(None),
            )
            .order_by(ReviewProduct.created_at)
            .limit(1)
        )
        product = result.scalars().first()
        if product is None:
            return None

        product.plot_status = "running"  # type: ignore[assignment]
        product.locked_by = worker_id  # type: ignore[assignment]
        product.locked_at = datetime.now(UTC)  # type: ignore[assignment]
        product.attempt = int(product.attempt or 0) + 1  # type: ignore[assignment]
        db.add(product)
        await db.flush()
        await db.refresh(product)
        return product

    async def count_running(self, db: AsyncSession) -> int:
        result = await db.execute(
            select(func.count()).select_from(ReviewProduct).where(
                ReviewProduct.plot_status == "running"
            )
        )
        return int(result.scalar_one())

    async def list_stale_tasks(
        self, db: AsyncSession, timeout_seconds: int = 300
    ) -> list[ReviewProduct]:
        cutoff = datetime.now(UTC) - timedelta(seconds=timeout_seconds)
        result = await db.execute(
            select(ReviewProduct)
            .where(
                ReviewProduct.plot_status == "running",
                ReviewProduct.locked_at < cutoff,
            )
            .order_by(ReviewProduct.locked_at)
        )
        return list(result.scalars().all())

    async def supersede_existing(
        self,
        db: AsyncSession,
        window_id: str,
        version_id: str,
        template_id: str,
    ) -> int:
        result = await db.execute(
            update(ReviewProduct)
            .where(
                ReviewProduct.window_id == window_id,
                ReviewProduct.version_id == version_id,
                ReviewProduct.template_id == template_id,
                ReviewProduct.plot_status.not_in(TERMINAL_PLOT_STATUSES),
            )
            .values(plot_status="superseded")
        )
        await db.flush()
        return int(result.rowcount or 0)


review_product_repo = ReviewProductRepository()
