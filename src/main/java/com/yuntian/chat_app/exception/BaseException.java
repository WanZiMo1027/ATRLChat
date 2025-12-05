package com.yuntian.chat_app.exception;

/**
 * 基础异常类
 */
public class BaseException extends RuntimeException {
    /**
     * 异常码
     */
    private int code;
    /**
     * 异常码的getter方法
     * @return 异常码
     */
    public int getCode() {
        return code;
    }

    public BaseException(int code, String message) {
        super(message);
        this.code = code;
    }


}
