package com.tool;

import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * @category
 * @date 2025/3/13 13:20
 * @return
 */
@Data
public class PageResult<T> implements Serializable {
    /**
     * 提示信息
     */
    String msg;
    /**
     * 代码
     */
    Integer code = ResultCode.OK.getCode();
    /**
     * 数据
     */
    List<T> data;

    Integer page;

    Long total;

    Integer pageSize;



    public static <T> PageResult fail(String message) {
        PageResult apiPageResult = new PageResult();
        apiPageResult.setCode(ResultCode.FAIL.getCode());
        apiPageResult.setMsg(message);
        return apiPageResult;
    }

    public static <T> PageResult empty() {
        PageResult apiPageResult = new PageResult();
        apiPageResult.setCode(ResultCode.OK.getCode());
        apiPageResult.setTotal(0l);
        apiPageResult.setData(Collections.emptyList());
        return apiPageResult;
    }
}
