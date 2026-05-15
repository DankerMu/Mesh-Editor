package com.user.pojo;

import com.authorize.pojo.Permission;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Set;

@Data
@TableName("sys_roles")
public class Role {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String roleName;
    private String roleKey;
    private String roleDesc;
    private Set<Permission> permissions;
}