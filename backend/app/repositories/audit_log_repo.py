from __future__ import annotations

from datetime import datetime
from typing import Any

from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.sql import Select

from app.db.models import AuditLog


class AuditLogRepository:
    async def list_by_filters(
        self,
        db: AsyncSession,
        user_id: int | None = None,
        action: str | None = None,
        resource_type: str | None = None,
        start_date: datetime | None = None,
        end_date: datetime | None = None,
        page: int = 1,
        page_size: int = 20,
    ) -> list[AuditLog]:
        query = self._apply_filters(
            select(AuditLog),
            user_id=user_id,
            action=action,
            resource_type=resource_type,
            start_date=start_date,
            end_date=end_date,
        )
        query = (
            query.order_by(AuditLog.created_at.desc(), AuditLog.id.desc())
            .offset((page - 1) * page_size)
            .limit(page_size)
        )
        result = await db.execute(query)
        return list(result.scalars().all())

    async def count_by_filters(
        self,
        db: AsyncSession,
        user_id: int | None = None,
        action: str | None = None,
        resource_type: str | None = None,
        start_date: datetime | None = None,
        end_date: datetime | None = None,
    ) -> int:
        query = self._apply_filters(
            select(func.count(AuditLog.id)),
            user_id=user_id,
            action=action,
            resource_type=resource_type,
            start_date=start_date,
            end_date=end_date,
        )
        result = await db.execute(query)
        return int(result.scalar_one())

    def _apply_filters(
        self,
        query: Select[Any],
        *,
        user_id: int | None,
        action: str | None,
        resource_type: str | None,
        start_date: datetime | None,
        end_date: datetime | None,
    ) -> Select[Any]:
        if user_id is not None:
            query = query.where(AuditLog.user_id == user_id)
        if action is not None:
            query = query.where(AuditLog.action == action)
        if resource_type is not None:
            query = query.where(AuditLog.resource_type == resource_type)
        if start_date is not None:
            query = query.where(AuditLog.created_at >= start_date)
        if end_date is not None:
            query = query.where(AuditLog.created_at < end_date)
        return query


audit_log_repo = AuditLogRepository()
