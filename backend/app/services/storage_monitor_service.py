from __future__ import annotations

import os
import shutil
import time
from datetime import datetime, timedelta, timezone
from pathlib import Path

from app.schemas.m6 import StorageBreakdownItem, StorageSummaryResponse
from app.storage.path_builder import PathBuilder, path_builder as default_path_builder

BEIJING_TZ = timezone(timedelta(hours=8))
GB = 1024**3


class StorageMonitorService:
    def __init__(
        self,
        path_builder: PathBuilder | None = None,
        ttl_seconds: int = 60,
    ) -> None:
        self.path_builder = path_builder or default_path_builder
        self.ttl_seconds = ttl_seconds
        self._cache: StorageSummaryResponse | None = None
        self._cache_time = 0.0

    def get_storage_summary(self) -> StorageSummaryResponse:
        now = time.monotonic()
        if self._cache is not None and now - self._cache_time < self.ttl_seconds:
            return self._cache

        base_dir = Path(self.path_builder.base_dir)
        archive_root = base_dir / "archive"
        tmp_root = base_dir / "tmp"
        breakdown = [
            self._breakdown_item("cases", archive_root / "cases"),
            self._breakdown_item("releases", archive_root / "releases"),
            self._breakdown_item("reviews", archive_root / "reviews"),
            self._breakdown_item("tmp", tmp_root),
        ]
        disk_path = archive_root if archive_root.exists() else base_dir
        usage = shutil.disk_usage(disk_path)
        response = StorageSummaryResponse(
            total_bytes=int(usage.total),
            used_bytes=int(usage.used),
            free_bytes=int(usage.free),
            total_gb=self._gb(usage.total),
            used_gb=self._gb(usage.used),
            free_gb=self._gb(usage.free),
            breakdown=breakdown,
            last_scan_at=datetime.now(BEIJING_TZ),
        )
        self._cache = response
        self._cache_time = now
        return response

    def _breakdown_item(self, item_type: str, root: Path) -> StorageBreakdownItem:
        size_bytes, file_count = self._scan(root)
        return StorageBreakdownItem(
            type=item_type,
            size_bytes=size_bytes,
            size_gb=self._gb(size_bytes),
            file_count=file_count,
        )

    def _scan(self, root: Path) -> tuple[int, int]:
        if not root.exists():
            return 0, 0
        total = 0
        count = 0
        for dirpath, _dirnames, filenames in os.walk(root):
            for filename in filenames:
                path = Path(dirpath) / filename
                try:
                    total += path.stat().st_size
                    count += 1
                except OSError:
                    continue
        return total, count

    def _gb(self, value: int) -> float:
        return round(value / GB, 2)


storage_monitor_service = StorageMonitorService()
