from __future__ import annotations

from datetime import datetime
from typing import Any

from sqlalchemy import desc, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.models import DataScanLog


class DataScanLogRepository:
    async def create(
        self,
        db: AsyncSession,
        scan_id: str,
        case_id: str,
        scan_started_at: datetime,
    ) -> DataScanLog:
        scan_log = DataScanLog(
            scan_id=scan_id,
            case_id=case_id,
            status="running",
            scan_started_at=scan_started_at,
        )
        db.add(scan_log)
        await db.flush()
        await db.refresh(scan_log)
        return scan_log

    async def update_finished(
        self,
        db: AsyncSession,
        scan_id: str,
        status: str,
        scan_finished_at: datetime,
        tp_files_found: int,
        ptype_files_found: int,
        windows_created: int,
        windows_updated: int,
        errors_json: list[dict[str, Any]] | dict[str, Any] | None = None,
    ) -> DataScanLog | None:
        scan_log = await self.get_by_scan_id(db, scan_id)
        if scan_log is None:
            return None
        setattr(scan_log, "status", status)
        setattr(scan_log, "scan_finished_at", scan_finished_at)
        setattr(scan_log, "tp_files_found", tp_files_found)
        setattr(scan_log, "ptype_files_found", ptype_files_found)
        setattr(scan_log, "windows_created", windows_created)
        setattr(scan_log, "windows_updated", windows_updated)
        setattr(scan_log, "errors_json", errors_json)
        db.add(scan_log)
        await db.flush()
        await db.refresh(scan_log)
        return scan_log

    async def get_latest_by_case_id(
        self, db: AsyncSession, case_id: str
    ) -> DataScanLog | None:
        result = await db.execute(
            select(DataScanLog)
            .where(DataScanLog.case_id == case_id)
            .order_by(desc(DataScanLog.scan_started_at))
            .limit(1)
        )
        return result.scalar_one_or_none()

    async def get_by_scan_id(
        self, db: AsyncSession, scan_id: str
    ) -> DataScanLog | None:
        result = await db.execute(
            select(DataScanLog).where(DataScanLog.scan_id == scan_id)
        )
        return result.scalar_one_or_none()

    async def has_running_scan(self, db: AsyncSession, case_id: str) -> bool:
        result = await db.execute(
            select(DataScanLog.scan_id)
            .where(DataScanLog.case_id == case_id, DataScanLog.status == "running")
            .limit(1)
        )
        return result.scalar_one_or_none() is not None


data_scan_log_repo = DataScanLogRepository()
