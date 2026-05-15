package com.check.controller;

import com.check.pojo.CheckDataParams;
import com.check.service.inf.CheckDataService;
import com.log.pojo.LogRecordParams;
import com.log.service.inf.LogService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

import java.util.Map;

/**
 * @category
 * @date 2025/3/19 9:50
 * @description TODO
 */
@RestController
@RequestMapping("/check")
@Api(tags = "预报检验")
public class CheckDataController {
    @Resource
    private CheckDataService checkDataService;
    @Resource
    private LogService logService;
    @ApiOperation("站点评估逐时")
    @PostMapping("checkStationDataHour")
    public Map<String, Map<String, Map<String, Map<String, Double>>>> checkStationDataHour(@RequestBody CheckDataParams param)
    {
//        Map<String, Map<String, Map<String, Map<String, Double>>>> checkDataEntities = checkDataService.checkStationDataHourNew(param);
        Map<String, Map<String, Map<String, Map<String, Double>>>> checkDataEntities = checkDataService.checkStationDataHourJdbc(param);
        LogRecordParams logRecordParams = new LogRecordParams("预报检验", "站点逐时检验数据查询");
        logService.addLogRecord(logRecordParams);
        
//        System.out.println(checkDataEntities);
        return checkDataEntities;
    }

    @ApiOperation("站点评估逐日")
    @PostMapping("checkStationDataDay")
    public Map<String, Map<String, Map<String, Map<String, Double>>>> checkStationDataDay(@RequestBody CheckDataParams param)
    {
//        Map<String, Map<String, Map<String, Map<String, Double>>>> checkDataEntities = checkDataService.checkStationDataDayNew(param);
        Map<String, Map<String, Map<String, Map<String, Double>>>> checkDataEntities = checkDataService.checkStationDataDayJdbc(param);
        LogRecordParams logRecordParams = new LogRecordParams("预报检验", "站点逐日检验数据查询");
        logService.addLogRecord(logRecordParams);
//        System.out.println(checkDataEntities);
        return checkDataEntities;
    }

    @ApiOperation("格点逐时评估")
    @PostMapping("checkGribDataHour")
    public Map<String, Map<String, Map<String, Double>>> checkGribDataHour(@RequestBody CheckDataParams param)
    {
//        Map<String, Map<String, Map<String, Double>>> checkDataEntities = checkDataService.checkGribDataHourNew(param);
        Map<String, Map<String, Map<String, Double>>> checkDataEntities = checkDataService.checkGribDataHourJdbc(param);
        LogRecordParams logRecordParams = new LogRecordParams("预报检验", "格点逐时检验数据查询");
        logService.addLogRecord(logRecordParams);
//        System.out.println(checkDataEntities);
        return checkDataEntities;
    }

    @ApiOperation("格点逐日评估")
    @PostMapping("checkGribDataDay")
    public Map<String, Map<String, Map<String, Double>>> checkGribDataDay(@RequestBody CheckDataParams param)
    {
//        Map<String, Map<String, Map<String, Double>>> checkDataEntities = checkDataService.checkGribDataDayNew(param);
        Map<String, Map<String, Map<String, Double>>> checkDataEntities = checkDataService.checkGribDataDayJdbc(param);
        LogRecordParams logRecordParams = new LogRecordParams("预报检验", "格点逐日检验数据查询");
        logService.addLogRecord(logRecordParams);
//        System.out.println(checkDataEntities);
        return checkDataEntities;
    }
    @ApiOperation("站点评估逐时")
    @PostMapping("checkStationDataHourOld")
    public Map<String, Map<String, Map<String, Map<String, Double>>>> checkStationDataHourOld(@RequestBody CheckDataParams param)
    {
    	Map<String, Map<String, Map<String, Map<String, Double>>>> checkDataEntities = checkDataService.checkStationDataHour(param);
    	
//        System.out.println(checkDataEntities);
    	return checkDataEntities;
    }
    
    @ApiOperation("站点评估逐日")
    @PostMapping("checkStationDataDayOld")
    public Map<String, Map<String, Map<String, Map<String, Double>>>> checkStationDataDayOld(@RequestBody CheckDataParams param)
    {
    	Map<String, Map<String, Map<String, Map<String, Double>>>> checkDataEntities = checkDataService.checkStationDataDay(param);
    	
//        System.out.println(checkDataEntities);
    	return checkDataEntities;
    }
    
    @ApiOperation("格点逐时评估")
    @PostMapping("checkGribDataHourOld")
    public Map<String, Map<String, Map<String, Double>>> checkGribDataHourOld(@RequestBody CheckDataParams param)
    {
    	Map<String, Map<String, Map<String, Double>>> checkDataEntities = checkDataService.checkGribDataHour(param);
    	
//        System.out.println(checkDataEntities);
    	return checkDataEntities;
    }
    
    @ApiOperation("格点逐日评估")
    @PostMapping("checkGribDataDayOld")
    public Map<String, Map<String, Map<String, Double>>> checkGribDataDayOld(@RequestBody CheckDataParams param)
    {
    	Map<String, Map<String, Map<String, Double>>> checkDataEntities = checkDataService.checkGribDataDay(param);
    	
//        System.out.println(checkDataEntities);
    	return checkDataEntities;
    }
}