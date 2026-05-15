package com.original.controller;

import com.log.pojo.LogRecordParams;
import com.log.service.inf.LogService;
import com.original.pojo.DataQcManagerParams;
import com.original.pojo.NafpDataParams;
import com.original.service.inf.QueryNafpDataService;
import com.tool.PageResult;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @category
 * @date 2025/3/13 13:15
 * @description TODO
 */
@RestController
@RequestMapping("/original/")
@Api(tags = "原始数据接口")
public class QueryNafpDataController extends BaseController{
    @Resource
    private QueryNafpDataService queryNafpDataService;
    @Resource
    private LogService logService;
    
    @PostMapping("/queryNafpData")
    @ApiOperation("查询接口")
    public PageResult queryNafpData(@RequestBody NafpDataParams params) {
        try {
            return queryNafpDataService.queryOriginalData(params);
        } catch (Exception e) {
            e.printStackTrace();
            return PageResult.fail(e.getMessage());
        }
    }

    @PostMapping("/queryNafpDataCount")
    @ApiOperation("查询原始数据入库情况接口")
    public PageResult queryNafpDataCount(@RequestBody NafpDataParams params)
    {
        return queryNafpDataService.queryNafpDataCount(params);
    }

    @PostMapping("/deleteNafpDataByDateTimeDataSource")
    @ApiOperation("删除原始数据入库情况和原始文件接口")
    public int deleteNafpDataByDateTimeDataSource(@RequestBody DataQcManagerParams params)
    {
        return queryNafpDataService.deleteNafpDataByDateTimeDataSource(params);
    }

    @PostMapping("/updateNafpDataByDateTimeDataSource")
    @ApiOperation("删除原始数据入库情况和原始文件接口")
    public int updateNafpDataByDateTimeDataSource(@RequestBody DataQcManagerParams params)
    {
        return queryNafpDataService.updateNafpDataByDateTimeDataSource(params);
    }

    @PostMapping("/queryOriginalNafpDataCount")
    @ApiOperation("根据数据类型查询数据采集情况接口")
    public PageResult queryOriginalNafpDataCount(@RequestBody DataQcManagerParams params)
    {
        PageResult result = null;
        if(params.getDataSource().equals("all"))
        {
//            result = queryNafpDataService.queryOriginalNafpDataCountByPageAll(params);
            result = queryNafpDataService.queryDataManagerCountByPageAll(params);
        }
        else
        {
//            result = queryNafpDataService.queryOriginalNafpDataCountByPage(params);
            result = queryNafpDataService.queryDataManagerCountByPage(params);
        }
        LogRecordParams logRecordParams = new LogRecordParams("数据管理", "原始数据统计查询");
        logService.addLogRecord(logRecordParams);

        return result;
    }
}
