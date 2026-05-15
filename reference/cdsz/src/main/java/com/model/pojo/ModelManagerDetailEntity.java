package com.model.pojo;

import lombok.Data;

/**
 * @category
 * @date 2025/6/19 15:40
 * @description TODO
 */
@Data
public class ModelManagerDetailEntity {
    private int id;
    private String stationName;
    private String stationNum;
    private String model;
    private int status;//0:替换中  1: 替换完成
    private int used;//0:在使用  1:未使用
    private String author;
    private String replaceTime;
    private int managerId;
    private double lon;
    private double lat;
    private int enabled;//0:未删除   1:已删除
    private String insertTime;
    private int bfz;//边防站: 0:非边防站 1:边防站
    private String dataType;
    private int replaceflag;// 0:  不需要替换  1: 需要替换
}
