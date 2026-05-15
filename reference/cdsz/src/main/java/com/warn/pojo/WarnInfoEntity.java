package com.warn.pojo;

import lombok.Data;

/**
 * @category
 * @date 2025/5/6 23:00
 * @description TODO
 */
@Data
public class WarnInfoEntity {
    private long id;
    private String type;
    private String content;
    private String insertTime;
}
