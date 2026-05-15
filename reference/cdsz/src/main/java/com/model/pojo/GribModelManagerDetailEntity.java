package com.model.pojo;

import lombok.Data;

/**
 * @category
 * @date 2025/6/19 15:40
 * @description TODO
 */
@Data
public class GribModelManagerDetailEntity {
    private int id;
    private String stationName;
    private String stationNum = "";
    private String model;
    private int status;
    private int used;
    private String author;
    private String replaceTime;
    private int managerId;
    private double lon;
    private double lat;
    private int enabled;
    private int replaceflag;
}
