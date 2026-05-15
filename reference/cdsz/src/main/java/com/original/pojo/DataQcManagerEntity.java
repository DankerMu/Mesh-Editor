package com.original.pojo;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @category
 * @date 2025/4/21 18:13
 * @description TODO
 */
@Data
@TableName(value = "public.data_qc_manager_tab")
public class DataQcManagerEntity {
    private String dataTime;
    private int total;
    private int arrived;
    private int unarrived;
    private String rate;
    private String status;
    private String insertTime;
    private String dataSource;
}
