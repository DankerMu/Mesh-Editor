import meteva.base as meb
import matplotlib.pyplot as plt
from matplotlib.font_manager import FontProperties 
from matplotlib.colors import BoundaryNorm
from cartopy.mpl.ticker import LongitudeFormatter, LatitudeFormatter
import geopandas as gpd
import cartopy.crs as ccrs
from datetime import datetime
from datetime import timedelta
from scipy.ndimage import gaussian_filter
import pandas as pd
import os
import numpy as np

def process_time_info(dataset,validtime):
    """处理时间信息(内部函数)"""
    start_time = pd.to_datetime(str(dataset.time.values[0]))
    forecast_time = start_time + timedelta(hours=int(validtime)) 


    # 格式化输出
    start_time_str = start_time.strftime('%Y年%m月%d日%H时')
    forecast_time_str = forecast_time.strftime('%Y年%m月%d日%H时')

    return start_time_str, forecast_time_str

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
        
def plot_rain(year,month,day,hour,validtime1,validtime2,lenth,level,region):
    extent = {'All':[70,115,25,50],
              'NW':[70,105,30,50],
              'SW':[75,100,25,40],
              'East':[90,115,25,40]
        }
    input_file00 = '/cloud/share-files/SC/mdfs/ECMWF_HR/APCP/'+fcst_year+fcst_month+fcst_day+fcst_hour+'/'+fcst_year[2:]+fcst_month+fcst_day+fcst_hour+'.'+validtime1
    input_file01 = '/cloud/share-files/SC/mdfs/ECMWF_HR/APCP/'+fcst_year+fcst_month+fcst_day+fcst_hour+'/'+fcst_year[2:]+fcst_month+fcst_day+fcst_hour+'.'+validtime2
    output_dir = '/gaoli/huishang/static/images/'+fcst_year+fcst_month+fcst_day+fcst_hour+'/'+'rain/'+region+'/'
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)
    output_name = 'rain'+str(lenth)+'_'+region+'_'+str(level)+'hPa_'+fcst_year+fcst_month+fcst_day+fcst_hour+'_'+validtime1+'.png'
    output_file = output_dir + output_name
    print(output_file)
    if os.path.exists(input_file00) and os.path.exists(input_file01) and not os.path.exists(output_file):
        data0 = meb.read_griddata_from_gds_file(input_file00)
        data1 = meb.read_griddata_from_gds_file(input_file01)
        #0.取时间
        start_time, forecast_time = process_time_info(data0,validtime1)
        #1.取经纬度
        lon = data0['lon'].data
        lat = data0['lat'].data
        lons,lats = np.meshgrid(lon,lat)
        #2.取数据并平滑
        data0 = data0.isel(level=0,time=0,dtime=0,member=0).values
        data1 = data1.isel(level=0,time=0,dtime=0,member=0).values
        data = data0 - data1
        smoothed_data = gaussian_filter(data,sigma=1.2)
        WESTERN_PROVINCES = ['新疆维吾尔自治区', '西藏自治区', '青海省',
                    '四川省', '甘肃省', '宁夏回族自治区', '重庆市']
        COUNTRY_BOUNDARY = "/gaoli/huishang/draw/nation.json"
        PROVINCE_BOUNDARY = "/gaoli/huishang/draw/province.json"
        gdf_country, gdf_province = read_map_data(COUNTRY_BOUNDARY, PROVINCE_BOUNDARY, WESTERN_PROVINCES)
        # 3. 绘图设置
        plt.style.use('default')
        plt.rcParams.update({
            'axes.unicode_minus': False,
            'font.size': 12,
            'figure.titlesize': 20,
            'axes.linewidth': 0.8,
            'grid.linewidth': 0.4,
        })    
        font_path = './simhei.ttf'
        font_prop = FontProperties(fname=font_path)
        
        # 4. 创建图形
        fig = plt.figure(figsize=(16, 10))
        ax = fig.add_subplot(111, projection=ccrs.PlateCarree())
        ax.set_extent(extent[region], crs=ccrs.PlateCarree())
        # 5. 定义颜色和等级
        colors = ['#a6f28e', '#3db93d', '#5fb6f8', '#0000fe', '#f900fc', '#a80000']
        levels = [0.1, 10, 25, 50, 100, 250]
        norm = BoundaryNorm(levels, len(colors) - 1)
        # 6. 绘制降水数据
        img = ax.contourf(
            lons,
            lats,
            smoothed_data,
            levels=levels,
            colors=colors,
            transform=ccrs.PlateCarree(),
            extend='max',
            alpha=0.85
        )
        # 7. 添加colorbar
        cbar = fig.colorbar(img, ax=ax, orientation='horizontal',
                           pad=0.08, aspect=50, shrink=0.75,
                           ticks=levels, drawedges=True)
        cbar.set_label('降水量 (mm)', fontsize=14, labelpad=10,fontproperties=font_prop)
        cbar.ax.tick_params(labelsize=12, width=0.5)
        cbar.outline.set_linewidth(0.5)
        # 8. 绘制边界
        gdf_province.plot(
            ax=ax,
            facecolor="none",
            edgecolor="#555555",
            linewidth=0.6,
            linestyle=':',
            alpha=0.7,
            transform=ccrs.PlateCarree()
        )
        # 9. 添加经纬度标注
        
        ax.set_xticks(range(extent[region][0],extent[region][1]+1, 5), crs=ccrs.PlateCarree())
        ax.set_yticks(range(extent[region][2],extent[region][3]+1, 5), crs=ccrs.PlateCarree())
        ax.xaxis.set_major_formatter(LongitudeFormatter(zero_direction_label=True))
        ax.yaxis.set_major_formatter(LatitudeFormatter())
        ax.tick_params(axis='both', which='major', labelsize=11, width=0.5)
        ax.set_aspect('equal')  # 关键设置
        gdf_country.plot(
            ax=ax,
            facecolor="none",
            edgecolor="black",
            linewidth=1.5,
            alpha=0.9,
            transform=ccrs.PlateCarree()
        )
        # 10. 标注城市
        CITIES = {'All':{
            '乌鲁木齐': (87.6168, 43.8256),
            '拉萨': (91.1409, 29.6469),
            '兰州': (103.8342, 36.0611),
            '银川': (106.2309, 38.4872),
            '西宁': (101.7778, 36.6173),
            '成都': (104.0665, 30.5728),
            '重庆': (106.5507, 29.5637)
            },
                  'NW':{
            '乌鲁木齐': (87.6168, 43.8256),
            '兰州': (103.8342, 36.0611),
            '西宁': (101.7778, 36.6173),
            },
                  'SW':{
            '拉萨': (91.1409, 29.6469),
            },
                  'East':{
            '拉萨': (91.1409, 29.6469),
            '兰州': (103.8342, 36.0611),
            '银川': (106.2309, 38.4872),
            '西宁': (101.7778, 36.6173),
            '成都': (104.0665, 30.5728),
            '重庆': (106.5507, 29.5637)
            }
                  }        
        for city, (lon, lat) in CITIES[region].items():
            ax.plot(lon, lat, 'o', markersize=7,
                   markerfacecolor='red', markeredgecolor='white',
                   markeredgewidth=0.5, transform=ccrs.PlateCarree())
            ax.text(lon + 0.2, lat + 0.1, city, fontsize=12,fontproperties=font_prop,
                   color='darkred', weight='bold', ha='left', va='bottom',
                   transform=ccrs.PlateCarree())
        # 11. 设置标题和时间标注
        text = {'All':'战区',
              'NW':'战区西北部',
              'SW':'战区西南部',
              'East':'战区东部'
            }
        ax.set_title(text[region]+str(lenth)+'小时累积降水分布',fontproperties=font_prop,fontsize=20)
        
        # 时间标注
        ax.text(0.02, 0.98, f'起报时间: {start_time}',
               transform=ax.transAxes, ha='left', va='top',fontproperties=font_prop,
               fontsize=12, bbox=dict(facecolor='white', alpha=0.7, edgecolor='none'))
        
        ax.text(0.98, 0.98, f'预报时间: {forecast_time}',
               transform=ax.transAxes, ha='right', va='top',fontproperties=font_prop,
               fontsize=12, bbox=dict(facecolor='white', alpha=0.7, edgecolor='none'))
        
        # 数据来源
        ax.text(0.99, 0.01, '数据来源: ECMWF',
               transform=ax.transAxes, ha='right', va='bottom',fontproperties=font_prop,
               fontsize=10, bbox=dict(facecolor='white', alpha=0.7, edgecolor='none'))
        
        # 12. 保存图形
        plt.savefig(output_file,
                   dpi=350, bbox_inches='tight',
                   facecolor='white')
        plt.close()
    else:
        print('file does not exist!')



t0 = t_now = datetime.now()
print(t0)
now_hour = t_now.hour 
if now_hour<14:
    t_fcst = t_now + timedelta(days=-1)
    fcst_year = str(t_fcst.year)
    fcst_month = str(t_fcst.month).zfill(2)
    fcst_day = str(t_fcst.day).zfill(2)
    fcst_hour = '20'
else:
    t_fcst = t_now
    fcst_year = str(t_fcst.year)
    fcst_month = str(t_fcst.month).zfill(2)
    fcst_day = str(t_fcst.day).zfill(2)
    fcst_hour = '08'

levels = [999]
regions = ['All','NW','SW','East']
for level in levels:
    for lenth in range(24,169,24):
        for h0 in range(lenth,241,12):                        
            for region in regions:
                h1 = h0 - lenth
                hh0 = str(h0).zfill(3)
                hh1 = str(h1).zfill(3)
                plot_rain(fcst_year,fcst_month,fcst_day,fcst_hour,hh0,hh1,lenth,level,region)
t1 = datetime.now()
print((t1-t0)/60)     



