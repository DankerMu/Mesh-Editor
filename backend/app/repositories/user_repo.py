from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.models import AppUser


class UserRepository:
    async def get_by_id(self, db: AsyncSession, user_id: int) -> AppUser | None:
        result = await db.execute(select(AppUser).where(AppUser.id == user_id))
        return result.scalar_one_or_none()

    async def get_by_username(self, db: AsyncSession, username: str) -> AppUser | None:
        result = await db.execute(select(AppUser).where(AppUser.username == username))
        return result.scalar_one_or_none()

    async def create(
        self,
        db: AsyncSession,
        username: str,
        password_hash: str,
        display_name: str,
        role: str,
    ) -> AppUser:
        user = AppUser(
            username=username,
            password_hash=password_hash,
            display_name=display_name,
            role=role,
        )
        db.add(user)
        await db.flush()
        await db.refresh(user)
        return user

    async def update(self, db: AsyncSession, user: AppUser, **kwargs: object) -> AppUser:
        for key, value in kwargs.items():
            setattr(user, key, value)
        db.add(user)
        await db.flush()
        await db.refresh(user)
        return user
