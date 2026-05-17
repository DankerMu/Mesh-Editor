from pathlib import Path

import pytest
import sqlalchemy as sa
from alembic import command
from alembic.config import Config
from sqlalchemy import inspect
from sqlalchemy.exc import IntegrityError


BACKEND_ROOT = Path(__file__).resolve().parents[1]
APP_USER_COLUMNS = {
    "id",
    "username",
    "password_hash",
    "display_name",
    "role",
    "is_active",
    "created_at",
    "updated_at",
}
AUDIT_LOG_COLUMNS = {
    "id",
    "user_id",
    "username",
    "action",
    "resource_type",
    "resource_id",
    "detail_json",
    "ip_address",
    "created_at",
}
FORECAST_CASE_COLUMNS = {
    "case_id",
    "init_time",
    "region_id",
    "grid_id",
    "data_source_path",
    "scan_count",
    "last_scan_at",
    "status",
    "created_at",
}
PRODUCT_WINDOW_COLUMNS = {
    "window_id",
    "case_id",
    "accum_hours",
    "start_lead",
    "end_lead",
    "status",
    "qc_status",
    "negative_count",
    "negative_min_value",
    "negative_abs_max",
    "missing_count",
    "ptype_missing_leads",
    "qpf_before_path",
    "ptype_before_path",
    "data_ready_at",
    "created_at",
    "updated_at",
}
DATA_SCAN_LOG_COLUMNS = {
    "scan_id",
    "case_id",
    "status",
    "scan_started_at",
    "scan_finished_at",
    "tp_files_found",
    "ptype_files_found",
    "windows_created",
    "windows_updated",
    "errors_json",
}


def sqlite_async_url(db_path: Path) -> str:
    return f"sqlite+aiosqlite:///{db_path}"


def sqlite_sync_url(db_path: Path) -> str:
    return f"sqlite:///{db_path}"


def alembic_config(db_path: Path) -> Config:
    cfg = Config(str(BACKEND_ROOT / "alembic.ini"))
    cfg.set_main_option("script_location", str(BACKEND_ROOT / "app/db/migrations"))
    cfg.set_main_option("sqlalchemy.url", sqlite_async_url(db_path))
    return cfg


def upgrade_head(db_path: Path) -> None:
    command.upgrade(alembic_config(db_path), "head")


def downgrade_base(db_path: Path) -> None:
    command.downgrade(alembic_config(db_path), "base")


def table_names(db_path: Path) -> set[str]:
    engine = sa.create_engine(sqlite_sync_url(db_path))
    try:
        return set(inspect(engine).get_table_names())
    finally:
        engine.dispose()


def column_names(db_path: Path, table_name: str) -> set[str]:
    engine = sa.create_engine(sqlite_sync_url(db_path))
    try:
        return {column["name"] for column in inspect(engine).get_columns(table_name)}
    finally:
        engine.dispose()


def test_alembic_upgrade_head(tmp_path: Path) -> None:
    db_path = tmp_path / "upgrade.db"

    upgrade_head(db_path)

    assert {
        "app_user",
        "audit_log",
        "forecast_case",
        "product_window",
        "data_scan_log",
    }.issubset(table_names(db_path))


def test_alembic_roundtrip(tmp_path: Path) -> None:
    db_path = tmp_path / "round_trip.db"

    upgrade_head(db_path)
    downgrade_base(db_path)
    tables_after_downgrade = table_names(db_path) - {"alembic_version"}
    assert "app_user" not in tables_after_downgrade
    assert "audit_log" not in tables_after_downgrade
    upgrade_head(db_path)

    assert {
        "app_user",
        "audit_log",
        "forecast_case",
        "product_window",
        "data_scan_log",
    }.issubset(table_names(db_path))


def test_app_user_columns(tmp_path: Path) -> None:
    db_path = tmp_path / "app_user_columns.db"

    upgrade_head(db_path)

    assert APP_USER_COLUMNS.issubset(column_names(db_path, "app_user"))


def test_username_unique(tmp_path: Path) -> None:
    db_path = tmp_path / "unique_username.db"
    upgrade_head(db_path)
    engine = sa.create_engine(sqlite_sync_url(db_path))

    try:
        with pytest.raises(IntegrityError):
            with engine.begin() as connection:
                connection.execute(
                    sa.text(
                        """
                        INSERT INTO app_user
                            (username, password_hash, display_name, role, is_active)
                        VALUES
                            ('demo', 'hash-a', '测试用户A', 'viewer', 1),
                            ('demo', 'hash-b', '测试用户B', 'viewer', 1)
                        """
                    )
                )
    finally:
        engine.dispose()


def test_audit_log_columns(tmp_path: Path) -> None:
    db_path = tmp_path / "audit_log_columns.db"

    upgrade_head(db_path)

    assert AUDIT_LOG_COLUMNS.issubset(column_names(db_path, "audit_log"))


def test_audit_log_indexes(tmp_path: Path) -> None:
    db_path = tmp_path / "audit_log_indexes.db"
    upgrade_head(db_path)
    engine = sa.create_engine(sqlite_sync_url(db_path))

    try:
        indexes = inspect(engine).get_indexes("audit_log")
    finally:
        engine.dispose()

    assert any(
        index["name"] == "idx_audit_log_user_time"
        and index["column_names"] == ["user_id", "created_at"]
        for index in indexes
    )
    assert any(
        index["name"] == "idx_audit_log_resource"
        and index["column_names"] == ["resource_type", "resource_id"]
        for index in indexes
    )


def test_m1_data_table_columns(tmp_path: Path) -> None:
    db_path = tmp_path / "m1_columns.db"

    upgrade_head(db_path)

    assert FORECAST_CASE_COLUMNS.issubset(column_names(db_path, "forecast_case"))
    assert PRODUCT_WINDOW_COLUMNS.issubset(column_names(db_path, "product_window"))
    assert DATA_SCAN_LOG_COLUMNS.issubset(column_names(db_path, "data_scan_log"))


def test_m1_data_table_indexes(tmp_path: Path) -> None:
    db_path = tmp_path / "m1_indexes.db"
    upgrade_head(db_path)
    engine = sa.create_engine(sqlite_sync_url(db_path))

    try:
        product_window_indexes = inspect(engine).get_indexes("product_window")
        data_scan_log_indexes = inspect(engine).get_indexes("data_scan_log")
    finally:
        engine.dispose()

    assert any(
        index["name"] == "idx_product_window_case_id"
        and index["column_names"] == ["case_id"]
        for index in product_window_indexes
    )
    assert any(
        index["name"] == "idx_data_scan_log_case_id"
        and index["column_names"] == ["case_id"]
        for index in data_scan_log_indexes
    )
