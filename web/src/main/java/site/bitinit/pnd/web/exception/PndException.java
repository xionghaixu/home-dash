package site.bitinit.pnd.web.exception;

import java.util.HashMap;
import java.util.Map;

/**
 * PND基础异常类。
 * 所有PND业务异常的基类，提供统一的异常处理机制。
 * 支持错误码、详细消息和上下文信息，便于问题定位和日志记录。
 *
 * <h3>主要特性：</h3>
 * <ul>
 * <li>支持ErrorCode枚举，统一错误码管理</li>
 * <li>支持上下文数据，便于记录额外的调试信息</li>
 * <li>提供丰富的构造函数，满足不同场景需求</li>
 * </ul>
 *
 * @author john
 * @date 2020-01-11
 */
public class PndException extends RuntimeException {
    private static final long serialVersionUID = -455084301379506105L;

    /**
     * 错误码枚举。
     */
    private final ErrorCode errorCode;

    /**
     * 上下文数据，用于存储额外的调试信息。
     */
    private final Map<String, Object> contextData;

    /**
     * 默认构造函数。
     * 使用INTERNAL_SERVER_ERROR作为默认错误码。
     */
    public PndException() {
        super(ErrorCode.INTERNAL_SERVER_ERROR.getMessage());
        this.errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        this.contextData = new HashMap<>();
    }

    /**
     * 构造函数，指定异常消息。
     * 使用INTERNAL_SERVER_ERROR作为默认错误码。
     *
     * @param message 异常消息
     */
    public PndException(String message) {
        super(message);
        this.errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        this.contextData = new HashMap<>();
    }

    /**
     * 构造函数，指定错误码。
     *
     * @param errorCode 错误码枚举
     */
    public PndException(ErrorCode errorCode) {
        super(errorCode != null ? errorCode.getMessage() : ErrorCode.INTERNAL_SERVER_ERROR.getMessage());
        this.errorCode = errorCode != null ? errorCode : ErrorCode.INTERNAL_SERVER_ERROR;
        this.contextData = new HashMap<>();
    }

    /**
     * 构造函数，指定错误码和自定义消息。
     *
     * @param errorCode 错误码枚举
     * @param message   自定义错误消息
     */
    public PndException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode != null ? errorCode : ErrorCode.INTERNAL_SERVER_ERROR;
        this.contextData = new HashMap<>();
    }

    /**
     * 构造函数，指定异常消息和原因。
     * 使用INTERNAL_SERVER_ERROR作为默认错误码。
     *
     * @param message 异常消息
     * @param cause   异常原因
     */
    public PndException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        this.contextData = new HashMap<>();
    }

    /**
     * 构造函数，指定异常原因。
     * 使用INTERNAL_SERVER_ERROR作为默认错误码。
     *
     * @param cause 异常原因
     */
    public PndException(Throwable cause) {
        super(cause);
        this.errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        this.contextData = new HashMap<>();
    }

    /**
     * 构造函数，指定错误码和原因。
     *
     * @param errorCode 错误码枚举
     * @param cause     异常原因
     */
    public PndException(ErrorCode errorCode, Throwable cause) {
        super(errorCode != null ? errorCode.getMessage() : ErrorCode.INTERNAL_SERVER_ERROR.getMessage(), cause);
        this.errorCode = errorCode != null ? errorCode : ErrorCode.INTERNAL_SERVER_ERROR;
        this.contextData = new HashMap<>();
    }

    /**
     * 构造函数，指定错误码、消息和原因。
     *
     * @param errorCode 错误码枚举
     * @param message   错误消息
     * @param cause     异常原因
     */
    public PndException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode != null ? errorCode : ErrorCode.INTERNAL_SERVER_ERROR;
        this.contextData = new HashMap<>();
    }

    /**
     * 构造函数，指定异常消息、原因、是否启用抑制和是否可写堆栈跟踪。
     *
     * @param message            异常消息
     * @param cause              异常原因
     * @param enableSuppression  是否启用抑制
     * @param writableStackTrace 是否可写堆栈跟踪
     */
    protected PndException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
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
     * @return 上下文数据的不可变视图
     */
    public Map<String, Object> getContextData() {
        return java.util.Collections.unmodifiableMap(contextData);
    }

    /**
     * 添加上下文数据。
     *
     * @param key   数据键
     * @param value 数据值
     * @return 当前异常对象（支持链式调用）
     */
    public PndException addContextData(String key, Object value) {
        if (key != null && value != null) {
            this.contextData.put(key, value);
        }
        return this;
    }

    /**
     * 判断是否包含上下文数据。
     *
     * @return 如果包含上下文数据返回true，否则返回false
     */
    public boolean hasContextData() {
        return !contextData.isEmpty();
    }

    /**
     * 获取包含上下文信息的完整错误消息。
     *
     * @return 包含上下文信息的完整错误消息
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
