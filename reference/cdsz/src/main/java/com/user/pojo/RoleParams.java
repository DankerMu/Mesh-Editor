package com.user.pojo;

import com.tool.PageParam;
import lombok.Data;

/**
 * @category
 * @date 2025/3/25 17:38
 * @description TODO
 */
@Data
public class RoleParams extends PageParam {
    public Long id;
    public String roleName;
    public String paramName;
    public String roleDesc;
}
