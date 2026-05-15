package com.log.dao;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.log.pojo.LogQueryParams;
import com.log.pojo.LogRecordParams;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LogMapper {
    int addLogRecord(LogRecordParams params);

    IPage<LogRecordParams> queryLogRecords(IPage page, @Param("params") LogQueryParams params);
}