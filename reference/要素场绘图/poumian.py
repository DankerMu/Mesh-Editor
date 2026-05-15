import os
import datetime
import numpy as np
import matplotlib.pyplot as plt
import matplotlib.dates as mdates
from matplotlib.colors import ListedColormap
import meteva.base as meb
from mpl_toolkits.axes_grid1 import make_axes_locatable

# --- 配置部分 ---
# 定义 GDS 上数据的根路径 (本地文件系统路径)
LOCAL_GDS_DATA_ROOT = "D:/ECMWF_HR"  # TODO: 修改为您的本地数据根目录

# 定义每个气象要素相对于根路径的路径
ELEMENT_PATHS = {
    'wind': '/WIND',
    'vvel': '/VVEL',
    'rh': '/RH',
    'tmp': '/TMP',
    'tmp_2m': '/TMP_2M',
    'rain06': '/RAIN06'
}

# 定义高空要素的压力层 (单位: hPa)
PRESSURE_LEVELS = [925, 850, 700, 500, 400, 100]

# 站点信息：名称 -> (经度, 纬度)
STATIONS_INFO = {
    # 新疆
    "乌鲁木齐": (87.6177, 43.7928),
    "和田": (79.9167, 37.1167),
    "喀什": (75.9895, 39.4707),
    "哈密": (93.5132, 42.8331),
    "伊宁": (81.3167, 43.9167),
    "塔城": (82.9833, 46.7333),
    "阿勒泰": (88.1333, 47.8500),
    "博乐": (82.0667, 44.9000),
    "昌吉": (87.3000, 44.0167),
    "阿图什": (76.1667, 40.0167),
    "库尔勒": (86.1500, 41.7667),
    "阿克苏": (80.2667, 41.1667),
    "吐鲁番": (89.1833, 42.9333),
    "石河子": (86.0333, 44.3000),

    # 西藏
    "拉萨": (91.1322, 29.6604),
    "日喀则": (88.8833, 29.2667),
    "山南": (91.7667, 29.2333),
    "林芝": (94.3667, 29.6500),
    "昌都": (97.1667, 31.1333),
    "那曲": (92.0500, 31.4833),
    "噶尔": (80.0833, 32.5000),

    # 甘肃
    "兰州": (103.8343, 36.0611),
    "嘉峪关": (98.2833, 39.7833),
    "金昌": (102.1833, 38.5167),
    "白银": (104.1833, 36.5500),
    "天水": (105.7167, 34.5833),
    "武威": (102.6333, 37.9333),
    "张掖": (100.4500, 38.9333),
    "平凉": (106.6833, 35.5333),
    "酒泉": (98.5000, 39.7500),
    "庆阳": (107.6333, 35.7333),
    "定西": (104.6167, 35.5833),
    "陇南": (104.9167, 33.4000),
    "合作": (102.9167, 34.9833),
    "临夏": (103.2167, 35.6000),

    # 青海
    "西宁": (101.7789, 36.6239),
    "海东": (102.1000, 36.5000),
    "共和": (100.6167, 36.2833),
    "德令哈": (97.3667, 37.3667),
    "同仁": (102.0167, 35.9167),
    "玛沁": (100.2333, 34.4667),
    "玉树": (97.0167, 33.0000),

    # 宁夏
    "银川": (106.2782, 38.4664),
    "石嘴山": (106.3833, 39.0333),
    "吴忠": (106.1833, 37.9833),
    "固原": (106.2833, 36.0000),
    "中卫": (105.1833, 37.5167),

    # 四川
    "成都": (104.0667, 30.6667),
    "自贡": (104.7778, 29.3397),
    "攀枝花": (101.7167, 26.5833),
    "泸州": (105.4439, 28.8728),
    "德阳": (104.3986, 31.1270),
    "绵阳": (104.7397, 31.4714),
    "广元": (105.8297, 32.4353),
    "遂宁": (105.5739, 30.5100),
    "内江": (105.0578, 29.5833),
    "乐山": (103.7667, 29.5833),
    "南充": (106.1106, 30.8378),
    "眉山": (103.8333, 30.0500),
    "宜宾": (104.6400, 28.7556),
    "广安": (106.6333, 30.4500),
    "达州": (107.5000, 31.2167),
    "雅安": (103.0000, 29.9833),
    "巴中": (106.7500, 31.8500),
    "资阳": (104.6400, 30.1297),
    "马尔康": (102.2167, 31.9000),
    "康定": (101.9667, 30.0500),
    "西昌": (102.2667, 27.8833),

    # 重庆（指定三个点）
    "沙坪坝": (106.4500, 29.5333),
    "酉阳": (108.8000, 28.8500),
    "万州": (108.4000, 30.8000),
}

# 站点 -> 省份映射
STATION_PROVINCE_MAP = {
    # 新疆
    "乌鲁木齐": "新疆", "和田": "新疆", "喀什": "新疆", "哈密": "新疆",
    "伊宁": "新疆", "塔城": "新疆", "阿勒泰": "新疆", "博乐": "新疆",
    "昌吉": "新疆", "阿图什": "新疆", "库尔勒": "新疆", "阿克苏": "新疆",
    "吐鲁番": "新疆", "石河子": "新疆",

    # 西藏
    "拉萨": "西藏", "日喀则": "西藏", "山南": "西藏", "林芝": "西藏",
    "昌都": "西藏", "那曲": "西藏", "噶尔": "西藏",

    # 甘肃
    "兰州": "甘肃", "嘉峪关": "甘肃", "金昌": "甘肃", "白银": "甘肃",
    "天水": "甘肃", "武威": "甘肃", "张掖": "甘肃", "平凉": "甘肃",
    "酒泉": "甘肃", "庆阳": "甘肃", "定西": "甘肃", "陇南": "甘肃",
    "合作": "甘肃", "临夏": "甘肃",

    # 青海
    "西宁": "青海", "海东": "青海", "共和": "青海", "德令哈": "青海",
    "同仁": "青海", "玛沁": "青海", "玉树": "青海",

    # 宁夏
    "银川": "宁夏", "石嘴山": "宁夏", "吴忠": "宁夏", "固原": "宁夏",
    "中卫": "宁夏",

    # 四川
    "成都": "四川", "自贡": "四川", "攀枝花": "四川", "泸州": "四川",
    "德阳": "四川", "绵阳": "四川", "广元": "四川", "遂宁": "四川",
    "内江": "四川", "乐山": "四川", "南充": "四川", "眉山": "四川",
    "宜宾": "四川", "广安": "四川", "达州": "四川", "雅安": "四川",
    "巴中": "四川", "资阳": "四川", "马尔康": "四川", "康定": "四川",
    "西昌": "四川",

    # 重庆
    "沙坪坝": "重庆", "酉阳": "重庆", "万州": "重庆",
}

# 定义绘图的时间范围 (预报时效，单位：小时)
FORECAST_START_HOUR = 0
FORECAST_END_HOUR = 240
FORECAST_INTERVAL = 6

# 绘图样式配置
PLOT_CONFIG = {
    'figsize': (14, 10),
    'dpi': 150,
    'upper_panel_height_ratio': 0.7,
    'lower_panel_height_ratio': 0.3,

    # 上部剖面图样式
    'rh_cmap': 'Blues',
    'tmp_color': 'red',
    'tmp_linewidth': 1.5,
    'vvel_color': 'gray',
    'vvel_linewidth': 1.0,
    'wind_barb_length': 5,

    # 下部地面要素时间序列图样式
    't2m_color': 'red',
    't2m_linewidth': 1.5,
    't2m_marker': 'o',  # 温度曲线数据点标记样式
    't2m_markersize': 4,  # 标记点大小
    't2m_markeredgecolor': 'red',  # 标记点边缘颜色
    't2m_markerfacecolor': 'white',  # 标记点填充颜色
    'rain06_color': 'green',
    'rain06_alpha': 0.7,

    # 坐标轴和标签
    'time_format_major': '%m/%d\n%HZ',
    'xlabel_fontsize': 10,
    'ylabel_fontsize': 10,
    'title_fontsize': 12,
    'tick_label_fontsize': 9,
}


# --- 数据读取和缓存函数 ---

def construct_local_filepath(element, level, init_time_dt, forecast_hour):
    """
    根据要素、层次、起报时间和预报时效，在本地文件系统中构造完整的文件路径。
    """
    base_path = LOCAL_GDS_DATA_ROOT
    element_path = ELEMENT_PATHS[element]
    init_time_str = init_time_dt.strftime("%Y%m%d%H")
    filename = init_time_dt.strftime("%y%m%d%H") + f".{forecast_hour:03d}"

    if element in ['tmp_2m', 'rain06']:
        # 地面要素路径不包含层次文件夹
        full_path = os.path.join(base_path + element_path, init_time_str, filename)
    else:
        # 所有其他要素（包括 wind, tmp, rh, vvel）都按层次存放
        if level is None:
            raise ValueError(f"要素 {element} 是高空要素，必须提供 pressure level！")
        full_path = os.path.join(base_path + element_path, str(level), init_time_str, filename)
    return full_path


def read_grid_data(filepath, cache_dict=None):
    """
    读取单个格点数据文件，支持缓存
    """
    if cache_dict is not None and filepath in cache_dict:
        return cache_dict[filepath]

    try:
        if os.path.exists(filepath):
            data = meb.read_griddata_from_gds_file(filepath)
            if cache_dict is not None:
                cache_dict[filepath] = data
            return data
        else:
            print(f"警告: 文件不存在 {filepath}")
            return None
    except Exception as e:
        print(f"警告: 读取文件 {filepath} 失败: {e}")
        return None


def read_wind_data(filepath, cache_dict=None):
    """
    读取单个风场数据文件，支持缓存
    """
    if cache_dict is not None and filepath in cache_dict:
        return cache_dict[filepath]

    try:
        if os.path.exists(filepath):
            data = meb.read_gridwind_from_gds_file(filepath)
            if cache_dict is not None:
                cache_dict[filepath] = data
            return data
        else:
            print(f"警告: 文件不存在 {filepath}")
            return None
    except Exception as e:
        print(f"警告: 读取风场文件 {filepath} 失败: {e}")
        return None


def load_all_stations_data(init_time_dt):
    """
    一次性加载所有站点所需的数据
    返回结构：{
        'tmp': {station_name: {level: np.array}},
        'rh': {station_name: {level: np.array}},
        'vvel': {station_name: {level: np.array}},
        'wind': {station_name: {'u': {level: np.array}, 'v': {level: np.array}}},
        'tmp_2m': {station_name: np.array},
        'rain06': {station_name: np.array}
    }
    """
    print(f"开始加载所有站点数据，起报时间: {init_time_dt.strftime('%Y-%m-%d %HZ')}")

    # 初始化数据结构
    all_data = {
        'tmp': {},
        'rh': {},
        'vvel': {},
        'wind': {},
        'tmp_2m': {},
        'rain06': {}
    }

    forecast_hours = list(range(FORECAST_START_HOUR, FORECAST_END_HOUR + 1, FORECAST_INTERVAL))
    n_timesteps = len(forecast_hours)

    # 创建缓存字典，避免重复读取同一文件
    grid_cache = {}
    wind_cache = {}

    # 为每个站点初始化数据结构
    for station_name, (lon, lat) in STATIONS_INFO.items():
        # 高空要素
        for element in ['tmp', 'rh', 'vvel']:
            all_data[element][station_name] = {level: np.full(n_timesteps, np.nan) for level in PRESSURE_LEVELS}

        # 风场
        all_data['wind'][station_name] = {
            'u': {level: np.full(n_timesteps, np.nan) for level in PRESSURE_LEVELS},
            'v': {level: np.full(n_timesteps, np.nan) for level in PRESSURE_LEVELS}
        }

        # 地面要素
        all_data['tmp_2m'][station_name] = np.full(n_timesteps, np.nan)
        all_data['rain06'][station_name] = np.full(n_timesteps, np.nan)

    print(f"数据结构初始化完成，共 {len(STATIONS_INFO)} 个站点")

    # 按要素和时效加载数据
    for fh_idx, forecast_hour in enumerate(forecast_hours):
        print(f"处理预报时效 T+{forecast_hour:03d}h ({fh_idx + 1}/{n_timesteps})")

        # 1. 加载高空要素数据
        for element in ['tmp', 'rh', 'vvel']:
            for level in PRESSURE_LEVELS:
                filepath = construct_local_filepath(element, level, init_time_dt, forecast_hour)
                data = read_grid_data(filepath, grid_cache)

                if data is not None:
                    # 为所有站点插值这个时次的数据
                    for station_name, (lon, lat) in STATIONS_INFO.items():
                        try:
                            interpolated_val = float(data.interp(lon=lon, lat=lat, method='linear').squeeze())
                            all_data[element][station_name][level][fh_idx] = interpolated_val
                        except Exception as e:
                            print(f"警告: 插值 {element}@{level}hPa@{station_name}@T+{forecast_hour}h 失败: {e}")

        # 2. 加载风场数据
        for level in PRESSURE_LEVELS:
            filepath = construct_local_filepath('wind', level, init_time_dt, forecast_hour)
            wind_data = read_wind_data(filepath, wind_cache)

            if wind_data is not None and hasattr(wind_data, 'isel') and 'member' in wind_data.coords:
                # 为所有站点插值这个时次的风场数据
                for station_name, (lon, lat) in STATIONS_INFO.items():
                    try:
                        u_data = wind_data.isel(member=1)
                        v_data = wind_data.isel(member=0)
                        u_interp = float(u_data.interp(lon=lon, lat=lat, method='linear').squeeze())
                        v_interp = float(v_data.interp(lon=lon, lat=lat, method='linear').squeeze())
                        all_data['wind'][station_name]['u'][level][fh_idx] = u_interp
                        all_data['wind'][station_name]['v'][level][fh_idx] = v_interp
                    except Exception as e:
                        print(f"警告: 插值 wind@{level}hPa@{station_name}@T+{forecast_hour}h 失败: {e}")

        # 3. 加载地面要素数据
        for element in ['tmp_2m', 'rain06']:
            filepath = construct_local_filepath(element, None, init_time_dt, forecast_hour)
            data = read_grid_data(filepath, grid_cache)

            if data is not None:
                for station_name, (lon, lat) in STATIONS_INFO.items():
                    try:
                        interpolated_val = float(data.interp(lon=lon, lat=lat, method='linear').squeeze())
                        all_data[element][station_name][fh_idx] = interpolated_val
                    except Exception as e:
                        print(f"警告: 插值 {element}@{station_name}@T+{forecast_hour}h 失败: {e}")

    print("所有站点数据加载完成")
    return all_data


# --- 绘图函数 ---

def create_profile_plot_from_preloaded(station_name, lon, lat, init_time_dt, station_data):
    """
    使用预加载的数据为单个站点生成剖面图
    """
    print(f"\n为站点 '{station_name}' ({lon:.2f}E, {lat:.2f}N) 生成剖面图...")

    # 准备时间轴
    forecast_hours = np.arange(FORECAST_START_HOUR, FORECAST_END_HOUR + 1, FORECAST_INTERVAL)
    valid_times = [init_time_dt + datetime.timedelta(hours=int(fh)) for fh in forecast_hours]
    valid_times_mdates = mdates.date2num(valid_times)

    # 从预加载数据中提取该站点的数据
    tmp_data = station_data['tmp'][station_name]
    rh_data = station_data['rh'][station_name]
    vvel_data = station_data['vvel'][station_name]
    wind_data = station_data['wind'][station_name]
    t2m_data = station_data['tmp_2m'][station_name]
    rain06_data = station_data['rain06'][station_name]

    # 对降水数据处理：先保留1位小数，再清除<0.1的值
    rain06_data = np.round(rain06_data, 1)
    rain06_data = np.where(rain06_data < 0.1, 0.0, rain06_data)

    # 创建图形
    fig = plt.figure(figsize=PLOT_CONFIG['figsize'], dpi=PLOT_CONFIG['dpi'])
    plt.subplots_adjust(top=0.94)

    gs = fig.add_gridspec(2, 1,
                          height_ratios=[PLOT_CONFIG['upper_panel_height_ratio'],
                                         PLOT_CONFIG['lower_panel_height_ratio']],
                          hspace=0.15)

    ax_upper = fig.add_subplot(gs[0])

    # 构造数据网格
    X_times_mesh, Y_levels_mesh = np.meshgrid(valid_times_mdates, PRESSURE_LEVELS)

    # 自定义颜色映射，使相对湿度50%以下为白色
    orig_cmap = plt.get_cmap(PLOT_CONFIG['rh_cmap'])
    new_colors = orig_cmap(np.linspace(0, 1, orig_cmap.N))
    new_colors[:int(orig_cmap.N * 0.5), :] = [1, 1, 1, 1]
    custom_cmap = ListedColormap(new_colors)

    Z_rh = np.array([rh_data[level] for level in PRESSURE_LEVELS])

    cf = ax_upper.contourf(X_times_mesh, Y_levels_mesh, Z_rh,
                           levels=np.arange(0, 105, 10),
                           cmap=custom_cmap,
                           extend='max')

    # 添加相对湿度的颜色条
    divider = make_axes_locatable(ax_upper)
    cax = divider.append_axes("bottom", size="3%", pad=0.3)
    cbar = fig.colorbar(cf, cax=cax, orientation='horizontal')
    cbar.set_label('相对湿度 (%)', fontsize=PLOT_CONFIG['tick_label_fontsize'])
    cbar.ax.tick_params(labelsize=PLOT_CONFIG['tick_label_fontsize'] - 1)
    cbar.set_ticks(np.arange(0, 110, 10))
    cbar.set_ticklabels(['{:.0f}%'.format(x) for x in np.arange(0, 110, 10)])

    # 绘制温度等值线
    Z_tmp = np.array([tmp_data[level] for level in PRESSURE_LEVELS])
    cs_tmp = ax_upper.contour(X_times_mesh, Y_levels_mesh, Z_tmp,
                              levels=15,
                              colors=PLOT_CONFIG['tmp_color'],
                              linewidths=PLOT_CONFIG['tmp_linewidth'])
    ax_upper.clabel(cs_tmp, inline=True,
                    fontsize=PLOT_CONFIG['tick_label_fontsize'] - 1,
                    fmt='%d')

    # 绘制垂直速度等值线
    Z_vvel = np.array([vvel_data[level] for level in PRESSURE_LEVELS])

    vmin, vmax = np.nanmin(Z_vvel), np.nanmax(Z_vvel)
    if not (np.isnan(vmin) or np.isnan(vmax)):
        n_levels = 15
        max_abs = max(abs(vmin), abs(vmax))
        levels = np.linspace(-max_abs, max_abs, n_levels)

        cs_pos = ax_upper.contour(X_times_mesh, Y_levels_mesh, Z_vvel,
                                  levels=[l for l in levels if l > 0],
                                  colors=PLOT_CONFIG['vvel_color'],
                                  linewidths=PLOT_CONFIG['vvel_linewidth'],
                                  linestyles='solid')

        cs_neg = ax_upper.contour(X_times_mesh, Y_levels_mesh, Z_vvel,
                                  levels=[l for l in levels if l < 0],
                                  colors=PLOT_CONFIG['vvel_color'],
                                  linewidths=PLOT_CONFIG['vvel_linewidth'],
                                  linestyles='dashed')

        ax_upper.clabel(cs_pos, inline=True,
                        fontsize=PLOT_CONFIG['tick_label_fontsize'] - 2,
                        fmt='%.1f')
        ax_upper.clabel(cs_neg, inline=True,
                        fontsize=PLOT_CONFIG['tick_label_fontsize'] - 2,
                        fmt='%.1f')

    # 绘制风向杆
    U_wind = np.array([wind_data['u'][level] for level in PRESSURE_LEVELS])
    V_wind = np.array([wind_data['v'][level] for level in PRESSURE_LEVELS])
    ax_upper.barbs(X_times_mesh, Y_levels_mesh,
                   U_wind * (-1), V_wind * (-1),
                   length=PLOT_CONFIG['wind_barb_length'],
                   barb_increments=dict(half=2.5, full=5, flag=25))

    # 反转Y轴（气压从上到下减小）
    ax_upper.invert_yaxis()

    # 设置Y轴范围
    padding = 30
    y_low = min(PRESSURE_LEVELS) - padding
    y_high = max(PRESSURE_LEVELS) + padding
    ax_upper.set_ylim(y_high, y_low)
    ax_upper.set_ylabel('气压 (hPa)', fontsize=PLOT_CONFIG['ylabel_fontsize'])
    ax_upper.set_yticks(PRESSURE_LEVELS)
    ax_upper.set_yticklabels([str(level) for level in PRESSURE_LEVELS])
    ax_upper.xaxis.set_major_formatter(mdates.DateFormatter(PLOT_CONFIG['time_format_major']))
    ax_upper.xaxis.set_major_locator(mdates.HourLocator(interval=24))
    ax_upper.tick_params(axis='x', labelbottom=False)
    ax_upper.tick_params(axis='both', which='major',
                         labelsize=PLOT_CONFIG['tick_label_fontsize'])

    # 下部地面要素时间序列图
    ax_lower = fig.add_subplot(gs[1], sharex=ax_upper)

    # 绘制温度曲线并添加数据点
    line_t2m = ax_lower.plot(valid_times_mdates, t2m_data,
                             color=PLOT_CONFIG['t2m_color'],
                             linewidth=PLOT_CONFIG['t2m_linewidth'],
                             marker=PLOT_CONFIG['t2m_marker'],  # 添加标记点
                             markersize=PLOT_CONFIG['t2m_markersize'],
                             markeredgecolor=PLOT_CONFIG['t2m_markeredgecolor'],
                             markerfacecolor=PLOT_CONFIG['t2m_markerfacecolor'],
                             label='2米温度 (°C)')

    ax_rain = ax_lower.twinx()

    bar_width = 0.2
    bar_rain = ax_rain.bar(valid_times_mdates - bar_width / 2, rain06_data,
                           width=bar_width,
                           color=PLOT_CONFIG['rain06_color'],
                           alpha=PLOT_CONFIG['rain06_alpha'],
                           label='6小时降水量 (mm)',
                           align='center')

    ax_lower.set_ylabel('2米温度 (°C)',
                        color=PLOT_CONFIG['t2m_color'],
                        fontsize=PLOT_CONFIG['ylabel_fontsize'])
    ax_lower.tick_params(axis='y',
                         labelcolor=PLOT_CONFIG['t2m_color'],
                         labelsize=PLOT_CONFIG['tick_label_fontsize'])
    ax_rain.set_ylabel('6小时降水量 (mm)',
                       color=PLOT_CONFIG['rain06_color'],
                       fontsize=PLOT_CONFIG['ylabel_fontsize'])
    ax_rain.tick_params(axis='y',
                        labelcolor=PLOT_CONFIG['rain06_color'],
                        labelsize=PLOT_CONFIG['tick_label_fontsize'])

    ax_lower.xaxis.set_major_formatter(mdates.DateFormatter(PLOT_CONFIG['time_format_major']))
    ax_lower.xaxis.set_major_locator(mdates.HourLocator(interval=24))
    ax_lower.tick_params(axis='x', rotation=0,
                         labelsize=PLOT_CONFIG['tick_label_fontsize'])

    ax_lower.set_xlim(valid_times_mdates.min(), valid_times_mdates.max())
    ax_upper.set_xlim(valid_times_mdates.min(), valid_times_mdates.max())

    ax_lower.grid(True, which="major", ls="--", alpha=0.5)

    lines, labels = ax_lower.get_legend_handles_labels()
    lines2, labels2 = ax_rain.get_legend_handles_labels()
    ax_lower.legend(lines + lines2, labels + labels2,
                    loc='upper left',
                    fontsize=PLOT_CONFIG['tick_label_fontsize'] - 1)

    # 设置标题
    title = f"EC细网格预报 剖面分析 [{station_name} {lon:.2f}E, {lat:.2f}N]\n起报时间: {init_time_dt.strftime('%Y-%m-%d %HZ')}"
    fig.suptitle(title, fontsize=PLOT_CONFIG['title_fontsize'], y=0.98)

    # 反转X轴
    ax_upper.invert_xaxis()

    # 创建按照格式的输出路径
    init_time_str = init_time_dt.strftime("%Y%m%d%H")
    province = STATION_PROVINCE_MAP.get(station_name, "未知")

    # 构建输出目录结构：/images/YYYYMMDDHH/profile/省份/
    output_dir = os.path.join("images", init_time_str, "profile", province)
    os.makedirs(output_dir, exist_ok=True)

    # 构建文件名：profile_省份_站点名_YYYYMMDDHH.png
    filename = f"profile_{province}_{station_name}_{init_time_str}.png"
    filepath = os.path.join(output_dir, filename)

    plt.savefig(filepath, bbox_inches='tight')
    plt.close(fig)
    print(f"图片已保存至: {filepath}")

    return filepath


# --- 主执行逻辑 ---
if __name__ == "__main__":
    # 获取当前本地时间
    now = datetime.datetime.now()

    # 判断当前小时是否小于 15
    if now.hour < 15:
        target_time = (now - datetime.timedelta(days=1)).replace(hour=20, minute=0, second=0, microsecond=0)
    else:
        target_time = now.replace(hour=8, minute=0, second=0, microsecond=0)

    INITIAL_TIME_STR = target_time.strftime("%Y%m%d%H")
    print(f"自动选择起报时间: {target_time.strftime('%Y-%m-%d %HZ')}")

    init_time = datetime.datetime.strptime(INITIAL_TIME_STR, "%Y%m%d%H")

    # --- 一次性加载所有站点数据 ---
    print("=" * 60)
    print("开始一次性加载所有站点数据...")
    print("=" * 60)

    try:
        all_stations_data = load_all_stations_data(init_time)
        print("\n所有站点数据加载完成，开始生成图表...")
        print("=" * 60)
    except Exception as e:
        print(f"加载所有站点数据失败: {e}")
        all_stations_data = None

    # --- 为每个站点生成剖面图 ---
    if all_stations_data is not None:
        success_count = 0
        fail_count = 0

        for station, (lon, lat) in STATIONS_INFO.items():
            try:
                create_profile_plot_from_preloaded(station, lon, lat, init_time, all_stations_data)
                success_count += 1
            except Exception as e:
                print(f"为站点 '{station}' 生成图表时发生错误: {e}")
                fail_count += 1

        print(f"\n图表生成完成: 成功 {success_count} 个, 失败 {fail_count} 个")

        # 显示输出目录信息
        init_time_str = init_time.strftime("%Y%m%d%H")
        print(f"\n图片输出目录结构:")
        print(f"  images/{init_time_str}/profile/省份名/")
        print(f"  例如: images/{init_time_str}/profile/新疆/profile_新疆_喀什_{init_time_str}.png")
    else:
        print("数据加载失败，无法生成图表")

    print("\n程序执行完毕。")