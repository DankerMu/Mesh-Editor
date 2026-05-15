package com.station.pojo;

import lombok.Data;

/**
 * @category
 * @date 2025/4/1 13:20
 * @description TODO
 */
@Data
public class City {
    private Long id;
    private String city;
    private String province;
    private Long provinceId;
}
