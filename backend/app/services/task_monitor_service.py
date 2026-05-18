from __future__ import annotations

import json
from datetime import UTC, datetime
from pathlib import Path
from typing import Any

from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.error_registry import get_error
from app.core.errors import DomainError
from app.db.models import AuditLog, ReviewProduct
from app.schemas.m6 import FailedTaskItem, TaskCountsResponse, TaskSummaryResponse

PLOT_STATUSES = [
    "pending",
    "running",
    "success",
    "partial_success",
    "failed",
    "permanently_failed",
    "superseded",
]
RETRYABLE_STATUSES = {"failed", "permanently_failed"}


def _domain_error(code: str, detail: dict[str, Any] | None = None) -> DomainError:
    message, http_status = get_error(code)
    return DomainError(
        code=code,
        message=message,
        detail=detail or {},
        http_status=http_status,
    )


class TaskMonitorService:
    async def get_task_summary(self, db: AsyncSession) -> TaskSummaryResponse:
        counts = {status: 0 for status in PLOT_STATUSES}
        result = await db.execute(
            select(ReviewProduct.plot_status, func.count())
            .select_from(ReviewProduct)
            .group_by(ReviewProduct.plot_status)
        )
        for status, count in result.all():
            if status in counts:
                counts[str(status)] = int(count)

        failed_result = await db.execute(
            select(ReviewProduct)
            .where(ReviewProduct.plot_status.in_(RETRYABLE_STATUSES))
            .order_by(ReviewProduct.plot_finished_at.desc().nullslast())
            .limit(10)
        )
        recent_failed = [
            FailedTaskItem(
                review_id=str(item.review_id),
                window_id=str(item.window_id),
                plot_status=str(item.plot_status),
                error_summary=self._error_summary(item),
                failed_at=item.plot_finished_at,  # type: ignore[arg-type]
            )
            for item in failed_result.scalars().all()
        ]
        return TaskSummaryResponse(
            counts=TaskCountsResponse(**counts),
            recent_failed=recent_failed,
        )

    async def manual_retry(
        self,
        db: AsyncSession,
        review_id: str,
        user_id: int,
        username: str,
        ip_address: str | None,
    ) -> ReviewProduct:
        product = await db.get(ReviewProduct, review_id)
        if product is None:
            raise _domain_error("REVIEW_NOT_FOUND", {"review_id": review_id})
        previous_status = str(product.plot_status)
        if previous_status not in RETRYABLE_STATUSES:
            raise _domain_error(
                "TASK_NOT_RETRYABLE",
                {"review_id": review_id, "plot_status": previous_status},
            )

        now = datetime.now(UTC).replace(tzinfo=None)
        product.plot_status = "pending"  # type: ignore[assignment]
        product.attempt = 0  # type: ignore[assignment]
        product.locked_by = None  # type: ignore[assignment]
        product.locked_at = None  # type: ignore[assignment]
        product.next_retry_at = None  # type: ignore[assignment]
        product.plot_started_at = None  # type: ignore[assignment]
        product.plot_finished_at = None  # type: ignore[assignment]
        db.add(product)
        db.add(
            AuditLog(
                user_id=user_id,
                username=username,
                action="task_retry",
                resource_type="review_product",
                resource_id=review_id,
                detail_json=json.dumps(
                    {"previous_status": previous_status, "new_status": "pending"},
                    ensure_ascii=False,
                ),
                ip_address=ip_address,
                created_at=now,
            )
        )
        await db.flush()
        await db.commit()
        await db.refresh(product)
        return product

    def _error_summary(self, product: ReviewProduct) -> str | None:
        path_value = product.error_log_path
        if path_value is None:
            return None
        try:
            text = Path(str(path_value)).read_text(encoding="utf-8")
        except OSError:
            text = str(path_value)
        text = text.strip()
        return text[:200] if text else None


task_monitor_service = TaskMonitorService()
