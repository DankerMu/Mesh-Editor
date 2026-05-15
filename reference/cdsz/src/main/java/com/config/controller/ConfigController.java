package com.config.controller;

import com.config.pojo.*;
import com.config.service.inf.ConfigService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

/**
 * @category
 * @date 2025/3/18 13:31
 * @description TODO
 */
@RestController
@RequestMapping("/config/")
@Api(tags = "配置信息接口")
public class ConfigController {
    @Resource
    private ConfigService configService;
    @Qualifier("dataSource")
    @Autowired
    private DataSource dataSource;

    @ApiOperation("预报时效查询")
    @PostMapping("getVtisByDataSource")
    public List<String> getVtisByDataSource(@RequestBody String dataSource)
    {
        List<String> result = configService.getVtisByDataSource(dataSource);

        return result;
    }

    @ApiOperation("数据源名称查询")
    @PostMapping("getDataSourcesName")
    public List<String> getDataSourcesName(@RequestBody String model)
    {
        List<String> result = configService.getDataSourcesName(model);

        return result;
    }

    @ApiOperation("获取数据源")
    @PostMapping("getDataSources")
    public List<String> getDataSources()
    {
        return configService.getDataSources();
    }

    @ApiOperation("预报产品展示，站点预报数据表头")
    @PostMapping("getStationForecastHeader")
    public String getStationForecastHeader(@RequestBody ConfigParams params)
    {
        String result = configService.getStationForecastHeader(params);

        return result;
    }

    @ApiOperation("获取站点数据对比要素")
    @PostMapping("getStationForecastCmpHeader")
    public List<HeaderEntity> getStationForecastCmpHeader(@RequestBody ConfigParams params)
    {
        return configService.getStationForecastCmpHeader(params);
    }

    @ApiOperation("获取站点数据分析评估要素")
    @GetMapping("getStationForecastCheckElements")
    public List<HeaderEntity> getStationForecastCheckElements()
    {
        return configService.getStationForecastCheckElements();
    }

    @ApiOperation("获取站点数据分析评估方法")
    @GetMapping("getStationForecastCheckMethods")
    public List<MethodEntity> getStationForecastCheckMethods()
    {
        List<MethodEntity> result = configService.getCheckDataMethod();

        return result;
    }

    @ApiOperation("获取数据质量管理数据类型")
    @GetMapping("queryDataQcDataType")
    public List<DataSourceType> queryDataQcDataType()
    {
        List<DataSourceType> dataSourceTypes = configService.queryDataQcDataType();

        return dataSourceTypes;
    }

    @ApiOperation("获取创建模型下拉框中的模型类型")
    @GetMapping("queryModelType")
    public List<ModelTypeEntity> queryModelType()
    {
        List<ModelTypeEntity> modelTypeEntities = configService.queryModelType();

        return modelTypeEntities;
    }

    @ApiOperation("根据数据类型获取需要检验的要素，类型是站点和格点    预报数据检验模块")
    @PostMapping("getElementsByDataType")
    public List<ElementEntity> getElementsByDataType(@RequestBody ConfigParams params)
    {
        List<ElementEntity> elementEntityList = configService.getElementsByDataType(params);

        return elementEntityList;
    }

    @ApiOperation("根据要素获取检验方法    预报数据检验模块")
    @PostMapping("getMethodByElement")
    public String[] getMethodByElement(@RequestBody ConfigParams params)
    {
        String[] methodByElement = configService.getMethodByElement(params);

        return methodByElement;
    }

    @ApiOperation("根据要素获取预报时效间隔    预报数据检验模块")
    @PostMapping("getDisVtiByElement")
    public String[] getDisVtiByElement(@RequestBody ConfigParams params)
    {
        String[] vtis = configService.getDisVtiByElement(params);

        return vtis;
    }

    @ApiOperation("根据数据类型获取数据源，数据类型是原始数据和自产数据    预报数据检验模块")
    @PostMapping("getDataSourceByDataType")
    public List<DataSourceType> getDataSourceByDataType(@RequestBody ConfigParams params)
    {
        List<DataSourceType> list = configService.getDataSourceByDataType(params);

        return list;
    }

    @ApiOperation("获取所有区域名称    预报数据检验模块")
    @GetMapping("getDataZone")
    public List<DataZoneEntity> getDataZone()
    {
        List<DataZoneEntity> result = configService.getDataZone();

        return result;
    }

    @ApiOperation("获取指定区域内的站点    预报数据检验模块")
    @PostMapping("getZoneStations")
    public List<ZoneStationEntity> getZoneStations(@RequestBody ConfigParams params)
    {
        List<ZoneStationEntity> zoneStations = configService.getZoneStations(params);

        return zoneStations;
    }
}
