package com.obs.pojo;

import lombok.Data;

import java.util.Date;

/**
 * @category
 * @date 2025/3/18 9:51
 * @description TODO
 */
@Data
public class ObsDataParams {
    private String station;
    private String dataTime;
    private String startDateTime;
    private String endDateTime;
    private String hour;
}
