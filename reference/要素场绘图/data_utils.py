import xarray as xr
import pandas as pd
import geopandas as gpd


def read_map_data(country_path, province_path, selected_provinces=None):
    """
    读取地图边界数据

    参数:
        country_path (str): 国界数据路径
        province_path (str): 省界数据路径
        selected_provinces (list): 要选择的省份列表

    返回:
        tuple: (gdf_country, gdf_province)
    """
    try:
        gdf_country = gpd.read_file(country_path)
        gdf_province = gpd.read_file(province_path)

        if selected_provinces:
            gdf_province = gdf_province[gdf_province['name'].isin(selected_provinces)]

        return gdf_country, gdf_province

    except Exception as e:
        raise ValueError(f"读取地图数据失败: {e}")


def read_surface_data(filepath, short_name):
    """
    读取地面数据

    参数:
        filepath (str): GRIB文件路径
        short_name (str): 变量短名(如'tp'表示降水)

    返回:
        tuple: (data, forecast_time, valid_time)
    """
    try:
        ds = xr.open_dataset(
            filepath,
            engine='cfgrib',
            backend_kwargs={'filter_by_keys': {'shortName': short_name}}
        )

        # 获取数据变量
        data = ds[short_name]

        # 处理时间信息
        forecast_time, valid_time = _process_time_info(ds)

        return data, forecast_time, valid_time

    except Exception as e:
        raise ValueError(f"读取地面数据失败: {e}")


def read_upper_air_data(filepath, short_name, pressure_level):
    """
    读取高空数据

    参数:
        filepath (str): GRIB文件路径
        short_name (str): 变量短名(如'r'表示相对湿度)
        pressure_level (int): 气压层(如500表示500hPa)

    返回:
        tuple: (data, forecast_time, valid_time)
    """
    try:
        ds = xr.open_dataset(
            filepath,
            engine='cfgrib',
            backend_kwargs={
                'filter_by_keys': {
                    'shortName': short_name,
                    'typeOfLevel': 'isobaricInhPa'
                }
            }
        )

        # 选择指定气压层
        data = ds[short_name].sel(isobaricInhPa=pressure_level)

        # 特殊处理位势高度(转换为位势什米)
        if short_name == 'gh':
            data = data / 10

        # 处理时间信息
        forecast_time, valid_time = _process_time_info(ds)

        return data, forecast_time, valid_time

    except Exception as e:
        raise ValueError(f"读取高空数据失败: {e}")


def _process_time_info(dataset):
    """处理时间信息(内部函数)"""
    forecast_time_utc = pd.to_datetime(str(dataset.time.values))
    valid_time_utc = pd.to_datetime(str(dataset.valid_time.values))

    # UTC转北京时间(UTC+8)
    forecast_time = forecast_time_utc.tz_localize('UTC').tz_convert('Asia/Shanghai')
    valid_time = valid_time_utc.tz_localize('UTC').tz_convert('Asia/Shanghai')

    # 格式化输出
    forecast_time_str = forecast_time.strftime('%Y年%m月%d日%H时')
    valid_time_str = valid_time.strftime('%Y年%m月%d日%H时')

    return forecast_time_str, valid_time_str