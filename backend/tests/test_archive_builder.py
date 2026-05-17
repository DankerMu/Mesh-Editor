import json
from pathlib import Path

import numpy as np

from app.engines.ptype_builder import PtypeBuildResult
from app.engines.qpf_builder import QpfBuildResult
from app.storage.archive_builder import atomic_write_npz, save_window_original
from app.storage.path_builder import PathBuilder
from tests.fixtures.conftest import CASE_ID, TEST_SHAPE


def test_atomic_write_npz_cleans_tmp_on_failure(tmp_path: Path, monkeypatch) -> None:
    target = tmp_path / "field.npz"

    def fail_savez(*args, **kwargs) -> None:
        raise RuntimeError("boom")

    monkeypatch.setattr(np, "savez", fail_savez)

    try:
        atomic_write_npz(target, data=np.ones(TEST_SHAPE))
    except RuntimeError:
        pass

    assert not target.exists()
    assert list(tmp_path.glob("*.tmp")) == []


def test_save_window_original_writes_expected_files(tmp_path: Path) -> None:
    builder = PathBuilder(
        base_dir=tmp_path / "archive", data_source_root=tmp_path / "source"
    )
    window_id = f"{CASE_ID}_ACC6_000_006"
    qpf = np.ones(TEST_SHAPE)
    negative_mask = np.zeros(TEST_SHAPE, dtype=bool)
    missing_mask = np.zeros(TEST_SHAPE, dtype=bool)
    ptype = np.ones(TEST_SHAPE, dtype=int)
    invalid_mask = np.zeros(TEST_SHAPE, dtype=bool)
    invalid_mask[0, 0] = True
    qpf_result = QpfBuildResult(
        qpf=qpf,
        qc_status="pass",
        negative_count=0,
        negative_min_value=None,
        negative_abs_max=None,
        missing_count=0,
        missing_mask=missing_mask,
        manifest={"window_id": window_id, "qc_status": "pass"},
        negative_mask=negative_mask,
        start_tp_path=None,
        end_tp_path=builder.tp_file_path(CASE_ID, 6),
    )
    ptype_result = PtypeBuildResult(
        ptype=ptype,
        qc_status="warn",
        ptype_missing_leads=[],
        effective_lead_count=2,
        manifest={"window_id": window_id, "qc_status": "warn"},
        ptype_invalid_mask=invalid_mask,
        tp_missing_leads=[],
        used_leads=[3, 6],
        all_leads=[3, 6],
        step_negative_count=0,
        step_nan_count=0,
    )

    saved = save_window_original(CASE_ID, window_id, qpf_result, ptype_result, builder)

    expected_names = {
        "qpf_before.npz",
        "ptype_before.npz",
        "negative_qpf_mask.npz",
        "negative_mask.npz",
        "missing_mask.npz",
        "qpf_missing_mask.npz",
        "ptype_invalid_mask.npz",
        "qpf_build_manifest.json",
        "ptype_source_manifest.json",
        "v000_delta_qpf.npz",
        "v000_change_ptype.npz",
        "v000_touched_mask.npz",
        "v000_changed_mask.npz",
    }
    saved_names = {path.name for path in saved.values()}
    assert expected_names <= saved_names
    assert saved["qpf_before_path"].exists()
    assert saved["ptype_before_path"].exists()
    manifest = json.loads(saved["qpf_build_manifest_path"].read_text(encoding="utf-8"))
    assert manifest["window_id"] == window_id
