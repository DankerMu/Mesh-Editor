package com.warn.controller;

import com.util.TimeUtil;
import com.warn.pojo.WarnInfoEntity;
import com.warn.service.inf.WarnService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @category
 * @date 2025/5/6 23:19
 * @description TODO
 */
@RestController
@RequestMapping("/warn/")
@Api(tags = "警告")
public class WarnController {
    @Resource
    private WarnService warnService;

    @PostMapping("queryWarnCount")
    @ApiOperation("查询警告数量")
    public int queryWarnCount(@RequestBody WarnInfoEntity entity){
        return warnService.queryWarnCount(entity);
    }

    @PostMapping("queryWarnInfo")
    @ApiOperation("查询警告信息")
    public List<WarnInfoEntity> queryWarnInfo(){
    	WarnInfoEntity entity = new WarnInfoEntity();
        return warnService.queryWarnInfo(entity);
    }
    
    @PostMapping("queryServerDateTime")
    @ApiOperation("查询服务器时间")
    public String queryServerDateTime(){
    	Calendar calendar = Calendar.getInstance();
    	calendar.setTime(new Date());
    	calendar.add(Calendar.HOUR_OF_DAY, 8);
    	
    	String dateTime = TimeUtil.date2String(calendar.getTime(), TimeUtil.DEFAULT_DATETIME_FORMAT);
    	
        return dateTime;
    }
}
