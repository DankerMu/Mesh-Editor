from pathlib import Path

import pytest

from app.storage.path_builder import PathBuilder


def test_path_builder_eight_methods_return_paths(tmp_path: Path) -> None:
    data_source_root = tmp_path / "source"
    builder = PathBuilder(base_dir=tmp_path, data_source_root=data_source_root)

    paths = [
        builder.case_root("20240101_rain"),
        builder.window_root("window_001"),
        builder.original_field("window_001", "qpf.txt"),
        builder.session_root("session_001"),
        builder.preview_file("session_001", "preview_001"),
        builder.version_root("window_001", "v001"),
        builder.review_root("window_001", "review_001"),
        builder.release_root("window_001", "v001"),
        builder.data_source_dir("2024051608"),
        builder.window_original_dir("2024051608", "2024051608_W024_000_024"),
        builder.tp_file_path("2024051608", 24),
        builder.ptype_file_path("2024051608", 24),
    ]

    assert all(isinstance(path, Path) for path in paths)
    assert "20240101_rain" in builder.case_root("20240101_rain").parts
    assert "window_001" in builder.window_root("window_001").parts
    assert builder.preview_file("session_001", "preview_001").name == "preview_001.npz"
    assert builder.data_source_dir("2024051608") == data_source_root / "2024051608"
    assert builder.window_original_dir("2024051608", "2024051608_W024_000_024") == (
        tmp_path
        / "cases"
        / "2024051608"
        / "windows"
        / "2024051608_W024_000_024"
        / "original"
    )
    assert (
        builder.tp_file_path("2024051608", 24)
        == data_source_root / "2024051608" / "tp_024.txt"
    )
    assert (
        builder.ptype_file_path("2024051608", 24)
        == data_source_root / "2024051608" / "ptype_024.txt"
    )


@pytest.mark.parametrize(
    "bad_id", ["../../etc/passwd", "../escape", "", "/abs", "a/b", "has\x00null"]
)
def test_path_builder_rejects_traversal_payloads(tmp_path: Path, bad_id: str) -> None:
    builder = PathBuilder(base_dir=tmp_path)
    with pytest.raises(ValueError, match="非法路径片段"):
        builder.case_root(bad_id)


def test_path_builder_rejects_bad_data_source_case_id(tmp_path: Path) -> None:
    builder = PathBuilder(base_dir=tmp_path, data_source_root=tmp_path / "source")

    with pytest.raises(ValueError, match="非法路径片段"):
        builder.data_source_dir("../2024051608")
