from sqlalchemy import (
    Boolean,
    Column,
    DateTime,
    Float,
    ForeignKey,
    Index,
    Integer,
    JSON,
    String,
    Text,
    func,
)
from sqlalchemy.orm import DeclarativeBase


class Base(DeclarativeBase):
    pass


class AppUser(Base):
    __tablename__ = "app_user"
    id = Column(Integer, primary_key=True, autoincrement=True)
    username = Column(String(64), unique=True, nullable=False)
    password_hash = Column(String(256), nullable=False)
    display_name = Column(String(64), nullable=False)
    role = Column(String(20), nullable=False, server_default="viewer")
    is_active = Column(Boolean, nullable=False, server_default=func.true())
    created_at = Column(DateTime, nullable=False, server_default=func.now())
    updated_at = Column(DateTime, server_default=func.now(), onupdate=func.now())


class AuditLog(Base):
    __tablename__ = "audit_log"
    __table_args__ = (
        Index("idx_audit_log_user_time", "user_id", "created_at"),
        Index("idx_audit_log_resource", "resource_type", "resource_id"),
    )
    id = Column(Integer, primary_key=True, autoincrement=True)
    user_id = Column(Integer, nullable=True)
    username = Column(String(64), nullable=False)
    action = Column(String(64), nullable=False)
    resource_type = Column(String(64), nullable=True)
    resource_id = Column(String(128), nullable=True)
    detail_json = Column(Text, nullable=True)
    ip_address = Column(String(45), nullable=True)
    created_at = Column(DateTime, server_default=func.now())


class ForecastCase(Base):
    __tablename__ = "forecast_case"
    case_id = Column(String(10), primary_key=True)
    init_time = Column(DateTime, nullable=False)
    region_id = Column(String(32), nullable=False, server_default="west7")
    grid_id = Column(String(32), nullable=False, server_default="0p05_501x821")
    data_source_path = Column(String(512), nullable=True)
    scan_count = Column(Integer, nullable=False, server_default="0")
    last_scan_at = Column(DateTime, nullable=True)
    status = Column(String(32), nullable=False, server_default="pending")
    created_at = Column(DateTime, server_default=func.now())


class ProductWindow(Base):
    __tablename__ = "product_window"
    __table_args__ = (Index("idx_product_window_case_id", "case_id"),)

    window_id = Column(String(32), primary_key=True)
    case_id = Column(String(10), ForeignKey("forecast_case.case_id"), nullable=False)
    accum_hours = Column(Integer, nullable=False)
    start_lead = Column(Integer, nullable=False)
    end_lead = Column(Integer, nullable=False)
    status = Column(String(32), nullable=False, server_default="pending")
    qc_status = Column(String(32), nullable=False, server_default="unchecked")
    negative_count = Column(Integer, nullable=False, server_default="0")
    negative_min_value = Column(Float, nullable=True)
    negative_abs_max = Column(Float, nullable=True)
    missing_count = Column(Integer, nullable=False, server_default="0")
    ptype_missing_leads = Column(JSON, nullable=True)
    qpf_before_path = Column(String(512), nullable=True)
    ptype_before_path = Column(String(512), nullable=True)
    data_ready_at = Column(DateTime, nullable=True)
    created_at = Column(DateTime, server_default=func.now())
    updated_at = Column(DateTime, server_default=func.now(), onupdate=func.now())


class DataScanLog(Base):
    __tablename__ = "data_scan_log"
    __table_args__ = (Index("idx_data_scan_log_case_id", "case_id"),)

    scan_id = Column(String(36), primary_key=True)
    case_id = Column(String(10), ForeignKey("forecast_case.case_id"), nullable=False)
    status = Column(String(32), nullable=False)
    scan_started_at = Column(DateTime, nullable=False)
    scan_finished_at = Column(DateTime, nullable=True)
    tp_files_found = Column(Integer, nullable=False, server_default="0")
    ptype_files_found = Column(Integer, nullable=False, server_default="0")
    windows_created = Column(Integer, nullable=False, server_default="0")
    windows_updated = Column(Integer, nullable=False, server_default="0")
    errors_json = Column(JSON, nullable=True)


class EditSession(Base):
    __tablename__ = "edit_session"
    __table_args__ = (
        Index(
            "idx_edit_session_window_active",
            "window_id",
            "status",
            unique=True,
            sqlite_where=Column("status") == "editing",
        ),
    )

    session_id = Column(String(36), primary_key=True)
    window_id = Column(
        String(32), ForeignKey("product_window.window_id"), nullable=False
    )
    user_id = Column(Integer, ForeignKey("app_user.id"), nullable=False)
    base_version_id = Column(String(36), nullable=True)
    status = Column(String(20), nullable=False, server_default="editing")
    created_at = Column(DateTime, server_default=func.now())
    updated_at = Column(DateTime, server_default=func.now(), onupdate=func.now())


class EditOperation(Base):
    __tablename__ = "edit_operation"
    __table_args__ = (
        Index("idx_edit_op_session_seq", "session_id", "sequence_no"),
    )

    operation_id = Column(String(36), primary_key=True)
    session_id = Column(
        String(36), ForeignKey("edit_session.session_id"), nullable=False
    )
    window_id = Column(String(32), nullable=False)
    sequence_no = Column(Integer, nullable=False)
    tool_name = Column(String(32), nullable=False)
    variable_name = Column(String(16), nullable=False)
    operation_type = Column(String(32), nullable=False)
    parameters_json = Column(Text, nullable=True)
    mask_geometry_json = Column(Text, nullable=True)
    mask_raster_path = Column(String(512), nullable=True)
    before_stats_json = Column(Text, nullable=True)
    after_stats_json = Column(Text, nullable=True)
    op_ptype_transition_json = Column(Text, nullable=True)
    is_undone = Column(Integer, nullable=False, server_default="0")
    created_at = Column(DateTime, server_default=func.now())
