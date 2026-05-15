package com.log.controller;

import com.log.pojo.LogQueryParams;
import com.log.pojo.LogRecordParams;
import com.log.service.inf.LogService;
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
 * @date 2025/3/28 7:59
 * @description TODO
 */
@RestController
@RequestMapping("/log/")
@Api(tags = "日志管理")
public class LogController {
    @Resource
    private LogService logService;

    @ApiOperation("查询操作记录日志")
    @PostMapping("queryLogRecords")
    public PageResult queryLogRecords(@RequestBody LogQueryParams params){
        PageResult result = logService.queryLogRecords(params);

        return result;
    }
}
