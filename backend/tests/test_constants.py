from app.core import constants


def test_grid_constants() -> None:
    assert constants.NY == 501
    assert constants.NX == 821
    assert constants.LON_MIN == 70.0
    assert constants.LON_MAX == 111.0
    assert constants.LAT_MIN == 25.0
    assert constants.LAT_MAX == 50.0
    assert constants.DLON == 0.05
    assert constants.DLAT == 0.05
    assert constants.PROJECTION == "EPSG:4326"
    assert constants.ROW_COL_ORDER == "y_desc_x_asc"
