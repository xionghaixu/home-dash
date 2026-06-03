package com.hd.common.exception;

import com.hd.common.enums.ErrorCodeEnum;

/**
 * @author xhx
 * @version 1.0
 * @package com.hd.common.exception
 * @createTime 2026/04/23 23:34
 * @description 系统异常。用于表示数据库、IO、网络等系统级错误。
 */
public class SystemException extends HomeDashException {
    private static final long serialVersionUID = -455084301379506107L;

    /**
     * 指定错误码。
     */
    public SystemException(ErrorCodeEnum errorCode) {
        super(errorCode);
    }

    /**
     * 指定错误码和消息。
     */
    public SystemException(ErrorCodeEnum errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * 指定错误码、消息和原因。
     */
    public SystemException(ErrorCodeEnum errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    /**
     * 指定错误码和原因。
     */
    public SystemException(ErrorCodeEnum errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}

