# -*- coding: utf-8 -*-
"""
Created on Mon Jul  7 15:08:14 2025

@author: gaoli
"""
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
        
def plot_circulation(year,month,day,hour,valid_time,level,region):
    extent = {'All':[70,140,15,55],
              'NW':[70,110,30,55],
              'SW':[70,100,20,40],
              'East':[90,125,20,40]
        }
    input_file_hgt = '/cloud/share-files/SC/mdfs/ECMWF_HR/HGT/'+str(level)+'/'+fcst_year+fcst_month+fcst_day+fcst_hour+'/'+fcst_year[2:]+fcst_month+fcst_day+fcst_hour+'.'+valid_time
    input_file_wind = '/cloud/share-files/SC/mdfs/ECMWF_HR/WIND/'+str(level)+'/'+fcst_year+fcst_month+fcst_day+fcst_hour+'/'+fcst_year[2:]+fcst_month+fcst_day+fcst_hour+'.'+valid_time
    input_file_rh = '/cloud/share-files/SC/mdfs/ECMWF_HR/RH/'+str(level)+'/'+fcst_year+fcst_month+fcst_day+fcst_hour+'/'+fcst_year[2:]+fcst_month+fcst_day+fcst_hour+'.'+valid_time
    output_dir = '/gaoli/huishang/static/images/'+fcst_year+fcst_month+fcst_day+fcst_hour+'/'+'circulation'+'/'+region+'/'
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)
    output_name = 'circulation'+'_'+region+'_'+str(level)+'hPa_'+fcst_year+fcst_month+fcst_day+fcst_hour+'_'+valid_time+'.png'
    output_file = output_dir + output_name
    print(output_file)
    if os.path.exists(input_file_hgt) and os.path.exists(input_file_rh) and os.path.exists(input_file_wind) and not os.path.exists(output_file):
        data_hgt = meb.read_griddata_from_gds_file(input_file_hgt)
        data_rh = meb.read_griddata_from_gds_file(input_file_rh)
        data_wind = meb.read_gridwind_from_gds_file(input_file_wind)
        #0.取时间
        start_time, forecast_time = process_time_info(data_hgt,valid_time)
        #1.取经纬度
        lon = data_hgt['lon'].data
        lat = data_hgt['lat'].data
        lons,lats = np.meshgrid(lon,lat)
        #2.取数据
        data_hgt = data_hgt.isel(level=0,time=0,dtime=0,member=0).values
        data_rh = data_rh.isel(level=0,time=0,dtime=0,member=0).values
        data_u = data_wind.isel(level=0,time=0,dtime=0,member=1).values
        data_v = data_wind.isel(level=0,time=0,dtime=0,member=0).values
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
        # 5. 绘制相对湿度填色图（绿色系）
        rh_levels = np.arange(50, 101, 10)
        rh_colors = ['#f0f9e8', '#bae4bc', '#7bccc4', '#43a2ca', '#0868ac']
        rh_plot = ax.contourf(
            lons,lats, data_rh,
            levels=rh_levels,
            colors=rh_colors,
            transform=ccrs.PlateCarree(),
            extend='max',
            alpha=0.85
        )
        # 添加colorbar
        rh_cbar = fig.colorbar(rh_plot, ax=ax, orientation='horizontal',
                              pad=0.05, aspect=50, shrink=0.6)
        rh_cbar.set_label('500hPa 相对湿度 (%)',fontproperties=font_prop, fontsize=12)
        # 6. 绘制位势高度等值线（添加高斯滤波）
        data_hgt = gaussian_filter(data_hgt, sigma=1.2)
        gh_levels = {500:np.arange(540, 600, 4),
                     400:np.arange(700, 780, 4),
                     600:np.arange(400, 460, 4),
                     700:np.arange(280, 340, 4),
                     850:np.arange(120, 160, 4)
                     }
        gh_contour = ax.contour(
        lons,lats,data_hgt,
        levels=gh_levels[level],
        colors='black',
        linewidths=1.2,
        transform=ccrs.PlateCarree()
        )
        # 7. 绘制风场（风向杆）
        skip = {'All':8,
              'NW':6,
              'SW':6,
              'East':6
        }
        lon = lon[::skip[region]]
        lat = lat[::skip[region]]
        lon_grid, lat_grid = np.meshgrid(lon, lat)
        u_sub = data_u[::skip[region], ::skip[region]]
        v_sub = data_v[::skip[region], ::skip[region]]    
        
        ax.barbs(
            lon_grid, lat_grid,  # 经度、纬度（筛选后的点）
            u_sub*(-1), v_sub*(-1),        # U/V风场分量
            length=6,                        # 风杆总长度
            pivot='middle',                  # 旋转支点在中间
            barbcolor='black',               # 风杆颜色
            flagcolor='black',               # 三角旗颜色
            linewidth=0.8,                   # 线宽
            transform=ccrs.PlateCarree(),    # 坐标系
            barb_increments={
                'half': 2,
                'full': 4,
                'flag': 20
            }
        )
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
        ax.set_title(text[region]+str(level)+'hPa环流形式分析',fontproperties=font_prop,fontsize=20)
        
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

levels = [400,500,600,700,850]
regions = ['All','NW','SW','East']
for level in levels:
    for h in range(0,241,6):                        
        for region in regions:
            hh = str(h).zfill(3)
            plot_circulation(fcst_year,fcst_month,fcst_day,fcst_hour,hh,level,region)
t1 = datetime.now()
print((t1-t0)/60)
