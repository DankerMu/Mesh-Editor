"""create m4 version approval release tables

Revision ID: v007
Revises: v006
Create Date: 2026-05-17

"""

from alembic import op
import sqlalchemy as sa


revision = "v007"
down_revision = "v006"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        "edit_version",
        sa.Column("version_id", sa.String(length=64), primary_key=True),
        sa.Column("window_id", sa.String(length=32), nullable=False),
        sa.Column("version_no", sa.Integer(), nullable=False),
        sa.Column("base_version_id", sa.String(length=64), nullable=True),
        sa.Column("session_id", sa.String(length=36), nullable=True),
        sa.Column(
            "status", sa.String(length=20), nullable=False, server_default="draft"
        ),
        sa.Column("qpf_after_path", sa.String(length=512), nullable=False),
        sa.Column("ptype_after_path", sa.String(length=512), nullable=False),
        sa.Column("delta_qpf_path", sa.String(length=512), nullable=False),
        sa.Column("change_ptype_path", sa.String(length=512), nullable=False),
        sa.Column("touched_mask_path", sa.String(length=512), nullable=False),
        sa.Column("changed_mask_path", sa.String(length=512), nullable=False),
        sa.Column(
            "version_ptype_transition_path", sa.String(length=512), nullable=True
        ),
        sa.Column("before_image_path", sa.String(length=512), nullable=True),
        sa.Column("after_image_path", sa.String(length=512), nullable=True),
        sa.Column("delta_qpf_image_path", sa.String(length=512), nullable=True),
        sa.Column("change_ptype_image_path", sa.String(length=512), nullable=True),
        sa.Column("touched_mask_image_path", sa.String(length=512), nullable=True),
        sa.Column("changed_mask_image_path", sa.String(length=512), nullable=True),
        sa.Column("review_image_path", sa.String(length=512), nullable=True),
        sa.Column("created_by", sa.String(length=64), nullable=True),
        sa.Column(
            "created_at", sa.DateTime(), nullable=False, server_default=sa.func.now()
        ),
        sa.ForeignKeyConstraint(["window_id"], ["product_window.window_id"]),
    )
    op.create_index(
        "idx_edit_version_window",
        "edit_version",
        ["window_id", "status"],
        unique=False,
    )

    op.create_table(
        "review_approval",
        sa.Column("approval_id", sa.String(length=36), primary_key=True),
        sa.Column("version_id", sa.String(length=64), nullable=False),
        sa.Column("reviewer_id", sa.String(length=64), nullable=False),
        sa.Column("action", sa.String(length=20), nullable=False),
        sa.Column("comment", sa.Text(), nullable=True),
        sa.Column("reviewed_at", sa.DateTime(), nullable=False),
    )

    op.create_table(
        "release_product",
        sa.Column("release_id", sa.String(length=36), primary_key=True),
        sa.Column("version_id", sa.String(length=64), nullable=False),
        sa.Column("window_id", sa.String(length=32), nullable=False),
        sa.Column(
            "release_status",
            sa.String(length=20),
            nullable=False,
            server_default="active",
        ),
        sa.Column("product_path", sa.String(length=512), nullable=True),
        sa.Column("manifest_path", sa.String(length=512), nullable=True),
        sa.Column("released_by", sa.String(length=64), nullable=False),
        sa.Column("released_at", sa.DateTime(), nullable=False),
        sa.Column("superseded_at", sa.DateTime(), nullable=True),
    )
    op.create_index(
        "idx_release_product_window",
        "release_product",
        ["window_id", "release_status"],
        unique=False,
    )


def downgrade() -> None:
    op.drop_index("idx_release_product_window", table_name="release_product")
    op.drop_table("release_product")
    op.drop_table("review_approval")
    op.drop_index("idx_edit_version_window", table_name="edit_version")
    op.drop_table("edit_version")
