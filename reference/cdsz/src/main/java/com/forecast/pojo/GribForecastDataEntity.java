package com.forecast.pojo;

import lombok.Data;

/**
 * @category
 * @date 2025/3/20 10:41
 * @description TODO
 */
@Data
public class GribForecastDataEntity {
    private String dataSource;
    private String dataTime;
    private String validDate;
    private int vti;
    private int rainVti;
    private String sourcePath;
    private String filePath;
    private String urlPath;
//    private String tableName;
}
