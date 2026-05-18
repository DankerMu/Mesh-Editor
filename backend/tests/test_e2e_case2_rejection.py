from __future__ import annotations

import asyncio

from tests.e2e_helpers import (
    apply_qpf_edit,
    audit_actions,
    audit_logs,
    e2e_client,
    release_version,
    review_version,
    save_version,
    set_session_base_version,
    start_session,
    submit_version,
    versions_by_no,
)


def test_e2e_case2_rejection_reedit_and_release_chain(tmp_path, monkeypatch) -> None:
    with e2e_client(tmp_path, monkeypatch) as env:
        first_session_id = start_session(env)
        apply_qpf_edit(env, first_session_id, delta_mm=0.3)
        v001 = save_version(env, first_session_id)
        submit_version(env, v001)
        review_version(env, v001, action="reject", comment="需要调整降水量")

        second_session_id = start_session(env)
        asyncio.run(set_session_base_version(env.session_factory, second_session_id, v001))
        apply_qpf_edit(env, second_session_id, delta_mm=0.6)
        v002 = save_version(env, second_session_id)
        submit_version(env, v002)
        review_version(env, v002, action="approve", comment="通过")
        release_version(env, v002)

        versions = asyncio.run(versions_by_no(env.session_factory))
        assert [(version.version_no, version.status) for version in versions] == [
            (1, "rejected"),
            (2, "released"),
        ]
        assert [version.base_version_id for version in versions] == [None, v001]

        actions = asyncio.run(audit_actions(env.session_factory))
        for expected in [
            "session_start",
            "edit_apply",
            "version_save",
            "version_submit",
            "version_review",
            "version_release",
        ]:
            assert expected in actions
        assert actions.count("version_review") == 2

        review_logs = asyncio.run(audit_logs(env.session_factory, "version_review"))
        reject_log = next(log for log in review_logs if log.resource_id == v001)
        assert "reject" in str(reject_log.detail_json)
