package site.bitinit.pnd.web.exception;

/**
 * 文件未找到异常。
 * 当请求的文件在系统中不存在时抛出此异常。
 * 继承自BusinessException，表示业务层面的文件未找到错误。
 *
 * 使用场景：
 * - 文件ID不存在
 * - 文件路径不存在
 * - 文件已被删除
 *
 * @author john
 * @date 2020-01-11
 */
public class FileNotFoundException extends BusinessException {
    private static final long serialVersionUID = -1731288043175680000L;

    /**
     * 默认构造函数。
     */
    public FileNotFoundException() {
        super(ErrorCode.FILE_NOT_FOUND);
    }

    /**
     * 构造函数，指定异常消息。
     *
     * @param message 异常消息
     */
    public FileNotFoundException(String message) {
        super(ErrorCode.FILE_NOT_FOUND, message);
    }

    /**
     * 构造函数，指定异常消息和原因。
     *
     * @param message 异常消息
     * @param cause   异常原因
     */
    public FileNotFoundException(String message, Throwable cause) {
        super(ErrorCode.FILE_NOT_FOUND, message, cause);
    }

    /**
     * 构造函数，指定异常原因。
     *
     * @param cause 异常原因
     */
    public FileNotFoundException(Throwable cause) {
        super(ErrorCode.FILE_NOT_FOUND, cause);
    }
}
