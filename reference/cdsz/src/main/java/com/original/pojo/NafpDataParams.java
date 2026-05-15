package com.original.pojo;

import com.tool.PageParam;
import lombok.Data;

/**
 * @category
 * @date 2025/3/13 11:30
 * @description TODO
 */
@Data
public class NafpDataParams extends PageParam {
    private String startDateTime;
    private String endDateTime;
    private String fileName;
    private String dataType;
}
