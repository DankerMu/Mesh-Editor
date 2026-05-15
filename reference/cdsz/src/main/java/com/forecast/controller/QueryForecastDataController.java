package com.forecast.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.forecast.pojo.GribForecastDataEntity;
import com.forecast.pojo.GribForecastDataParams;
import com.forecast.pojo.StationForecastDataParams;
import com.forecast.service.inf.QueryForecastDataService;
import com.log.pojo.LogRecordParams;
import com.log.service.inf.LogService;
import com.tool.PageResult;

/**
 * @category
 * @date 2025/3/13 17:27
 * @description TODO
 */
@RestController
@RequestMapping("/forecast/")
@Api(tags = "预报产品展示")
public class QueryForecastDataController {
    @Resource
    private QueryForecastDataService queryStationForecastDataService;
    @Resource
    private QueryForecastDataService queryGribForecastDataService;
    @Resource
    private LogService logService;

    @ApiOperation("站点预报展示，分页查询")
    @PostMapping("queryStationForecastData")
    public PageResult queryStationForecastData(@RequestBody StationForecastDataParams params) {
        try {
            return queryStationForecastDataService.queryStationForecastByPage(params);
        } catch (Exception e) {
            e.printStackTrace();
            return PageResult.fail(e.getMessage());
        }
    }

    @ApiOperation("站点预报查询，按天返回数据   预报展示模块")
    @PostMapping("queryGribForecastDataMap")
    public Map<String, Map<String, List<String>>> queryGribForecastDataMap(@RequestBody StationForecastDataParams params) {
        Map<String, Map<String, List<String>>> map = queryStationForecastDataService.queryStationForecast(params);
        LogRecordParams logRecordParams = new LogRecordParams("预报展示", "站点预报数据查询");
        logService.addLogRecord(logRecordParams);

        return map;
    }

    @ApiOperation("格点预报展示")
    @PostMapping("queryGribForecast")
    public List<GribForecastDataEntity> queryGribForecast(@RequestBody GribForecastDataParams params) {

    	List<GribForecastDataEntity> result = queryGribForecastDataService.queryGribForecast(params);
    	LogRecordParams logRecordParams = new LogRecordParams("预报展示", "格点预报数据查询");
        logService.addLogRecord(logRecordParams);
    	
    	
        return result;
    }
}
