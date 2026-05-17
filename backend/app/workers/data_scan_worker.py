from __future__ import annotations

import logging
from datetime import UTC, datetime
from typing import Any

from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker

from app.core.errors import DomainError
from app.db.session import async_session_factory
from app.repositories.data_scan_log_repo import data_scan_log_repo
from app.services.data_scan_service import scan_case

logger = logging.getLogger(__name__)


async def run_data_scan(
    case_id: str,
    session_factory: async_sessionmaker[AsyncSession] = async_session_factory,
    scan_id: str | None = None,
) -> None:
    try:
        await scan_case(case_id, session_factory, scan_id)
    except DomainError as exc:
        if exc.code == "SCAN_ALREADY_RUNNING":
            logger.info("扫描已在运行中，跳过 case_id=%s", case_id)
            return
        logger.exception("数据扫描任务失败 case_id=%s", case_id)
        await _mark_latest_running_failed(case_id, session_factory)
    except Exception:
        logger.exception("数据扫描任务失败 case_id=%s", case_id)
        await _mark_latest_running_failed(case_id, session_factory)


async def _mark_latest_running_failed(
    case_id: str, session_factory: async_sessionmaker[AsyncSession]
) -> None:
    async with session_factory() as db:
        latest = await data_scan_log_repo.get_latest_by_case_id(db, case_id)
        if latest is None or latest.status != "running":
            return
        await data_scan_log_repo.update_finished(
            db,
            str(latest.scan_id),
            "failed",
            datetime.now(UTC),
            int(latest.tp_files_found or 0),
            int(latest.ptype_files_found or 0),
            int(latest.windows_created or 0),
            int(latest.windows_updated or 0),
            [_exception_marker()],
        )
        await db.commit()


def _exception_marker() -> dict[str, Any]:
    return {
        "code": "BACKGROUND_TASK_EXCEPTION",
        "message": "后台扫描任务异常，详见服务日志",
    }
