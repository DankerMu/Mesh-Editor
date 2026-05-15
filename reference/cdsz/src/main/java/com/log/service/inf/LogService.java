package com.log.service.inf;

import com.log.pojo.LogQueryParams;
import com.log.pojo.LogRecordParams;
import com.tool.PageResult;

public interface LogService {
    int addLogRecord(LogRecordParams params);

    PageResult queryLogRecords(LogQueryParams params);
}
