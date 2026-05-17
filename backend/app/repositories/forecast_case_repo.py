from __future__ import annotations

from datetime import UTC, datetime
from pathlib import Path

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.models import ForecastCase


class ForecastCaseRepository:
    async def create_or_update(
        self,
        db: AsyncSession,
        case_id: str,
        init_time: datetime,
        data_source_path: str | Path,
    ) -> ForecastCase:
        now = datetime.now(UTC)
        existing = await self.get_by_case_id(db, case_id)
        if existing is not None:
            setattr(existing, "scan_count", int(existing.scan_count or 0) + 1)
            setattr(existing, "last_scan_at", now)
            setattr(existing, "data_source_path", str(data_source_path))
            db.add(existing)
            await db.flush()
            await db.refresh(existing)
            return existing

        forecast_case = ForecastCase(
            case_id=case_id,
            init_time=init_time,
            data_source_path=str(data_source_path),
            scan_count=1,
            last_scan_at=now,
            status="pending",
        )
        db.add(forecast_case)
        await db.flush()
        await db.refresh(forecast_case)
        return forecast_case

    async def get_by_case_id(
        self, db: AsyncSession, case_id: str
    ) -> ForecastCase | None:
        result = await db.execute(
            select(ForecastCase).where(ForecastCase.case_id == case_id)
        )
        return result.scalar_one_or_none()


forecast_case_repo = ForecastCaseRepository()
