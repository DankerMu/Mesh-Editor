package com.log.pojo;

import com.tool.PageParam;
import lombok.Data;

/**
 * @category
 * @date 2025/3/28 8:08
 * @description TODO
 */
@Data
public class LogQueryParams extends PageParam {
    private String userName;
    private String startTime;
    private String endTime;
}
