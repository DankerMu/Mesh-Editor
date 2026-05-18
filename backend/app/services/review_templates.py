from __future__ import annotations

import json
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Any

from app.core.constants import REPO_ROOT


@dataclass(frozen=True)
class ReviewTemplatePanel:
    id: str
    type: str
    fields: list[str]


@dataclass(frozen=True)
class ReviewTemplate:
    template_id: str
    template_name: str
    required_fields: list[str]
    optional_fields: list[str]
    allow_partial_success: bool
    panels: list[ReviewTemplatePanel]
    review_time_policy: str


TEMPLATE_CONFIG_PATH = REPO_ROOT / "schemas" / "review_templates.json"


_DEFAULT_TEMPLATES: dict[str, ReviewTemplate] = {
    "snow_phase_review_v1": ReviewTemplate(
        template_id="snow_phase_review_v1",
        template_name="雨雪相态复盘 V1",
        required_fields=[
            "qpf_before",
            "ptype_before",
            "qpf_after",
            "ptype_after",
            "delta_qpf",
            "change_ptype",
        ],
        optional_fields=["z500", "t850", "rh700", "u850", "v850"],
        allow_partial_success=True,
        panels=[
            ReviewTemplatePanel(
                id="before", type="precip_phase", fields=["qpf_before", "ptype_before"]
            ),
            ReviewTemplatePanel(
                id="after", type="precip_phase", fields=["qpf_after", "ptype_after"]
            ),
            ReviewTemplatePanel(id="delta", type="delta_qpf", fields=["delta_qpf"]),
            ReviewTemplatePanel(
                id="change", type="change_ptype", fields=["change_ptype"]
            ),
            ReviewTemplatePanel(
                id="circulation", type="circulation", fields=["z500", "u850", "v850"]
            ),
        ],
        review_time_policy="middle",
    ),
}


_TEMPLATES: dict[str, ReviewTemplate] = dict(_DEFAULT_TEMPLATES)


def _panel_from_dict(payload: dict[str, Any]) -> ReviewTemplatePanel:
    return ReviewTemplatePanel(
        id=str(payload["id"]),
        type=str(payload["type"]),
        fields=[str(field) for field in payload["fields"]],
    )


def _template_from_dict(template_id: str, payload: dict[str, Any]) -> ReviewTemplate:
    return ReviewTemplate(
        template_id=template_id,
        template_name=str(payload["template_name"]),
        required_fields=[str(field) for field in payload["required_fields"]],
        optional_fields=[str(field) for field in payload.get("optional_fields", [])],
        allow_partial_success=bool(payload.get("allow_partial_success", False)),
        panels=[_panel_from_dict(panel) for panel in payload["panels"]],
        review_time_policy=str(payload.get("review_time_policy", "middle")),
    )


def template_to_dict(template: ReviewTemplate) -> dict[str, Any]:
    return asdict(template)


def _templates_to_dict(
    templates: dict[str, ReviewTemplate],
) -> dict[str, dict[str, Any]]:
    return {
        template_id: template_to_dict(template)
        for template_id, template in templates.items()
    }


def reload_templates(path: Path | None = None) -> dict[str, ReviewTemplate]:
    template_path = path or TEMPLATE_CONFIG_PATH
    global _TEMPLATES
    if not template_path.exists():
        _TEMPLATES = dict(_DEFAULT_TEMPLATES)
        return _TEMPLATES

    payload = json.loads(template_path.read_text(encoding="utf-8"))
    if not isinstance(payload, dict):
        raise ValueError("模板配置文件顶层必须为对象")

    loaded = dict(_DEFAULT_TEMPLATES)
    for template_id, template_payload in payload.items():
        if not isinstance(template_payload, dict):
            raise ValueError("模板配置必须为对象")
        loaded[str(template_id)] = _template_from_dict(str(template_id), template_payload)
    _TEMPLATES = loaded
    return _TEMPLATES


def save_template(
    template_id: str,
    template_payload: dict[str, Any],
    path: Path | None = None,
) -> ReviewTemplate:
    template_path = path or TEMPLATE_CONFIG_PATH
    existing = dict(_TEMPLATES)
    template = _template_from_dict(template_id, template_payload)
    existing[template_id] = template
    template_path.parent.mkdir(parents=True, exist_ok=True)
    template_path.write_text(
        json.dumps(_templates_to_dict(existing), ensure_ascii=False, indent=2)
        + "\n",
        encoding="utf-8",
    )
    reload_templates(template_path)
    return load_template(template_id)


def load_template(template_id: str) -> ReviewTemplate:
    template = _TEMPLATES.get(template_id)
    if template is None:
        from app.core.error_registry import get_error

        msg, _status = get_error("TEMPLATE_NOT_FOUND")
        raise ValueError(msg)
    return template


def list_templates() -> list[ReviewTemplate]:
    return list(_TEMPLATES.values())
