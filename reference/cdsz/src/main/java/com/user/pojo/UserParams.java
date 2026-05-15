package com.user.pojo;

import com.tool.PageParam;
import lombok.Data;

/**
 * @category
 * @date 2025/3/25 10:31
 * @description TODO
 */
@Data
public class UserParams extends PageParam {
    private Long id;
    private String username;
    private String loginname;
    private String role;
    private boolean enabled;
}
