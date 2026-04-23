package com.hd.common.exception;

import com.hd.common.enums.ErrorCode;

import java.util.HashMap;
import java.util.Map;

/**
 * HomeDash 异常基类。
 *
 * <p>统一承载错误码、上下文和详细信息。
 *
 * @author john
 * @date 2020-01-11
 */
public class HomeDashException extends RuntimeException {
    private static final long serialVersionUID = -455084301379506105L;

    /**
     * 错误码枚举。
     */
    private final ErrorCode errorCode;

    /**
     * 上下文数据，用于保存调试信息。
     */
    private final Map<String, Object> contextData;

    /**
     * 默认构造函数。
     */
    public HomeDashException() {
        super(ErrorCode.INTERNAL_SERVER_ERROR.getMessage());
        this.errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        this.contextData = new HashMap<>();
    }

    /**
     * 构造函数，指定异常信息。
     *
     * @param message 异常信息
     */
    public HomeDashException(String message) {
        super(message);
        this.errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        this.contextData = new HashMap<>();
    }

    /**
     * 构造函数，指定错误码。
     *
     * @param errorCode 错误码枚举
     */
    public HomeDashException(ErrorCode errorCode) {
        super(errorCode != null ? errorCode.getMessage() : ErrorCode.INTERNAL_SERVER_ERROR.getMessage());
        this.errorCode = errorCode != null ? errorCode : ErrorCode.INTERNAL_SERVER_ERROR;
        this.contextData = new HashMap<>();
    }

    /**
     * 构造函数，指定错误码和自定义信息。
     *
     * @param errorCode 错误码枚举
     * @param message 自定义异常信息
     */
    public HomeDashException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode != null ? errorCode : ErrorCode.INTERNAL_SERVER_ERROR;
        this.contextData = new HashMap<>();
    }

    /**
     * 构造函数，指定异常信息和原因。
     *
     * @param message 异常信息
     * @param cause 异常原因
     */
    public HomeDashException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        this.contextData = new HashMap<>();
    }

    /**
     * 构造函数，指定异常原因。
     *
     * @param cause 异常原因
     */
    public HomeDashException(Throwable cause) {
        super(cause);
        this.errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        this.contextData = new HashMap<>();
    }

    /**
     * 构造函数，指定错误码和异常原因。
     *
     * @param errorCode 错误码枚举
     * @param cause 异常原因
     */
    public HomeDashException(ErrorCode errorCode, Throwable cause) {
        super(errorCode != null ? errorCode.getMessage() : ErrorCode.INTERNAL_SERVER_ERROR.getMessage(), cause);
        this.errorCode = errorCode != null ? errorCode : ErrorCode.INTERNAL_SERVER_ERROR;
        this.contextData = new HashMap<>();
    }

    /**
     * 构造函数，指定错误码、信息和原因。
     *
     * @param errorCode 错误码枚举
     * @param message 异常信息
     * @param cause 异常原因
     */
    public HomeDashException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode != null ? errorCode : ErrorCode.INTERNAL_SERVER_ERROR;
        this.contextData = new HashMap<>();
    }

    /**
     * 构造函数，指定异常信息、原因和栈信息开关。
     *
     * @param message 异常信息
     * @param cause 异常原因
     * @param enableSuppression 是否启用抑制异常
     * @param writableStackTrace 是否写入堆栈信息
     */
    protected HomeDashException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        this.contextData = new HashMap<>();
    }

    /**
     * 获取错误码枚举。
     *
     * @return 错误码枚举
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * 获取错误码数值。
     *
     * @return 错误码数值
     */
    public int getCode() {
        return errorCode.getCode();
    }

    /**
     * 获取上下文数据。
     *
     * @return 不可变上下文数据
     */
    public Map<String, Object> getContextData() {
        return java.util.Collections.unmodifiableMap(contextData);
    }

    /**
     * 添加上下文数据。
     *
     * @param key 键
     * @param value 值
     * @return 当前异常对象，便于链式调用
     */
    public HomeDashException addContextData(String key, Object value) {
        if (key != null && value != null) {
            this.contextData.put(key, value);
        }
        return this;
    }

    /**
     * 判断是否包含上下文数据。
     *
     * @return 包含则返回 true，否则返回 false
     */
    public boolean hasContextData() {
        return !contextData.isEmpty();
    }

    /**
     * 获取包含上下文信息的详细异常消息。
     *
     * @return 详细异常消息
     */
    public String getDetailedMessage() {
        StringBuilder sb = new StringBuilder(super.getMessage());

        if (!contextData.isEmpty()) {
            sb.append(" [Context: ");
            sb.append(contextData.entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .collect(java.util.stream.Collectors.joining(", ")));
            sb.append("]");
        }

        return sb.toString();
    }
}

