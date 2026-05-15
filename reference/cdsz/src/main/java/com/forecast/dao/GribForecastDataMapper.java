package com.forecast.dao;

import com.check.pojo.CheckDataGribIndbEntity;
import com.check.pojo.CheckDataParams;
import com.compare.pojo.CompareDataParams;
import com.forecast.pojo.GribForecastDataEntity;
import com.forecast.pojo.GribForecastDataParams;
import com.forecast.pojo.GribForecastRainValueEntity;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface GribForecastDataMapper {
    List<GribForecastDataEntity> queryGribForecastCheck(CheckDataParams params);
    List<GribForecastRainValueEntity> queryGribForecastRainCheckHour(CheckDataParams params);
    List<GribForecastRainValueEntity> queryGribForecastRainCheckHourOrg(CheckDataParams params);
    List<GribForecastRainValueEntity> queryGribForecastRainCheckDay(CheckDataParams params);
    List<GribForecastRainValueEntity> queryGribForecastRainCheckDayOrg(CheckDataParams params);
    List<GribForecastRainValueEntity> queryGribRainValueCompare(CompareDataParams params);
    List<GribForecastRainValueEntity> queryGribRainValueCompareOrg(CompareDataParams params);
    List<GribForecastDataEntity> queryGribRainCompare(CompareDataParams params);
    List<GribForecastDataEntity> queryGribForecast(GribForecastDataParams params);
    int addCheckGribValue(CheckDataGribIndbEntity data);
    int updateCheckGribValue(CheckDataGribIndbEntity data);
    List<CheckDataGribIndbEntity> queryGribCheckValue(CheckDataGribIndbEntity data);
    
    List<CheckDataGribIndbEntity> queryGribCheckHourDataFromDb(CheckDataParams params);
}
