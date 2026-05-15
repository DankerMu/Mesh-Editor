package com.authorize.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_permissions")
public class Permission {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String permName;
    private String permKey;
}