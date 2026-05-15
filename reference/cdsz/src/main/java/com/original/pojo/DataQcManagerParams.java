package com.original.pojo;

import com.tool.PageParam;
import lombok.Data;

/**
 * @category
 * @date 2025/4/21 18:20
 * @description TODO
 */
@Data
public class DataQcManagerParams extends PageParam {
    private String dataTime;
    private String startTime;
    private String endTime;
    private String dataSource;
    private String[] datatimes;
    private String tableName;
}
