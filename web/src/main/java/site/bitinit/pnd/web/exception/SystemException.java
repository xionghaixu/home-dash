package site.bitinit.pnd.web.exception;

/**
 * 系统异常类。
 * 用于表示系统级别的异常，如数据库错误、IO错误、网络错误等。
 * 这类异常通常不是由用户操作引起的，而是由系统环境或配置问题导致的。
 *
 * @author john
 * @date 2020-01-11
 */
public class SystemException extends PndException {
    private static final long serialVersionUID = -455084301379506107L;

    private final ErrorCode errorCode;

    /**
     * 构造函数，指定错误码。
     *
     * @param errorCode 错误码枚举
     */
    public SystemException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * 构造函数，指定错误码和自定义消息。
     *
     * @param errorCode 错误码枚举
     * @param message   自定义错误消息
     */
    public SystemException(ErrorCode errorCode, String message) {
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
    public SystemException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * 构造函数，指定错误码和原因。
     *
     * @param errorCode 错误码枚举
     * @param cause     异常原因
     */
    public SystemException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    /**
     * 获取错误码。
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
}
