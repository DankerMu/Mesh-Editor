package com.forecast.pojo;

import com.tool.PageParam;
import lombok.Data;

/**
 * @category
 * @date 2025/3/13 17:19
 * @description TODO
 */
@Data
public class StationForecastDataParams extends PageParam {
    private String station;
    private String[] stations;
    private String dataTime;
    private String startDateTime;
    private String endDateTime;
    private String tableName;
    private int startVti;
    private int endVti;
}
