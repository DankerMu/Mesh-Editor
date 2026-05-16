import re
from pathlib import Path

from app.core.config import settings

_SAFE_SEGMENT_RE = re.compile(r"^[A-Za-z0-9][A-Za-z0-9._-]*$")


def _safe_segment(value: str) -> str:
    if not value or not _SAFE_SEGMENT_RE.match(value) or ".." in value:
        raise ValueError(f"非法路径片段: {value!r}")
    return value


class PathBuilder:
    def __init__(self, base_dir: Path | None = None) -> None:
        self.base_dir = Path(base_dir) if base_dir is not None else settings.storage.base_dir

    def case_root(self, case_id: str) -> Path:
        return self.base_dir / "cases" / _safe_segment(case_id)

    def window_root(self, window_id: str) -> Path:
        return self.base_dir / "windows" / _safe_segment(window_id)

    def original_field(self, window_id: str, name: str) -> Path:
        return self.window_root(window_id) / "original" / _safe_segment(name)

    def session_root(self, session_id: str) -> Path:
        return self.base_dir / "sessions" / _safe_segment(session_id)

    def preview_file(self, session_id: str, preview_id: str) -> Path:
        return self.session_root(session_id) / "previews" / f"{_safe_segment(preview_id)}.npz"

    def version_root(self, window_id: str, version_id: str) -> Path:
        return self.window_root(window_id) / "versions" / _safe_segment(version_id)

    def review_root(self, window_id: str, review_id: str) -> Path:
        return self.window_root(window_id) / "reviews" / _safe_segment(review_id)

    def release_root(self, window_id: str, version_id: str) -> Path:
        return self.window_root(window_id) / "releases" / _safe_segment(version_id)


path_builder = PathBuilder()
