package com.station.pojo;

import lombok.Data;

/**
 * @category
 * @date 2025/4/1 13:21
 * @description TODO
 */
@Data
public class Cnty {
    private Long id;
    private String cnty;
    private String city;
    private String province;
    private Long cityId;
    private Long provinceId;
}
