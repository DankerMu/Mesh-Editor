from __future__ import annotations

from datetime import UTC, datetime
from typing import Any
from uuid import uuid4

from sqlalchemy.ext.asyncio import AsyncSession

from app.core.error_registry import get_error
from app.core.errors import DomainError
from app.repositories.edit_version_repo import EditVersionRepository, edit_version_repo
from app.repositories.review_approval_repo import (
    ReviewApprovalRepository,
    review_approval_repo,
)


def _domain_error(code: str, detail: dict[str, Any] | None = None) -> DomainError:
    message, http_status = get_error(code)
    return DomainError(
        code=code,
        message=message,
        detail=detail or {},
        http_status=http_status,
    )


class ApprovalService:
    def __init__(
        self,
        versions: EditVersionRepository | None = None,
        approvals: ReviewApprovalRepository | None = None,
    ) -> None:
        self.versions = versions or edit_version_repo
        self.approvals = approvals or review_approval_repo

    async def review(
        self,
        db: AsyncSession,
        version_id: str,
        reviewer_id: str,
        action: str,
        comment: str | None,
    ) -> dict[str, Any]:
        version = await self.versions.get(db, version_id)
        if version is None:
            raise _domain_error("VERSION_NOT_FOUND", {"version_id": version_id})
        if version.status != "submitted":
            raise _domain_error(
                "VERSION_STATUS_CONFLICT",
                {"version_id": version_id, "status": str(version.status)},
            )
        if action not in {"approve", "reject"}:
            raise _domain_error(
                "VALIDATION_ERROR",
                {"action": action, "valid_actions": ["approve", "reject"]},
            )
        if action == "reject" and not (comment or "").strip():
            raise _domain_error(
                "VALIDATION_ERROR",
                {"field": "comment", "reason": "reject requires non-empty comment"},
            )

        next_status = "approved" if action == "approve" else "rejected"
        await self.versions.update_status(db, version_id, next_status)
        reviewed_at = datetime.now(UTC).replace(tzinfo=None)
        approval = await self.approvals.create(
            db,
            approval_id=str(uuid4()),
            version_id=version_id,
            reviewer_id=reviewer_id,
            action=action,
            comment=comment,
            reviewed_at=reviewed_at,
        )
        return {
            "approval_id": str(approval.approval_id),
            "version_id": str(approval.version_id),
            "reviewer_id": str(approval.reviewer_id),
            "action": str(approval.action),
            "comment": approval.comment,
            "reviewed_at": approval.reviewed_at.isoformat(),
            "version_status": next_status,
        }


approval_service = ApprovalService()
