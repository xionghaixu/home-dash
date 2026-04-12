package site.bitinit.pnd.web.exception;

/**
 * 文件已存在异常。
 * 当尝试创建已存在的文件或文件夹时抛出此异常。
 * 继承自BusinessException，表示业务层面的文件冲突错误。
 *
 * 使用场景：
 * - 创建文件时文件已存在
 * - 重命名文件时目标文件名已存在
 * - 移动文件时目标位置已存在同名文件
 *
 * @author john
 * @date 2020-01-11
 */
public class FileAlreadyExistsException extends BusinessException {
    private static final long serialVersionUID = -1731288043175680001L;

    /**
     * 默认构造函数。
     */
    public FileAlreadyExistsException() {
        super(ErrorCode.FILE_ALREADY_EXISTS);
    }

    /**
     * 构造函数，指定异常消息。
     *
     * @param message 异常消息
     */
    public FileAlreadyExistsException(String message) {
        super(ErrorCode.FILE_ALREADY_EXISTS, message);
    }

    /**
     * 构造函数，指定异常消息和原因。
     *
     * @param message 异常消息
     * @param cause   异常原因
     */
    public FileAlreadyExistsException(String message, Throwable cause) {
        super(ErrorCode.FILE_ALREADY_EXISTS, message, cause);
    }

    /**
     * 构造函数，指定异常原因。
     *
     * @param cause 异常原因
     */
    public FileAlreadyExistsException(Throwable cause) {
        super(ErrorCode.FILE_ALREADY_EXISTS, cause);
    }
}
