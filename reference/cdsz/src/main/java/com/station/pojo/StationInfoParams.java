package com.station.pojo;

import lombok.Data;

/**
 * @category
 * @date 2025/3/14 15:08
 * @description TODO
 */
@Data
public class StationInfoParams {
    private String stationName;
    private String stationCode;
    private String[] stationsNum;
    private int enabled;
}
