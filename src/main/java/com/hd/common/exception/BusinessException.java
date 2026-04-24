package com.hd.common.exception;

import com.hd.common.enums.ErrorCode;

/**
 * @author xhx
 * @version 1.0
 * @package com.hd.common.exception
 * @createTime 2026/04/23 23:34
 * @description 业务异常基类。所有业务逻辑异常都应继承此类，用于区分业务异常和系统异常。业务异常通常是由于用户操作不当或业务规则限制导致的，可以通过修正操作来避免。
 */
public class BusinessException extends HomeDashException {
    private static final long serialVersionUID = -455084301379506106L;

    private final ErrorCode errorCode;

    /**
     * 构造函数，指定错误码。
     *
     * @param errorCode 错误码枚举
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * 构造函数，指定错误码和自定义消息。
     *
     * @param errorCode 错误码枚举
     * @param message   自定义错误消息
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 构造函数，指定错误码、消息和原因。
     *
     * @param errorCode 错误码枚举
     * @param message   错误消息
     * @param cause     异常原因
     */
    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * 构造函数，指定错误码和原因。
     *
     * @param errorCode 错误码枚举
     * @param cause     异常原因
     */
    public BusinessException(ErrorCode errorCode, Throwable cause) {
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

