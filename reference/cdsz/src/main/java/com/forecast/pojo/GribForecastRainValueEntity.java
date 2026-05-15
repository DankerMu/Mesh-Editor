package com.forecast.pojo;

import lombok.Data;

/**
 * @category
 * @date 2025/5/22 10:51
 * @description TODO
 */
@Data
public class GribForecastRainValueEntity {
    private String station;
    private String dataTime;
    private String validDate;
    private int vti;
    private double rain;
}
