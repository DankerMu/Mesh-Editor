package com.log.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.log.dao.LogMapper;
import com.log.pojo.LogQueryParams;
import com.log.pojo.LogRecordParams;
import com.log.service.inf.LogService;
import com.tool.PageBuilder;
import com.tool.PageResult;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;

/**
 * @category
 * @date 2025/3/27 14:52
 * @description TODO
 */
@Service
public class LogServiceImpl implements LogService {
    @Resource
    private LogMapper logMapper;
    @Override
    public int addLogRecord(LogRecordParams params) {
        int count = logMapper.addLogRecord(params);

        return count;
    }

    @Override
    public PageResult queryLogRecords(LogQueryParams params) {
        PageResult pageResult = new PageResult();
        IPage<LogRecordParams> result = logMapper.queryLogRecords(PageBuilder.build(params.getPageNum(), params.getPageSize()), params);
        pageResult.setPage(params.getPageNum());
        pageResult.setPageSize(params.getPageSize());
        pageResult.setTotal(result.getTotal());
        pageResult.setData(result.getRecords());

        return pageResult;
    }
}
