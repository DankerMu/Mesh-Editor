package com.model.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @category
 * @date 2025/3/14 16:55
 * @description TODO
 */
@Data
@TableName("public.model_info_tab")
public class ModelInfoEntity {
    @TableField("model_id")
    private String modelId;
    @TableField("model_name")
    private String modelName;
    @TableField("model_version")
    private String modelVersion;
    @TableField("publisher")
    private String publisher;
    @TableField("valid_time")
    private String validTime;
    @TableField("create_time")
    private String createTime;
    @TableField("params")
    private String params;
    @TableField("model")
    private String model;
    @TableField("modeltype")
    private String modelType;
    private String lon;
    private String lat;
}
