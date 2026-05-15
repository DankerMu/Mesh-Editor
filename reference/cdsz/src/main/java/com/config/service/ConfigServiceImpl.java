package com.config.service;

import com.config.dao.ConfigMapper;
import com.config.pojo.*;
import com.config.service.inf.ConfigService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @category
 * @date 2025/3/18 13:24
 * @description TODO
 */
@Service
public class ConfigServiceImpl implements ConfigService {
    @Resource
    private ConfigMapper configMapper;
    @Override
    public List<String> getVtisByDataSource(String dataSource) {
        List<String> result = new ArrayList<>();
        List<VitsEntity> vtisByDataSource = configMapper.getVtisByDataSource(dataSource);
        for(VitsEntity vitsEntity : vtisByDataSource)
        {
            result.add(vitsEntity.getVtis());
        }

        return result;
    }

    @Override
    public List<String> getDataSourcesName(String model) {
        List<String> result = new ArrayList<>();
        List<DataSourceNameEntity> dataSourcesName = configMapper.getDataSourcesName(model);
        for(DataSourceNameEntity dataSourceNameEntity : dataSourcesName)
        {
            result.add(dataSourceNameEntity.getDataSource());
        }

        return result;
    }

    @Override
    public List<String> getDataSources() {

        List<String> result = new ArrayList<>();
        List<DataSourceNameEntity> dataSources = configMapper.getDataSources();
        for(DataSourceNameEntity dataSourceNameEntity : dataSources)
        {
            result.add(dataSourceNameEntity.getDataSource());
        }

        return result;
    }

    @Override
    public String getStationForecastHeader(ConfigParams params)
    {
        StringBuilder result = new StringBuilder();
        List<HeaderEntity> stationForecastHeader = configMapper.getStationForecastHeader(params);
        String unit = null;
        for(HeaderEntity header : stationForecastHeader)
        {
            if(header.getElementUnit() != null && header.getElementUnit().length() > 0)
            {
                unit = "(" + header.getElementUnit() + ")";
            }
            else
            {
                unit = "";
            }
            result.append(header.getElementName() + unit);
            result.append(",");
        }

        return result.substring(0, result.length()-1);
    }

    @Override
    public List<HeaderEntity> getStationForecastCmpHeader(ConfigParams params) {
        List<HeaderEntity> stationForecastHeader = configMapper.getStationForecastHeader(params);
        List<HeaderEntity> result = new ArrayList<>();
        for(HeaderEntity header : stationForecastHeader)
        {
            if(header.getIndex() > 1)
            {
                result.add(header);
            }
        }
        return  result;
    }

    @Override
    public List<HeaderEntity> getStationForecastCheckElements() {
        List<HeaderEntity> stationForecastCheckElements = configMapper.getStationForecastCheckElements();
        List<HeaderEntity> result = new ArrayList<>();
        for(HeaderEntity header : stationForecastCheckElements)
        {
            if(header.getIndex() > 1)
            {
                result.add(header);
            }
        }
        return  result;
    }

    @Override
    public List<MethodEntity> getCheckDataMethod() {
        List<MethodEntity> result = configMapper.getCheckDataMethod();

        return  result;
    }

    @Override
    public List<DataSourceType> queryDataQcDataType() {
        List<DataSourceType> dataSourceTypes = configMapper.queryDataQcDataType();

        return dataSourceTypes;
    }

    @Override
    public List<ModelTypeEntity> queryModelType() {
        List<ModelTypeEntity> modelTypeEntities = configMapper.queryModelType();

        return modelTypeEntities;
    }

    @Override
    public List<ElementEntity> getElementsByDataType(ConfigParams params) {
        List<ElementEntity> elementEntityList = configMapper.getElementsByDataType(params);

        return elementEntityList;
    }

    @Override
    public String[] getMethodByElement(ConfigParams params) {
        List<MethodEntity> methodEntityList = configMapper.getMethodByElement(params);
        String[] result = null;
        if(methodEntityList != null && methodEntityList.size() > 0)
        {
            result = methodEntityList.get(0).getMethod().split(",");
        }

        return result;
    }

    @Override
    public String[] getDisVtiByElement(ConfigParams params) {
        List<VitsEntity> list = configMapper.getDisVtiByElement(params);
        String[] result = null;
        if(list != null && list.size() > 0)
        {
            result = list.get(0).getVtis().split(",");
        }

        return result;
    }

    @Override
    public List<DataSourceType> getDataSourceByDataType(ConfigParams params) {
        List<DataSourceType> list = configMapper.getDataSourceByDataType(params);

        return list;
    }

    @Override
    public List<DataZoneEntity> getDataZone() {
        List<DataZoneEntity> result = configMapper.getDataZone();

        return result;
    }

    @Override
    public List<ZoneStationEntity> getZoneStations(ConfigParams params) {
        List<ZoneStationEntity> zoneStations = configMapper.getZoneStations(params);

        return zoneStations;
    }
}
