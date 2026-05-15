package com.original.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @category
 * @date 2025/3/13 11:46
 * @description TODO
 */
@Data
@TableName(value = "public.nafp_t1279_tab")
public class NafpDataEntity {
    @TableField("filename")
    private String fileName;
    @TableField("datatime")
    private String datatime;
    @TableField("hour")
    private String hour;
    @TableField("vti")
    private String vti;
    @TableField("datatype")
    private String dataType;
    @TableField("filepath")
    private String filePath;
    @TableField("inserttime")
    private String insertTime;
    @TableField("filesize")
    private String fileSize;
    @TableField("filetype")
    private String fileType;
}
