package com.station.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @category
 * @date 2025/3/14 15:08
 * @description TODO
 */
@Data
@TableName("public.station_info_tab")
public class StationInfoEntity {
    @TableField("station_name")
    private String stationName;
    @TableField("station_id_c")
    private String stationIdC;
    @TableField("station_id_d")
    private int stationIdD;
    @TableField("lon")
    private double lon;
    @TableField("lat")
    private double lat;
    private int maxNum;
    private int enabled;
    private Integer zone;
    private Integer zone2;
    private String author;
    private int flag;//0:原有站点 1:无站点 2:自建点
    private String tableName;
    private String transName;
    private String stationNumStr;
    private int bfz;//边防站: 0:非边防站 1:边防站
    private int index;//添加站点的顺序
}
