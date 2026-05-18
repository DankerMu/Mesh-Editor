import re
from pathlib import Path

from app.core.config import settings

_SAFE_SEGMENT_RE = re.compile(r"^[A-Za-z0-9][A-Za-z0-9._-]*$")


def _safe_segment(value: str) -> str:
    if not value or not _SAFE_SEGMENT_RE.match(value) or ".." in value:
        raise ValueError(f"非法路径片段: {value!r}")
    return value


class PathBuilder:
    TP_SUBDIR = "deeplearning"
    PTYPE_SUBDIR = Path("statistics") / "ptype_deeplearning"

    def __init__(
        self, base_dir: Path | None = None, data_source_root: Path | None = None
    ) -> None:
        self.base_dir = (
            Path(base_dir) if base_dir is not None else settings.storage.base_dir
        )
        self.data_source_root = (
            Path(data_source_root)
            if data_source_root is not None
            else settings.data_source_root
        )

    def case_root(self, case_id: str) -> Path:
        return self.base_dir / "cases" / _safe_segment(case_id)

    def window_root(self, window_id: str) -> Path:
        return self.base_dir / "windows" / _safe_segment(window_id)

    def original_field(self, window_id: str, name: str) -> Path:
        return self.window_root(window_id) / "original" / _safe_segment(name)

    def session_root(self, session_id: str) -> Path:
        return self.base_dir / "sessions" / _safe_segment(session_id)

    def preview_file(self, session_id: str, preview_id: str) -> Path:
        return (
            self.session_root(session_id)
            / "previews"
            / f"{_safe_segment(preview_id)}.npz"
        )

    def version_root(self, window_id: str, version_id: str) -> Path:
        return self.window_root(window_id) / "versions" / _safe_segment(version_id)

    def version_images_dir(self, window_id: str, version_id: str) -> Path:
        return self.version_root(window_id, version_id) / "images"

    def review_root(self, window_id: str, review_id: str) -> Path:
        return self.window_root(window_id) / "reviews" / _safe_segment(review_id)

    def review_payload_path(self, window_id: str, review_id: str) -> Path:
        return self.review_root(window_id, review_id) / "review_payload.json"

    def review_images_dir(self, window_id: str, review_id: str) -> Path:
        return self.review_root(window_id, review_id) / "images"

    def review_log_path(self, window_id: str, review_id: str) -> Path:
        return self.review_root(window_id, review_id) / "plot_log.txt"

    def release_root(self, window_id: str, version_id: str) -> Path:
        return self.window_root(window_id) / "releases" / _safe_segment(version_id)

    def release_temp_dir(self, window_id: str, version_id: str) -> Path:
        return self.base_dir / "tmp" / f"release_{_safe_segment(version_id)}"

    def _case_date(self, case_id: str) -> str:
        return _safe_segment(case_id[:8])

    def tp_source_dir(self, case_id: str) -> Path:
        return self.data_source_root / self.TP_SUBDIR / self._case_date(case_id)

    def ptype_source_dir(self, case_id: str) -> Path:
        return self.data_source_root / self.PTYPE_SUBDIR / self._case_date(case_id)

    def window_original_dir(self, case_id: str, window_id: str) -> Path:
        return (
            self.case_root(case_id) / "windows" / _safe_segment(window_id) / "original"
        )

    def tp_file_path(self, case_id: str, lead: int) -> Path:
        return self.tp_source_dir(case_id) / f"tp_999_deeplearning_{_safe_segment(case_id)}_{lead:03d}.txt"

    def ptype_file_path(self, case_id: str, lead: int) -> Path:
        return self.ptype_source_dir(case_id) / f"ptype_999_revised_{_safe_segment(case_id)}_{lead:03d}.txt"


path_builder = PathBuilder()
