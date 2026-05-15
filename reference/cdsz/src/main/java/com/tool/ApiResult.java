package com.tool;

import lombok.Data;

import java.io.Serializable;

/**
 * @category
 * @date 2025/3/13 13:21
 * @return
 */
@Data
public class ApiResult<T> implements Serializable {
    /**
     * 提示信息
     */
    String msg;
    /**
     * 代码
     */
    Integer code;
    /**
     * 数据
     */
    T data;

    Integer metaId;

    public ApiResult(String msg, Integer code, T data,Integer metaid) {
        this.msg = msg;
        this.code = code;
        this.data = data;
        this.metaId =metaid;
    }

    public ApiResult(String msg, Integer code, T data ) {
        this.msg = msg;
        this.code = code;
        this.data = data;
    }
    public ApiResult(String msg, Integer code) {
        this.msg = msg;
        this.code = code;
    }

    public ApiResult(Integer code, T data) {
        this.code = code;
        this.data = data;
    }

    public static <T> ApiResult instance(ResultCode code, T data) {
        return new ApiResult(code.getCode(), data);
    }

    public static <T> ApiResult success(T data) {
        return new ApiResult(ResultCode.OK.msg, ResultCode.OK.code, data);
    }

    public static <T> ApiResult successMetaid(T data,Integer metaId) {
        return new ApiResult(ResultCode.OK.msg, ResultCode.OK.code, data,metaId);
    }

    public static <T> ApiResult success() {
        return new ApiResult(ResultCode.OK.code, "成功!");
    }

    public static <T> ApiResult fail(String message) {
        return new ApiResult(message, ResultCode.FAIL.code);
    }
}
