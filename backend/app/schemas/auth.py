from datetime import datetime

from pydantic import BaseModel


class LoginRequest(BaseModel):
    username: str
    password: str


class LoginResponse(BaseModel):
    user_id: int
    username: str
    display_name: str
    role: str
    token: str
    expires_at: datetime
