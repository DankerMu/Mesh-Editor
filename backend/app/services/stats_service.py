from __future__ import annotations

import csv
import io
import json
import zipfile
from dataclasses import dataclass
from datetime import UTC, date, datetime, time, timedelta, timezone
from typing import Any

from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.models import (
    EditOperation,
    EditSession,
    EditVersion,
    ProductWindow,
    ReleaseProduct,
)
from app.schemas.m6 import (
    OperationStatsResponse,
    PtypeTransitionStatsResponse,
    TopTransitionItem,
)

BEIJING_TZ = timezone(timedelta(hours=8))
PTYPE_LABELS = {0: "none", 1: "rain", 2: "sleet", 3: "snow"}
MATRIX_KEYS = [f"{old}->{new}" for old in range(4) for new in range(4)]


@dataclass(frozen=True)
class StatsExportResult:
    content: bytes
    media_type: str
    filename: str


class StatsService:
    async def get_operation_stats(
        self,
        db: AsyncSession,
        start_date: date,
        end_date: date,
        user_id: int | None = None,
        window_id: str | None = None,
        accum_hours: int | None = None,
    ) -> OperationStatsResponse:
        start_dt, end_dt = self._date_bounds(start_date, end_date)
        filters = self._operation_filters(start_dt, end_dt, user_id, window_id, accum_hours)

        totals = await db.execute(
            select(
                func.count(EditOperation.operation_id),
                func.count(func.distinct(EditSession.session_id)),
            )
            .select_from(EditOperation)
            .join(EditSession, EditOperation.session_id == EditSession.session_id)
            .join(ProductWindow, EditSession.window_id == ProductWindow.window_id)
            .where(*filters)
        )
        total_operations, total_sessions = totals.one()

        by_tool = await self._count_grouped(
            db,
            EditOperation.tool_name,
            filters,
        )
        by_operation = await self._count_grouped(
            db,
            EditOperation.operation_type,
            filters,
        )
        by_accum_hours = await self._accum_stats(
            db,
            start_dt,
            end_dt,
            user_id,
            window_id,
            accum_hours,
        )
        total_versions_saved = sum(item["versions"] for item in by_accum_hours.values())
        total_versions_released = await self._version_count(
            db,
            start_dt,
            end_dt,
            user_id=user_id,
            window_id=window_id,
            accum_hours=accum_hours,
            released_only=True,
        )

        return OperationStatsResponse(
            period=self._period(start_date, end_date),
            total_sessions=int(total_sessions or 0),
            total_operations=int(total_operations or 0),
            total_versions_saved=total_versions_saved,
            total_versions_released=total_versions_released,
            by_accum_hours=by_accum_hours,
            by_tool=by_tool,
            by_operation=by_operation,
        )

    async def get_ptype_transition_stats(
        self,
        db: AsyncSession,
        start_date: date,
        end_date: date,
        user_id: int | None = None,
        window_id: str | None = None,
    ) -> PtypeTransitionStatsResponse:
        start_dt, end_dt = self._date_bounds(start_date, end_date)
        filters = [
            EditOperation.created_at >= start_dt,
            EditOperation.created_at < end_dt,
            EditOperation.op_ptype_transition_json.is_not(None),
        ]
        if user_id is not None:
            filters.append(EditSession.user_id == user_id)
        if window_id is not None:
            filters.append(EditSession.window_id == window_id)

        result = await db.execute(
            select(EditOperation.op_ptype_transition_json)
            .select_from(EditOperation)
            .join(EditSession, EditOperation.session_id == EditSession.session_id)
            .where(*filters)
        )
        matrix = self._zero_matrix()
        total = 0
        for value in result.scalars().all():
            parsed = self._parse_transition(value)
            if parsed:
                total += 1
                self._merge_matrix(matrix, parsed)

        return PtypeTransitionStatsResponse(
            period=self._period(start_date, end_date),
            total_operations_with_transitions=total,
            matrix=matrix,
            top_transitions=self._top_transitions(matrix),
        )

    async def export_stats_csv(
        self,
        db: AsyncSession,
        start_date: date,
        end_date: date,
        include: list[str],
    ) -> StatsExportResult:
        csv_files: dict[str, str] = {}
        if "operations" in include:
            csv_files["operations"] = await self._operations_csv(db, start_date, end_date)
        if "ptype_transitions" in include:
            csv_files["ptype_transitions"] = await self._ptype_transitions_csv(
                db, start_date, end_date
            )
        if "version_summary" in include:
            csv_files["version_summary"] = await self._version_summary_csv(
                db, start_date, end_date
            )

        start_token = start_date.strftime("%Y%m%d")
        end_token = end_date.strftime("%Y%m%d")
        if len(csv_files) == 1:
            name, content = next(iter(csv_files.items()))
            return StatsExportResult(
                content=content.encode("utf-8-sig"),
                media_type="text/csv",
                filename=f"{name}_{start_token}_{end_token}.csv",
            )

        buffer = io.BytesIO()
        with zipfile.ZipFile(buffer, mode="w", compression=zipfile.ZIP_DEFLATED) as zf:
            for name, content in csv_files.items():
                zf.writestr(f"{name}.csv", content.encode("utf-8-sig"))
        return StatsExportResult(
            content=buffer.getvalue(),
            media_type="application/zip",
            filename=f"stats_export_{start_token}_{end_token}.zip",
        )

    async def _count_grouped(
        self,
        db: AsyncSession,
        column: Any,
        filters: list[Any],
    ) -> dict[str, int]:
        result = await db.execute(
            select(column, func.count(EditOperation.operation_id))
            .select_from(EditOperation)
            .join(EditSession, EditOperation.session_id == EditSession.session_id)
            .join(ProductWindow, EditSession.window_id == ProductWindow.window_id)
            .where(*filters)
            .group_by(column)
        )
        return {str(key): int(count) for key, count in result.all()}

    async def _accum_stats(
        self,
        db: AsyncSession,
        start_dt: datetime,
        end_dt: datetime,
        user_id: int | None,
        window_id: str | None,
        accum_hours: int | None,
    ) -> dict[str, dict[str, int]]:
        session_filters = self._operation_filters(
            start_dt, end_dt, user_id, window_id, accum_hours
        )
        session_rows = await db.execute(
            select(
                ProductWindow.accum_hours,
                func.count(func.distinct(EditSession.session_id)),
            )
            .select_from(EditOperation)
            .join(EditSession, EditOperation.session_id == EditSession.session_id)
            .join(ProductWindow, EditSession.window_id == ProductWindow.window_id)
            .where(*session_filters)
            .group_by(ProductWindow.accum_hours)
        )
        stats = {
            str(accum): {"sessions": int(count), "versions": 0}
            for accum, count in session_rows.all()
        }

        version_filters = self._version_filters(
            start_dt,
            end_dt,
            user_id=user_id,
            window_id=window_id,
            accum_hours=accum_hours,
            released_only=False,
        )
        version_rows = await db.execute(
            select(ProductWindow.accum_hours, func.count(EditVersion.version_id))
            .select_from(EditVersion)
            .join(ProductWindow, EditVersion.window_id == ProductWindow.window_id)
            .join(EditSession, EditVersion.session_id == EditSession.session_id, isouter=True)
            .where(*version_filters)
            .group_by(ProductWindow.accum_hours)
        )
        for accum, count in version_rows.all():
            key = str(accum)
            stats.setdefault(key, {"sessions": 0, "versions": 0})
            stats[key]["versions"] = int(count)
        return stats

    async def _version_count(
        self,
        db: AsyncSession,
        start_dt: datetime,
        end_dt: datetime,
        user_id: int | None,
        window_id: str | None,
        accum_hours: int | None,
        released_only: bool,
    ) -> int:
        filters = self._version_filters(
            start_dt,
            end_dt,
            user_id=user_id,
            window_id=window_id,
            accum_hours=accum_hours,
            released_only=released_only,
        )
        result = await db.execute(
            select(func.count(EditVersion.version_id))
            .select_from(EditVersion)
            .join(ProductWindow, EditVersion.window_id == ProductWindow.window_id)
            .join(EditSession, EditVersion.session_id == EditSession.session_id, isouter=True)
            .where(*filters)
        )
        return int(result.scalar_one() or 0)

    async def _operations_csv(
        self, db: AsyncSession, start_date: date, end_date: date
    ) -> str:
        start_dt, end_dt = self._date_bounds(start_date, end_date)
        result = await db.execute(
            select(EditOperation, EditSession.user_id, ProductWindow.case_id, ProductWindow.accum_hours)
            .select_from(EditOperation)
            .join(EditSession, EditOperation.session_id == EditSession.session_id)
            .join(ProductWindow, EditSession.window_id == ProductWindow.window_id)
            .where(EditOperation.created_at >= start_dt, EditOperation.created_at < end_dt)
            .order_by(EditOperation.created_at, EditOperation.sequence_no)
        )
        rows: list[list[Any]] = []
        for operation, user_id, case_id, accum_hours in result.all():
            rows.append(
                [
                    case_id,
                    operation.window_id,
                    accum_hours,
                    operation.session_id,
                    user_id,
                    operation.operation_id,
                    operation.sequence_no,
                    operation.tool_name,
                    operation.variable_name,
                    operation.operation_type,
                    self._affected_count(operation.after_stats_json),
                    self._format_beijing(operation.created_at),  # type: ignore[arg-type]
                ]
            )
        return self._write_csv(
            [
                "case_id",
                "window_id",
                "accum_hours",
                "session_id",
                "user_id",
                "operation_id",
                "sequence_no",
                "tool_name",
                "variable_name",
                "operation_type",
                "affected_count",
                "created_at",
            ],
            rows,
        )

    async def _ptype_transitions_csv(
        self, db: AsyncSession, start_date: date, end_date: date
    ) -> str:
        start_dt, end_dt = self._date_bounds(start_date, end_date)
        result = await db.execute(
            select(
                EditVersion,
                ProductWindow.case_id,
                EditOperation.op_ptype_transition_json,
            )
            .select_from(EditVersion)
            .join(ProductWindow, EditVersion.window_id == ProductWindow.window_id)
            .join(EditOperation, EditVersion.session_id == EditOperation.session_id)
            .where(
                EditVersion.created_at >= start_dt,
                EditVersion.created_at < end_dt,
                EditOperation.op_ptype_transition_json.is_not(None),
            )
            .order_by(EditVersion.created_at, EditVersion.version_no)
        )
        by_version: dict[str, tuple[EditVersion, str, dict[str, int]]] = {}
        for version, case_id, transition_json in result.all():
            entry = by_version.setdefault(
                str(version.version_id),
                (version, str(case_id), self._zero_matrix()),
            )
            self._merge_matrix(entry[2], self._parse_transition(transition_json))

        rows: list[list[Any]] = []
        for version, case_id, matrix in by_version.values():
            for key in MATRIX_KEYS:
                count = matrix[key]
                if count == 0:
                    continue
                old, new = key.split("->", maxsplit=1)
                rows.append(
                    [
                        case_id,
                        version.window_id,
                        version.version_id,
                        version.version_no,
                        old,
                        new,
                        count,
                    ]
                )
        return self._write_csv(
            [
                "case_id",
                "window_id",
                "version_id",
                "version_no",
                "from_ptype",
                "to_ptype",
                "count",
            ],
            rows,
        )

    async def _version_summary_csv(
        self, db: AsyncSession, start_date: date, end_date: date
    ) -> str:
        start_dt, end_dt = self._date_bounds(start_date, end_date)
        result = await db.execute(
            select(
                EditVersion,
                ProductWindow.case_id,
                ReleaseProduct.released_at,
                func.count(EditOperation.operation_id),
            )
            .select_from(EditVersion)
            .join(ProductWindow, EditVersion.window_id == ProductWindow.window_id)
            .join(ReleaseProduct, EditVersion.version_id == ReleaseProduct.version_id, isouter=True)
            .join(EditOperation, EditVersion.session_id == EditOperation.session_id, isouter=True)
            .where(EditVersion.created_at >= start_dt, EditVersion.created_at < end_dt)
            .group_by(EditVersion.version_id, ProductWindow.case_id, ReleaseProduct.released_at)
            .order_by(EditVersion.created_at, EditVersion.version_no)
        )
        rows = [
            [
                case_id,
                version.window_id,
                version.version_id,
                version.version_no,
                version.status,
                int(operation_count or 0),
                version.created_by,
                self._format_beijing(version.created_at),  # type: ignore[arg-type]
                self._format_beijing(released_at),
            ]
            for version, case_id, released_at, operation_count in result.all()
        ]
        return self._write_csv(
            [
                "case_id",
                "window_id",
                "version_id",
                "version_no",
                "status",
                "operation_count",
                "created_by",
                "created_at",
                "released_at",
            ],
            rows,
        )

    def _operation_filters(
        self,
        start_dt: datetime,
        end_dt: datetime,
        user_id: int | None,
        window_id: str | None,
        accum_hours: int | None,
    ) -> list[Any]:
        filters: list[Any] = [
            EditOperation.created_at >= start_dt,
            EditOperation.created_at < end_dt,
        ]
        if user_id is not None:
            filters.append(EditSession.user_id == user_id)
        if window_id is not None:
            filters.append(EditSession.window_id == window_id)
        if accum_hours is not None:
            filters.append(ProductWindow.accum_hours == accum_hours)
        return filters

    def _version_filters(
        self,
        start_dt: datetime,
        end_dt: datetime,
        user_id: int | None,
        window_id: str | None,
        accum_hours: int | None,
        released_only: bool,
    ) -> list[Any]:
        filters: list[Any] = [
            EditVersion.created_at >= start_dt,
            EditVersion.created_at < end_dt,
        ]
        if user_id is not None:
            filters.append(EditSession.user_id == user_id)
        if window_id is not None:
            filters.append(EditVersion.window_id == window_id)
        if accum_hours is not None:
            filters.append(ProductWindow.accum_hours == accum_hours)
        if released_only:
            filters.append(EditVersion.status == "released")
        return filters

    def _date_bounds(self, start_date: date, end_date: date) -> tuple[datetime, datetime]:
        return (
            datetime.combine(start_date, time.min),
            datetime.combine(end_date + timedelta(days=1), time.min),
        )

    def _period(self, start_date: date, end_date: date) -> dict[str, str]:
        return {"start_date": start_date.isoformat(), "end_date": end_date.isoformat()}

    def _zero_matrix(self) -> dict[str, int]:
        return {key: 0 for key in MATRIX_KEYS}

    def _parse_transition(self, value: Any) -> dict[str, int]:
        if value is None:
            return {}
        payload = value
        if isinstance(value, str):
            try:
                payload = json.loads(value)
            except json.JSONDecodeError:
                return {}
        matrix: dict[str, int] = {}
        if isinstance(payload, dict):
            for key, count in payload.items():
                normalized = self._normalize_transition_key(str(key))
                if normalized in MATRIX_KEYS:
                    matrix[normalized] = matrix.get(normalized, 0) + int(count or 0)
            return matrix
        if isinstance(payload, list):
            for old, row in enumerate(payload[:4]):
                if not isinstance(row, list):
                    continue
                for new, count in enumerate(row[:4]):
                    matrix[f"{old}->{new}"] = matrix.get(f"{old}->{new}", 0) + int(
                        count or 0
                    )
        return matrix

    def _normalize_transition_key(self, key: str) -> str:
        if "_to_" in key:
            old, new = key.split("_to_", maxsplit=1)
            return f"{old}->{new}"
        return key

    def _merge_matrix(self, target: dict[str, int], source: dict[str, int]) -> None:
        for key, value in source.items():
            if key in target:
                target[key] += int(value)

    def _top_transitions(self, matrix: dict[str, int]) -> list[TopTransitionItem]:
        rows = sorted(
            ((key, count) for key, count in matrix.items() if count > 0),
            key=lambda item: (-item[1], item[0]),
        )
        return [
            TopTransitionItem(
                transition=key,
                count=count,
                label=self._transition_label(key),
            )
            for key, count in rows[:5]
        ]

    def _transition_label(self, key: str) -> str:
        old, new = (int(part) for part in key.split("->", maxsplit=1))
        return f"{PTYPE_LABELS[old]}->{PTYPE_LABELS[new]}"

    def _affected_count(self, value: Any) -> int:
        if not value:
            return 0
        try:
            parsed = json.loads(str(value))
        except json.JSONDecodeError:
            return 0
        if not isinstance(parsed, dict):
            return 0
        return int(parsed.get("count", 0) or 0)

    def _format_beijing(self, value: datetime | None) -> str:
        if value is None:
            return ""
        dt = value
        if dt.tzinfo is None:
            dt = dt.replace(tzinfo=UTC)
        return dt.astimezone(BEIJING_TZ).strftime("%Y-%m-%d %H:%M:%S")

    def _write_csv(self, header: list[str], rows: list[list[Any]]) -> str:
        buffer = io.StringIO()
        writer = csv.writer(buffer, lineterminator="\n")
        writer.writerow(header)
        writer.writerows(rows)
        return buffer.getvalue()


stats_service = StatsService()
