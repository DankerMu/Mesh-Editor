"""create edit operation table

Revision ID: v006
Revises: v005
Create Date: 2026-05-17

"""

from alembic import op
import sqlalchemy as sa


revision = "v006"
down_revision = "v005"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        "edit_operation",
        sa.Column("operation_id", sa.String(length=36), primary_key=True),
        sa.Column("session_id", sa.String(length=36), nullable=False),
        sa.Column("window_id", sa.String(length=32), nullable=False),
        sa.Column("sequence_no", sa.Integer(), nullable=False),
        sa.Column("tool_name", sa.String(length=32), nullable=False),
        sa.Column("variable_name", sa.String(length=16), nullable=False),
        sa.Column("operation_type", sa.String(length=32), nullable=False),
        sa.Column("parameters_json", sa.Text(), nullable=True),
        sa.Column("mask_geometry_json", sa.Text(), nullable=True),
        sa.Column("mask_raster_path", sa.String(length=512), nullable=True),
        sa.Column("before_stats_json", sa.Text(), nullable=True),
        sa.Column("after_stats_json", sa.Text(), nullable=True),
        sa.Column("op_ptype_transition_json", sa.Text(), nullable=True),
        sa.Column("is_undone", sa.Integer(), nullable=False, server_default="0"),
        sa.Column(
            "created_at", sa.DateTime(), nullable=False, server_default=sa.func.now()
        ),
        sa.CheckConstraint("is_undone IN (0, 1)", name="ck_edit_op_is_undone"),
        sa.ForeignKeyConstraint(["session_id"], ["edit_session.session_id"]),
    )
    op.create_index(
        "idx_edit_op_session_seq",
        "edit_operation",
        ["session_id", "sequence_no"],
        unique=True,
    )


def downgrade() -> None:
    op.drop_index("idx_edit_op_session_seq", table_name="edit_operation")
    op.drop_table("edit_operation")
