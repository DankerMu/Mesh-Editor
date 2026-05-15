package com.forecast.dao;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.check.pojo.CheckDataIndbEntity;
import com.check.pojo.CheckDataParams;
import com.check.pojo.OrgNafpDataEntity;
import com.check.pojo.OrgNafpDataParams;
import com.forecast.pojo.FcDateTimeEntity;
import com.forecast.pojo.StationForecastDataEntity;
import com.forecast.pojo.StationForecastDataParams;
import com.station.pojo.StationEntity;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StationForecastDataMapper {
    IPage<StationForecastDataEntity> queryStationForecastByPage(IPage page, StationForecastDataParams params);
    List<StationForecastDataEntity> queryStationForecast(StationForecastDataParams params);
    List<StationForecastDataEntity> queryStationForecastDownload(StationForecastDataParams params);
    List<StationForecastDataEntity> queryOrgStationForecast(StationForecastDataParams params);
    List<StationForecastDataEntity> queryStationForecastCheck(CheckDataParams params);
    List<StationForecastDataEntity> queryStationForecastCheckRain(CheckDataParams params);
    List<StationForecastDataEntity> queryStationForecastCheckDay(CheckDataParams params);
    List<StationForecastDataEntity> queryStationForecastCheckDayRain(CheckDataParams params);
    List<StationForecastDataEntity> queryOrgStationForecastCheck(CheckDataParams params);
    List<StationForecastDataEntity> queryOrgStationForecastCheckDay(CheckDataParams params);
    List<StationForecastDataEntity> queryOrgStationForecastCheckDayRain(CheckDataParams params);

    List<StationEntity> queryForecastStations(@Param("params") CheckDataParams params);
    List<StationEntity> queryForecastAllStations(@Param("params") CheckDataParams params);
    List<StationEntity> queryForecastAllWzdStations(@Param("params") CheckDataParams params);
    List<StationEntity> queryForecastStationsByZone(@Param("params") CheckDataParams params);
    List<StationEntity> queryForecastStationsByZone3(@Param("params") CheckDataParams params);
    List<StationEntity> queryForecastAllStationsLonLat(@Param("params") CheckDataParams params);

    List<OrgNafpDataEntity> queryOrgNafpDatas(@Param("params") OrgNafpDataParams params);
    
    int addCheckValue(CheckDataIndbEntity data);
    int updateCheckValue(CheckDataIndbEntity data);
    int updateStationCheckRainValue(CheckDataIndbEntity data);
    List<CheckDataIndbEntity> queryCheckValue(CheckDataIndbEntity data);
    
    List<CheckDataIndbEntity> queryStationCheckHourDataFromDb(CheckDataParams params);
    List<CheckDataIndbEntity> queryStationCheckHourDataFromDbHour(CheckDataParams params);
    
    List<CheckDataIndbEntity> queryStationCheckDataDay(CheckDataParams params);
    int addStationCheckValueMonth(CheckDataIndbEntity data);
    List<CheckDataIndbEntity> queryGribCheckDataDay(CheckDataParams params);
    int addGribCheckValueMonth(CheckDataIndbEntity data);
    
    List<FcDateTimeEntity> queryFcDateTime();
}
