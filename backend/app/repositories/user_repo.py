from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.models import AppUser


class UserRepository:
    async def get_by_id(self, db: AsyncSession, user_id: int) -> AppUser | None:
        result = await db.execute(select(AppUser).where(AppUser.id == user_id))
        return result.scalar_one_or_none()

    async def get_by_username(self, db: AsyncSession, username: str) -> AppUser | None:
        result = await db.execute(select(AppUser).where(AppUser.username == username))
        return result.scalar_one_or_none()

    async def list_all(
        self,
        db: AsyncSession,
        role: str | None = None,
        is_active: bool | None = None,
        page: int = 1,
        page_size: int = 20,
    ) -> list[AppUser]:
        query = select(AppUser)
        if role is not None:
            query = query.where(AppUser.role == role)
        if is_active is not None:
            query = query.where(AppUser.is_active == is_active)
        query = (
            query.order_by(AppUser.created_at.desc(), AppUser.id.desc())
            .offset((page - 1) * page_size)
            .limit(page_size)
        )
        result = await db.execute(query)
        return list(result.scalars().all())

    async def count(
        self,
        db: AsyncSession,
        role: str | None = None,
        is_active: bool | None = None,
    ) -> int:
        query = select(func.count(AppUser.id))
        if role is not None:
            query = query.where(AppUser.role == role)
        if is_active is not None:
            query = query.where(AppUser.is_active == is_active)
        result = await db.execute(query)
        return int(result.scalar_one())

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
            is_active=True,
        )
        db.add(user)
        await db.flush()
        await db.refresh(user)
        return user

    async def update(
        self, db: AsyncSession, user: AppUser, **kwargs: object
    ) -> AppUser:
        for key, value in kwargs.items():
            setattr(user, key, value)
        db.add(user)
        await db.flush()
        await db.refresh(user)
        return user
