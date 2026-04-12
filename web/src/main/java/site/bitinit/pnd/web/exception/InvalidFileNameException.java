package site.bitinit.pnd.web.exception;

/**
 * 无效文件名异常。
 * 当文件名不符合规范时抛出此异常。
 * 继承自BusinessException，表示业务层面的文件名验证错误。
 *
 * 使用场景：
 * - 文件名包含非法字符
 * - 文件名过长
 * - 文件名为空
 * - 文件名与系统保留名冲突
 *
 * @author john
 * @date 2020-01-11
 */
public class InvalidFileNameException extends BusinessException {
    private static final long serialVersionUID = -1731288043175680005L;

    /**
     * 默认构造函数。
     */
    public InvalidFileNameException() {
        super(ErrorCode.INVALID_FILE_NAME);
    }

    /**
     * 构造函数，指定异常消息。
     *
     * @param message 异常消息
     */
    public InvalidFileNameException(String message) {
        super(ErrorCode.INVALID_FILE_NAME, message);
    }

    /**
     * 构造函数，指定异常消息和原因。
     *
     * @param message 异常消息
     * @param cause   异常原因
     */
    public InvalidFileNameException(String message, Throwable cause) {
        super(ErrorCode.INVALID_FILE_NAME, message, cause);
    }

    /**
     * 构造函数，指定异常原因。
     *
     * @param cause 异常原因
     */
    public InvalidFileNameException(Throwable cause) {
        super(ErrorCode.INVALID_FILE_NAME, cause);
    }
}
