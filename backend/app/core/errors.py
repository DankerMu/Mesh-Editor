from typing import Any


class DomainError(Exception):
    def __init__(
        self,
        code: str,
        message: str,
        detail: dict[str, Any] | list[Any] | None = None,
        http_status: int = 400,
    ) -> None:
        super().__init__(message)
        self.code = code
        self.message = message
        self.detail = detail if detail is not None else {}
        self.http_status = http_status
