"""create audit_log table

Revision ID: v002
Revises: v001
Create Date: 2026-05-16

"""

from alembic import op
import sqlalchemy as sa


revision = "v002"
down_revision = "v001"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        "audit_log",
        sa.Column("id", sa.Integer(), primary_key=True, autoincrement=True),
        sa.Column("user_id", sa.Integer(), nullable=True),
        sa.Column("username", sa.String(length=64), nullable=False),
        sa.Column("action", sa.String(length=64), nullable=False),
        sa.Column("resource_type", sa.String(length=64), nullable=True),
        sa.Column("resource_id", sa.String(length=128), nullable=True),
        sa.Column("detail_json", sa.Text(), nullable=True),
        sa.Column("ip_address", sa.String(length=45), nullable=True),
        sa.Column("created_at", sa.DateTime(), server_default=sa.func.now()),
    )
    op.create_index(
        "idx_audit_log_user_time",
        "audit_log",
        ["user_id", "created_at"],
    )
    op.create_index(
        "idx_audit_log_resource",
        "audit_log",
        ["resource_type", "resource_id"],
    )


def downgrade() -> None:
    op.drop_index("idx_audit_log_resource", table_name="audit_log")
    op.drop_index("idx_audit_log_user_time", table_name="audit_log")
    op.drop_table("audit_log")
