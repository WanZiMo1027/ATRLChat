package com.yuntian.chat_app.exception;

/**
 * 用户异常类
 */
public class UserException extends BaseException {
    /**
     * 用户不存在异常码
     */
    public static final int USER_NOT_FOUND = 1001;
     /**
      * 密码错误异常码
      */
    public static final int PASSWORD_ERROR = 1002;



    public UserException(int code, String message) {
        super(code, message);
    }
}
