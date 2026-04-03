package com.yuntian.chat_app.exception;

/**
 * 群组异常类
 */
public class GroupException extends BaseException {

    public static final int GROUP_NOT_FOUND = 3001;

    public static final int GROUP_PERMISSION_DENIED = 3002;

    public static final int GROUP_AVATAR_INVALID_TYPE = 3003;

    public static final int GROUP_AVATAR_TOO_LARGE = 3004;

    public static final int GROUP_AVATAR_UPLOAD_FAILED = 3005;

    public GroupException(int code, String message) {
        super(code, message);
    }
}
