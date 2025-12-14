package com.yuntian.chat_app.exception;

public class CharacterException extends BaseException {

    /**
     * 角色不存在
     */
    public static final int CHARACTER_ERROR = 2001;

    /**
     * 新增角色失败异常码
     */
    public static final int CHARACTER_CREATE_MySQL_ERROR = 2002;

    /**
     * 新增角色失败异常码
     */
     public static final int CHARACTER_CREATE_ERROR = 2003;

    /**
     *
     * @param code
     * @param message
     */

    public CharacterException(int code, String message) {
        super(code, message);
    }

}
