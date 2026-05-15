package com.model.pojo;

import lombok.Data;

/**
 * @category
 * @date 2025/6/19 10:27
 * @description TODO
 */
@Data
public class ModelManagerEntity {
    private int id;
    private String modelName;
    private String author;
    private String latestUpdateTime;
    private String nextUpdateTime;
    private int status;
    private int dis;
    private String type;
    private int hasReplacing; 
    private int detail;// 0: 不需要详情  1: 需要详情
    private int upgrade;//0:  不需要升级  1: 需要升级
    private int replaceflag;// 0:  不需要替换  1: 需要替换
}
