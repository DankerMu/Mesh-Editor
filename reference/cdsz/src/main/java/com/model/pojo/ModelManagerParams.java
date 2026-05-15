package com.model.pojo;

import com.tool.PageParam;
import lombok.Data;

/**
 * @category
 * @date 2025/6/19 11:17
 * @description TODO
 */
@Data
public class ModelManagerParams extends PageParam {
    private int id;
    private String author;
    private int status;
    private String stationNum;
    private int stationInt;
    private String stationName;
    private String tableName;
    private int managerId;
    private String model;
    private String replaceTime;
    private String url;
    private int fixRateDay;
    private int replaceId;
}
