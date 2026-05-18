"""create m5 review plot tables

Revision ID: v008
Revises: v007
Create Date: 2026-05-17

"""

from alembic import op
import sqlalchemy as sa


revision = "v008"
down_revision = "v007"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        "review_field",
        sa.Column("field_id", sa.String(length=36), primary_key=True),
        sa.Column("window_id", sa.String(length=32), nullable=False),
        sa.Column("version_id", sa.String(length=64), nullable=True),
        sa.Column("source_model", sa.String(length=32), nullable=False),
        sa.Column("variable_name", sa.String(length=32), nullable=False),
        sa.Column("level_type", sa.String(length=16), nullable=True),
        sa.Column("level_value", sa.Integer(), nullable=True),
        sa.Column("lead_hour", sa.Integer(), nullable=True),
        sa.Column("valid_time", sa.DateTime(), nullable=True),
        sa.Column("unit", sa.String(length=16), nullable=True),
        sa.Column("file_path", sa.String(length=512), nullable=False),
        sa.Column("created_at", sa.DateTime(), server_default=sa.func.now()),
        sa.ForeignKeyConstraint(["window_id"], ["product_window.window_id"]),
    )
    op.create_index(
        "idx_review_field_window",
        "review_field",
        ["window_id"],
        unique=False,
    )

    op.create_table(
        "review_product",
        sa.Column("review_id", sa.String(length=36), primary_key=True),
        sa.Column("window_id", sa.String(length=32), nullable=False),
        sa.Column("version_id", sa.String(length=64), nullable=False),
        sa.Column("template_id", sa.String(length=64), nullable=False),
        sa.Column("image_path", sa.String(length=512), nullable=True),
        sa.Column("plot_config_path", sa.String(length=512), nullable=True),
        sa.Column("plot_input_manifest_path", sa.String(length=512), nullable=True),
        sa.Column("plot_code_version", sa.String(length=64), nullable=True),
        sa.Column(
            "plot_status",
            sa.String(length=20),
            nullable=False,
            server_default="pending",
        ),
        sa.Column("attempt", sa.Integer(), nullable=False, server_default="0"),
        sa.Column("max_retries", sa.Integer(), nullable=False, server_default="3"),
        sa.Column("locked_by", sa.String(length=64), nullable=True),
        sa.Column("locked_at", sa.DateTime(), nullable=True),
        sa.Column("next_retry_at", sa.DateTime(), nullable=True),
        sa.Column("plot_started_at", sa.DateTime(), nullable=True),
        sa.Column("plot_finished_at", sa.DateTime(), nullable=True),
        sa.Column("total_panels", sa.Integer(), nullable=True),
        sa.Column("success_panels", sa.Integer(), nullable=True),
        sa.Column("skipped_panels", sa.Integer(), nullable=True),
        sa.Column("missing_fields_json", sa.Text(), nullable=True),
        sa.Column("error_log_path", sa.String(length=512), nullable=True),
        sa.Column(
            "created_at", sa.DateTime(), nullable=False, server_default=sa.func.now()
        ),
        sa.ForeignKeyConstraint(["window_id"], ["product_window.window_id"]),
        sa.ForeignKeyConstraint(["version_id"], ["edit_version.version_id"]),
    )
    op.create_index(
        "idx_review_product_window_status",
        "review_product",
        ["window_id", "plot_status"],
        unique=False,
    )
    op.create_index(
        "idx_review_product_supersede",
        "review_product",
        ["window_id", "version_id", "template_id"],
        unique=False,
    )


def downgrade() -> None:
    op.drop_index("idx_review_product_supersede", table_name="review_product")
    op.drop_index("idx_review_product_window_status", table_name="review_product")
    op.drop_table("review_product")
    op.drop_index("idx_review_field_window", table_name="review_field")
    op.drop_table("review_field")
