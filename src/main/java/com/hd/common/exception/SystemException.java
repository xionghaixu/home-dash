package com.hd.common.exception;

import com.hd.common.enums.ErrorCode;

/**
 * @author xhx
 * @version 1.0
 * @package com.hd.common.exception
 * @createTime 2026/04/23 23:34
 * @description 系统异常。用于表示数据库、IO、网络等系统级错误。
 */
public class SystemException extends HomeDashException {
    private static final long serialVersionUID = -455084301379506107L;

    private final ErrorCode errorCode;

    /**
     * 指定错误码。
     */
    public SystemException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * 指定错误码和消息。
     */
    public SystemException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 指定错误码、消息和原因。
     */
    public SystemException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * 指定错误码和原因。
     */
    public SystemException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    /**
     * 获取错误码。
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * 获取错误码数值。
     */
    public int getCode() {
        return errorCode.getCode();
    }
}

