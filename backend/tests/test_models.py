from app.db.models import DataScanLog, ForecastCase, ProductWindow


def test_m1_models_have_expected_table_names() -> None:
    assert ForecastCase.__tablename__ == "forecast_case"
    assert ProductWindow.__tablename__ == "product_window"
    assert DataScanLog.__tablename__ == "data_scan_log"
