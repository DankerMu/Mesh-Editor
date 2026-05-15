package com.station.pojo;

import lombok.Data;

/**
 * @category
 * @date 2025/4/1 14:31
 * @description TODO
 */
@Data
public class StationEntity {
    private int id;
    private double lat;
    private double lon;
    private String station;
    private Long provinceId;
    private Long cityId;
    private Long cntyId;
    private String stationName;
}
