package com.config.service.inf;

import com.config.pojo.*;

import java.util.List;

public interface ConfigService {
    List<String> getVtisByDataSource(String dataSource);
    List<String> getDataSourcesName(String model);
    List<String> getDataSources();
    String getStationForecastHeader(ConfigParams params);
    List<HeaderEntity> getStationForecastCmpHeader(ConfigParams params);

    List<HeaderEntity> getStationForecastCheckElements();

    List<MethodEntity> getCheckDataMethod();

    List<DataSourceType> queryDataQcDataType();

    List<ModelTypeEntity> queryModelType();

    List<ElementEntity> getElementsByDataType(ConfigParams params);

    String[] getMethodByElement(ConfigParams params);

    String[] getDisVtiByElement(ConfigParams params);

    List<DataSourceType> getDataSourceByDataType(ConfigParams params);

    List<DataZoneEntity> getDataZone();

    List<ZoneStationEntity> getZoneStations(ConfigParams params);
}
