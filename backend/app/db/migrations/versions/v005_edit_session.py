"""create edit session table

Revision ID: v005
Revises: v004
Create Date: 2026-05-17

"""

from alembic import op
import sqlalchemy as sa


revision = "v005"
down_revision = "v004"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        "edit_session",
        sa.Column("session_id", sa.String(length=36), primary_key=True),
        sa.Column("window_id", sa.String(length=32), nullable=False),
        sa.Column("user_id", sa.Integer(), nullable=False),
        sa.Column("base_version_id", sa.String(length=36), nullable=True),
        sa.Column(
            "status", sa.String(length=20), nullable=False, server_default="editing"
        ),
        sa.Column("created_at", sa.DateTime(), server_default=sa.func.now()),
        sa.Column("updated_at", sa.DateTime(), server_default=sa.func.now()),
        sa.ForeignKeyConstraint(["window_id"], ["product_window.window_id"]),
        sa.ForeignKeyConstraint(["user_id"], ["app_user.id"]),
    )
    op.create_index(
        "idx_edit_session_window_active",
        "edit_session",
        ["window_id", "status"],
        unique=True,
        sqlite_where=sa.text("status = 'editing'"),
    )


def downgrade() -> None:
    op.drop_index("idx_edit_session_window_active", table_name="edit_session")
    op.drop_table("edit_session")
