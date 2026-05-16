from pathlib import Path

from app.storage.path_builder import PathBuilder


def test_path_builder_eight_methods_return_paths(tmp_path: Path) -> None:
    builder = PathBuilder(base_dir=tmp_path)

    paths = [
        builder.case_root("20240101_rain"),
        builder.window_root("window_001"),
        builder.original_field("window_001", "qpf.txt"),
        builder.session_root("session_001"),
        builder.preview_file("session_001", "preview_001"),
        builder.version_root("window_001", "v001"),
        builder.review_root("window_001", "review_001"),
        builder.release_root("window_001", "v001"),
    ]

    assert all(isinstance(path, Path) for path in paths)
    assert "20240101_rain" in builder.case_root("20240101_rain").parts
    assert "window_001" in builder.window_root("window_001").parts
    assert builder.preview_file("session_001", "preview_001").name == "preview_001.npz"
