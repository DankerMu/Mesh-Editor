"""create m6 config snapshot table

Revision ID: v009
Revises: v008
Create Date: 2026-05-18

"""

from alembic import op
import sqlalchemy as sa


revision = "v009"
down_revision = "v008"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        "config_snapshot",
        sa.Column("snapshot_id", sa.String(), primary_key=True),
        sa.Column("config_type", sa.String(), nullable=False),
        sa.Column("config_json", sa.Text(), nullable=False),
        sa.Column("changed_by", sa.String(), nullable=True),
        sa.Column(
            "created_at", sa.DateTime(), nullable=False, server_default=sa.func.now()
        ),
    )
    op.add_column("app_user", sa.Column("last_login_at", sa.DateTime(), nullable=True))


def downgrade() -> None:
    op.drop_column("app_user", "last_login_at")
    op.drop_table("config_snapshot")
