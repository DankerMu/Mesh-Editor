package com.compare.pojo;

import com.obs.pojo.ObsDataParams;
import lombok.Data;

/**
 * @category
 * @date 2025/3/18 10:43
 * @description TODO
 */
@Data
public class CompareDataParams {
    private String station;
    private String dataTime;
    private String[] dataSource;
    private String[] dataSourceOrg;
    private String startDateTime;
    private String endDateTime;
    private String tableName;
}
