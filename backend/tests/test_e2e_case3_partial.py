from __future__ import annotations

import asyncio
import json
from functools import partial

from tests.e2e_helpers import (
    apply_qpf_edit,
    audit_actions,
    audit_logs,
    auth,
    e2e_client,
    export_review,
    fake_generate_review,
    generate_review,
    review_product,
    save_submit_approve_release,
    start_session,
)


MISSING_FIELDS = [
    {
        "variable_name": "next24_qpf",
        "level_type": "surface",
        "level_value": None,
        "lead_hour": 48,
        "reason": "file_not_found",
    },
    {
        "variable_name": "z500",
        "level_type": "pressure",
        "level_value": 500,
        "lead_hour": 12,
        "reason": "file_not_found",
    },
]


def test_e2e_case3_partial_data_review_partial_success(tmp_path, monkeypatch) -> None:
    monkeypatch.setattr(
        "app.api.routes.reviews.generate_review",
        partial(
            fake_generate_review,
            plot_status="partial_success",
            missing_fields=MISSING_FIELDS,
        ),
    )

    with e2e_client(tmp_path, monkeypatch, partial=True) as env:
        session_id = start_session(env)
        apply_qpf_edit(env, session_id)
        version_id = save_submit_approve_release(env, session_id)

        review_id = generate_review(env, version_id, expected_status="partial_success")
        task = env.client.get(
            f"/api/tasks/plot/{review_id}", headers=auth(env.tokens["forecaster"])
        )
        assert task.status_code == 200, task.text
        assert task.json()["data"]["plot_status"] == "partial_success"
        export_review(env, review_id)

        product = asyncio.run(review_product(env.session_factory, review_id))
        assert product.plot_status == "partial_success"
        missing_fields = json.loads(str(product.missing_fields_json))
        assert missing_fields
        assert {field["variable_name"] for field in missing_fields} >= {
            "next24_qpf",
            "z500",
        }

        actions = asyncio.run(audit_actions(env.session_factory))
        for expected in [
            "session_start",
            "edit_apply",
            "version_save",
            "version_submit",
            "version_review",
            "version_release",
            "review_generate",
        ]:
            assert expected in actions

        review_logs = asyncio.run(audit_logs(env.session_factory, "review_generate"))
        assert review_logs
        assert review_logs[-1].resource_id == review_id
