from __future__ import annotations

from typing import Any

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.models import EditSession


class SessionRepository:
    async def create(self, db: AsyncSession, **kwargs: Any) -> EditSession:
        session = EditSession(**kwargs)
        db.add(session)
        await db.flush()
        await db.refresh(session)
        return session

    async def get_by_id(self, db: AsyncSession, session_id: str) -> EditSession | None:
        return await db.get(EditSession, session_id)

    async def get_active_by_window(
        self, db: AsyncSession, window_id: str
    ) -> EditSession | None:
        result = await db.execute(
            select(EditSession).where(
                EditSession.window_id == window_id,
                EditSession.status == "editing",
            )
        )
        return result.scalars().first()

    async def mark_expired(self, db: AsyncSession, session_id: str) -> None:
        session = await self.get_by_id(db, session_id)
        if session is None:
            return
        session.status = "expired"  # type: ignore[assignment]
        db.add(session)
        await db.flush()


session_repo = SessionRepository()
