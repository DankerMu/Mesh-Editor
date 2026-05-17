from __future__ import annotations

from typing import Any

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.models import ReviewApproval


class ReviewApprovalRepository:
    async def create(self, db: AsyncSession, **kwargs: Any) -> ReviewApproval:
        approval = ReviewApproval(**kwargs)
        db.add(approval)
        await db.flush()
        await db.refresh(approval)
        return approval

    async def list_by_version_id(
        self, db: AsyncSession, version_id: str
    ) -> list[ReviewApproval]:
        result = await db.execute(
            select(ReviewApproval)
            .where(ReviewApproval.version_id == version_id)
            .order_by(ReviewApproval.reviewed_at.desc())
        )
        return list(result.scalars().all())


review_approval_repo = ReviewApprovalRepository()
