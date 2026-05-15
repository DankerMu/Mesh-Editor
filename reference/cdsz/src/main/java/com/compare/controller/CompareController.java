package com.compare.controller;

import com.compare.pojo.CompareDataParams;
import com.compare.service.CompareServiceImpl;
import com.compare.service.inf.CompareService;
import com.log.pojo.LogRecordParams;
import com.log.service.inf.LogService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

import java.util.List;
import java.util.Map;

/**
 * @category
 * @date 2025/3/18 16:32
 * @description TODO
 */
@RestController
@RequestMapping("/compare/")
@Api(tags = "实况预报数据对比")
public class CompareController {
    @Resource
    private CompareService compareService;
    @Resource
    private LogService logService;
    @ApiOperation("站点对比")
    @PostMapping("compareStationData")
    public Map<String, Map<String, List<Map<String, String>>>> compareStationData(@RequestBody CompareDataParams params)
    {
    	Map<String, Map<String, List<Map<String, String>>>> result = compareService.compareStationData(params);
    	LogRecordParams logRecordParams = new LogRecordParams("预报对比", "站点预报对比数据查询");
        logService.addLogRecord(logRecordParams);
    	
        return result;
    }
    @ApiOperation("格点对比")
    @PostMapping("compareGribData")
    public Map<String, Map<String, Double>> compareGribData(@RequestBody CompareDataParams params)
    {
    	Map<String, Map<String, Double>> result = compareService.compareGribData(params);
    	LogRecordParams logRecordParams = new LogRecordParams("预报对比", "格点预报对比数据查询");
        logService.addLogRecord(logRecordParams);
        
        return result;
    }

}
