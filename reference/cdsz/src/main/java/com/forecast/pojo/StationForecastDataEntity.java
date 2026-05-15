package com.forecast.pojo;

import lombok.Data;

/**
 * @category
 * @date 2025/3/13 17:19
 * @description TODO
 */
@Data
public class StationForecastDataEntity {
    private String station;
    private String datatime;
    private String validdate;
    private int vti;
    private double at;
    private double atmax;
    private double atmin;
    private double rh;
    private double rain;
    private double rain24;
    private double wd;
    private double ws;
    private String wdStr;
    private String wsStr;
    private double slp;
    private double vis;
    private String lcc;
    private double tcc;
    private double n;
    private double ptype;
    private String ptypeStr;
}
