package com.model.pojo;

import com.tool.PageParam;
import lombok.Data;

/**
 * @category
 * @date 2025/3/14 16:55
 * @description TODO
 */
@Data
public class ModelInfoParams extends PageParam {
    private int modelId;
    private String modelName;
    private String modelType;
}
