package com.obs.pojo;

import lombok.Data;

/**
 * @category
 * @date 2025/3/18 9:48
 * @description TODO
 */
@Data
public class ObsDataEntity {
    private String station;
    private double lat;
    private double lon;
    private String datetime;
    private double at;
    private double atmin;
    private double atmax;
    private double rh;
    private double rain;
    private double rain24;
    private double wd;
    private double ws;
    private double slp;
    private double vis;
    private double n;
    private double lcc;
    private double tcc;
    private double ptype;
}
