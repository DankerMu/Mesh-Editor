package com.config.dao;

import com.config.pojo.*;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ConfigMapper {
    List<VitsEntity> getVtisByDataSource(String dataSource);
    List<DataSourceNameEntity> getDataSourcesName(String model);
    List<DataSourceNameEntity> getDataSources();
    List<HeaderEntity> getStationForecastHeader(ConfigParams params);
    List<HeaderEntity> getStationForecastCheckElements();

    List<MethodEntity> getCheckDataMethod();

    List<DataSourceType> queryDataQcDataType();

    List<ModelTypeEntity> queryModelType();

    List<ElementEntity> getElementsByDataType(ConfigParams params);

    List<MethodEntity> getMethodByElement(ConfigParams params);

    List<VitsEntity> getDisVtiByElement(ConfigParams params);

    List<DataSourceType> getDataSourceByDataType(ConfigParams params);

    List<DataZoneEntity> getDataZone();

    List<ZoneStationEntity> getZoneStations(ConfigParams params);
}
