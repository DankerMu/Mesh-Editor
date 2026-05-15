package com.check.pojo;

import java.io.Serializable;

import lombok.Data;

/**
 * @category
 * @date 2025/3/19 9:45
 * @description TODO
 */
@Data
public class CheckDataParams implements Serializable{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String startTime;
    private String endTime;
    private String validDate;
    private int hour;
    private int[] vtis;
    private int vti;
    private int startVti;
    private int endVti;
    private int disVti;//预报时效间距
    private String[] stations;
    private String[] elements;
    private String[] methods;
    private String startValidDate;
    private String endValidDate;
    private String dataSource;//自产数据来源
    private String[] dataSources;//自产数据来源
    private String[] dataSourcesOrg;//原始预报模式来源
    private String tableName;
    private Integer[] provinceId;
    private Integer[] cityId;
    private Integer[] cntyId;
    private Integer zone;//选择的区域

    private Double lonLeft;
    private Double latUp;
    private Double lonRight;
    private Double latDown;
    
    private String dataTime;
    private String fieldsName;
}
