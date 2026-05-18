from __future__ import annotations

import asyncio
from functools import partial

from tests.e2e_helpers import (
    TEMPLATE_ID,
    WINDOW_ID,
    apply_qpf_edit,
    audit_actions,
    auth,
    e2e_client,
    export_review,
    fake_generate_review,
    generate_review,
    review_product,
    save_submit_approve_release,
    start_session,
)


def test_e2e_case1_normal_full_happy_path(tmp_path, monkeypatch) -> None:
    monkeypatch.setattr(
        "app.api.routes.reviews.generate_review",
        partial(fake_generate_review, plot_status="success", missing_fields=[]),
    )

    with e2e_client(tmp_path, monkeypatch) as env:
        session_id = start_session(env)
        apply_qpf_edit(env, session_id)
        version_id = save_submit_approve_release(env, session_id)

        detail = env.client.get(
            f"/api/versions/{version_id}", headers=auth(env.tokens["forecaster"])
        )
        assert detail.status_code == 200, detail.text
        assert detail.json()["data"]["status"] == "released"

        review_id = generate_review(env, version_id, expected_status="success")
        task = env.client.get(
            f"/api/tasks/plot/{review_id}", headers=auth(env.tokens["forecaster"])
        )
        assert task.status_code == 200, task.text
        assert task.json()["data"]["plot_status"] == "success"
        export_review(env, review_id)

        product = asyncio.run(review_product(env.session_factory, review_id))
        assert product.window_id == WINDOW_ID
        assert product.version_id == version_id
        assert product.template_id == TEMPLATE_ID
        assert product.plot_status == "success"

        actions = asyncio.run(audit_actions(env.session_factory))
        for expected in [
            "login",
            "session_start",
            "edit_apply",
            "version_save",
            "version_submit",
            "version_review",
            "version_release",
            "review_generate",
        ]:
            assert expected in actions
