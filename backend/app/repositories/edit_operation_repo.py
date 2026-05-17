from __future__ import annotations

from typing import Any

from sqlalchemy import delete, func, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.models import EditOperation


class EditOperationRepository:
    async def create(self, db: AsyncSession, **kwargs: Any) -> EditOperation:
        operation = EditOperation(**kwargs)
        db.add(operation)
        await db.flush()
        await db.refresh(operation)
        return operation

    async def query_by_session(
        self, db: AsyncSession, session_id: str, include_undone: bool = True
    ) -> list[EditOperation]:
        statement = select(EditOperation).where(EditOperation.session_id == session_id)
        if not include_undone:
            statement = statement.where(EditOperation.is_undone == 0)
        statement = statement.order_by(EditOperation.sequence_no)
        result = await db.execute(statement)
        return list(result.scalars().all())

    async def update_is_undone(
        self, db: AsyncSession, operation_id: str, is_undone: int
    ) -> None:
        operation = await db.get(EditOperation, operation_id)
        if operation is None:
            return
        operation.is_undone = is_undone  # type: ignore[assignment]
        db.add(operation)
        await db.flush()

    async def delete_after_sequence(
        self, db: AsyncSession, session_id: str, sequence_no: int
    ) -> int:
        result = await db.execute(
            delete(EditOperation).where(
                EditOperation.session_id == session_id,
                EditOperation.sequence_no > sequence_no,
            )
        )
        await db.flush()
        return int(result.rowcount or 0)

    async def get_max_sequence(self, db: AsyncSession, session_id: str) -> int:
        result = await db.execute(
            select(func.max(EditOperation.sequence_no)).where(
                EditOperation.session_id == session_id
            )
        )
        return int(result.scalar_one() or 0)


edit_operation_repo = EditOperationRepository()
