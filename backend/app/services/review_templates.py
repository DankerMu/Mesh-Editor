from dataclasses import dataclass


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


_TEMPLATES: dict[str, ReviewTemplate] = {
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


def load_template(template_id: str) -> ReviewTemplate:
    template = _TEMPLATES.get(template_id)
    if template is None:
        from app.core.error_registry import get_error

        msg, _status = get_error("TEMPLATE_NOT_FOUND")
        raise ValueError(msg)
    return template


def list_templates() -> list[ReviewTemplate]:
    return list(_TEMPLATES.values())
