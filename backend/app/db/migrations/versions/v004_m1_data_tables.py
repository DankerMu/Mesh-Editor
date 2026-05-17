"""create m1 data ingestion tables

Revision ID: v004
Revises: v003
Create Date: 2026-05-16

"""

from alembic import op
import sqlalchemy as sa


revision = "v004"
down_revision = "v003"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        "forecast_case",
        sa.Column("case_id", sa.String(length=10), primary_key=True),
        sa.Column("init_time", sa.DateTime(), nullable=False),
        sa.Column(
            "region_id", sa.String(length=32), nullable=False, server_default="west7"
        ),
        sa.Column(
            "grid_id",
            sa.String(length=32),
            nullable=False,
            server_default="0p05_501x821",
        ),
        sa.Column("data_source_path", sa.String(length=512), nullable=True),
        sa.Column("scan_count", sa.Integer(), nullable=False, server_default="0"),
        sa.Column("last_scan_at", sa.DateTime(), nullable=True),
        sa.Column(
            "status", sa.String(length=32), nullable=False, server_default="pending"
        ),
        sa.Column("created_at", sa.DateTime(), server_default=sa.func.now()),
    )
    op.create_table(
        "product_window",
        sa.Column("window_id", sa.String(length=32), primary_key=True),
        sa.Column("case_id", sa.String(length=10), nullable=False),
        sa.Column("accum_hours", sa.Integer(), nullable=False),
        sa.Column("start_lead", sa.Integer(), nullable=False),
        sa.Column("end_lead", sa.Integer(), nullable=False),
        sa.Column(
            "status", sa.String(length=32), nullable=False, server_default="pending"
        ),
        sa.Column(
            "qc_status",
            sa.String(length=32),
            nullable=False,
            server_default="unchecked",
        ),
        sa.Column("negative_count", sa.Integer(), nullable=False, server_default="0"),
        sa.Column("negative_min_value", sa.Float(), nullable=True),
        sa.Column("negative_abs_max", sa.Float(), nullable=True),
        sa.Column("missing_count", sa.Integer(), nullable=False, server_default="0"),
        sa.Column("ptype_missing_leads", sa.JSON(), nullable=True),
        sa.Column("qpf_before_path", sa.String(length=512), nullable=True),
        sa.Column("ptype_before_path", sa.String(length=512), nullable=True),
        sa.Column("data_ready_at", sa.DateTime(), nullable=True),
        sa.Column("created_at", sa.DateTime(), server_default=sa.func.now()),
        sa.Column("updated_at", sa.DateTime(), server_default=sa.func.now()),
        sa.ForeignKeyConstraint(["case_id"], ["forecast_case.case_id"]),
    )
    op.create_index("idx_product_window_case_id", "product_window", ["case_id"])
    op.create_table(
        "data_scan_log",
        sa.Column("scan_id", sa.String(length=36), primary_key=True),
        sa.Column("case_id", sa.String(length=10), nullable=False),
        sa.Column("status", sa.String(length=32), nullable=False),
        sa.Column("scan_started_at", sa.DateTime(), nullable=False),
        sa.Column("scan_finished_at", sa.DateTime(), nullable=True),
        sa.Column("tp_files_found", sa.Integer(), nullable=False, server_default="0"),
        sa.Column(
            "ptype_files_found", sa.Integer(), nullable=False, server_default="0"
        ),
        sa.Column("windows_created", sa.Integer(), nullable=False, server_default="0"),
        sa.Column("windows_updated", sa.Integer(), nullable=False, server_default="0"),
        sa.Column("errors_json", sa.JSON(), nullable=True),
        sa.ForeignKeyConstraint(["case_id"], ["forecast_case.case_id"]),
    )
    op.create_index("idx_data_scan_log_case_id", "data_scan_log", ["case_id"])


def downgrade() -> None:
    op.drop_index("idx_data_scan_log_case_id", table_name="data_scan_log")
    op.drop_table("data_scan_log")
    op.drop_index("idx_product_window_case_id", table_name="product_window")
    op.drop_table("product_window")
    op.drop_table("forecast_case")
