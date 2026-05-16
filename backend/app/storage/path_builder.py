from pathlib import Path

from app.core.config import settings


class PathBuilder:
    def __init__(self, base_dir: Path | None = None) -> None:
        self.base_dir = Path(base_dir) if base_dir is not None else settings.storage.base_dir

    def case_root(self, case_id: str) -> Path:
        return self.base_dir / "cases" / case_id

    def window_root(self, window_id: str) -> Path:
        return self.base_dir / "windows" / window_id

    def original_field(self, window_id: str, name: str) -> Path:
        return self.window_root(window_id) / "original" / name

    def session_root(self, session_id: str) -> Path:
        return self.base_dir / "sessions" / session_id

    def preview_file(self, session_id: str, preview_id: str) -> Path:
        return self.session_root(session_id) / "previews" / f"{preview_id}.npz"

    def version_root(self, window_id: str, version_id: str) -> Path:
        return self.window_root(window_id) / "versions" / version_id

    def review_root(self, window_id: str, review_id: str) -> Path:
        return self.window_root(window_id) / "reviews" / review_id

    def release_root(self, window_id: str, version_id: str) -> Path:
        return self.window_root(window_id) / "releases" / version_id


path_builder = PathBuilder()
