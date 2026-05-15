package com.forecast.service.inf;

import com.check.pojo.CheckDataGribIndbEntity;
import com.check.pojo.CheckDataIndbEntity;
import com.check.pojo.CheckDataParams;
import com.forecast.pojo.GribForecastDataEntity;
import com.forecast.pojo.GribForecastDataParams;
import com.forecast.pojo.StationForecastDataEntity;
import com.forecast.pojo.StationForecastDataParams;
import com.tool.PageResult;

import ucar.nc2.grib.collection.Grib;

import java.util.List;
import java.util.Map;

public interface QueryForecastDataService {
    PageResult queryStationForecastByPage(StationForecastDataParams params);
    Map<String, Map<String, List<String>>> queryStationForecast(StationForecastDataParams params);
    List<GribForecastDataEntity> queryGribForecast(GribForecastDataParams params);
    
    List<CheckDataIndbEntity> queryStationCheckHourDataFromDb(CheckDataParams params);
    List<CheckDataGribIndbEntity> queryGribCheckHourDataFromDb(CheckDataParams params);
}
