package com.tool;

/**
 * @category
 * @date 2025/3/13 13:20
 * @return
 */
public enum ResultCode {
    OK(200,"操作成功"),
    FAIL(-1,"操作失败"),
    LOGIN_FAIL(-2,"登录失败")
    ;

    Integer code;
    String msg;

    ResultCode(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
