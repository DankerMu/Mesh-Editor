from pathlib import Path
from typing import Any

import numpy as np
import pytest

from app.core.config import ProductConfig
from app.storage.path_builder import PathBuilder

TEST_SHAPE = (5, 5)
CASE_ID = "2026051608"


def write_grid(path: Path, array: np.ndarray) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    np.savetxt(path, array, delimiter=",", fmt="%.6f")


def make_builder(tmp_path: Path) -> PathBuilder:
    return PathBuilder(
        base_dir=tmp_path / "archive", data_source_root=tmp_path / "source"
    )


@pytest.fixture()
def product_config() -> ProductConfig:
    return ProductConfig.model_validate(
        {
            "max_lead_hours": 240,
            "lead_step_hours": 3,
            "ptype_qpf_threshold_mm": 0.1,
            "allow_zero_start_lead_fallback": True,
        }
    )


@pytest.fixture()
def case_factory(tmp_path: Path) -> Any:
    def create(
        *,
        leads: list[int] | None = None,
        start_missing: bool = False,
        ptype_missing_leads: set[int] | None = None,
        ptype_value_by_lead: dict[int, int] | None = None,
        qpf_step: float = 0.2,
    ) -> tuple[str, PathBuilder]:
        selected_leads = leads or [0, 3, 6]
        missing_ptype = ptype_missing_leads or set()
        value_by_lead = ptype_value_by_lead or {}
        builder = make_builder(tmp_path)
        running = np.zeros(TEST_SHAPE, dtype=float)
        for lead in selected_leads:
            if lead == 0:
                running = np.zeros(TEST_SHAPE, dtype=float)
            else:
                running = running + qpf_step
            if not (lead == 0 and start_missing):
                write_grid(builder.tp_file_path(CASE_ID, lead), running)
            if lead > 0 and lead not in missing_ptype:
                ptype_value = value_by_lead.get(lead, 1)
                write_grid(
                    builder.ptype_file_path(CASE_ID, lead),
                    np.full(TEST_SHAPE, ptype_value, dtype=float),
                )
        return CASE_ID, builder

    return create
