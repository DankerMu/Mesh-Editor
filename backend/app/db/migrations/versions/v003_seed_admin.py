"""seed default admin user

Revision ID: v003
Revises: v002
Create Date: 2026-05-16

"""

from alembic import op
from passlib.context import CryptContext  # type: ignore[import-untyped]
import sqlalchemy as sa


revision = "v003"
down_revision = "v002"
branch_labels = None
depends_on = None


pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")


def upgrade() -> None:
    conn = op.get_bind()
    result = conn.execute(
        sa.text("SELECT COUNT(*) FROM app_user WHERE username = :u").bindparams(
            u="admin"
        )
    )
    if result.scalar() != 0:
        return

    password_hash = pwd_context.hash("admin123")
    op.execute(
        sa.text(
            """
            INSERT INTO app_user
                (username, password_hash, display_name, role, is_active)
            VALUES
                (:username, :password_hash, :display_name, :role, :is_active)
            """
        ).bindparams(
            username="admin",
            password_hash=password_hash,
            display_name="系统管理员",
            role="admin",
            is_active=True,
        )
    )


def downgrade() -> None:
    op.execute(
        sa.text("DELETE FROM app_user WHERE username = :username").bindparams(
            username="admin"
        )
    )
