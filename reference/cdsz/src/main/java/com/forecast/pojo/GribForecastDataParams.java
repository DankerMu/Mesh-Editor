package com.forecast.pojo;

import com.tool.PageParam;
import lombok.Data;

/**
 * @category
 * @date 2025/3/13 17:19
 * @description TODO
 */
@Data
public class GribForecastDataParams extends PageParam {
    private String tableName;
    private String dataTime;
    private String startDateTime;
    private String endDateTime;
    private String dataSource;
    private int[] vtis;
    private int rainVti;
}
