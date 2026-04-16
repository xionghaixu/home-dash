package com.hd.exception;

/**
 * 无效文件路径异常。
 * 当文件路径不符合规范时抛出此异常。
 * 继承自BusinessException，表示业务层面的文件路径验证错误。
 *
 * 使用场景：
 * - 文件路径包含非法字符
 * - 文件路径不存在
 * - 文件路径格式错误
 * - 文件路径访问越权
 *
 * @author john
 * @date 2020-01-11
 */
public class InvalidFilePathException extends BusinessException {
    private static final long serialVersionUID = -1731288043175680006L;

    /**
     * 默认构造函数。
     */
    public InvalidFilePathException() {
        super(ErrorCode.INVALID_FILE_PATH);
    }

    /**
     * 构造函数，指定异常消息。
     *
     * @param message 异常消息
     */
    public InvalidFilePathException(String message) {
        super(ErrorCode.INVALID_FILE_PATH, message);
    }

    /**
     * 构造函数，指定异常消息和原因。
     *
     * @param message 异常消息
     * @param cause   异常原因
     */
    public InvalidFilePathException(String message, Throwable cause) {
        super(ErrorCode.INVALID_FILE_PATH, message, cause);
    }

    /**
     * 构造函数，指定异常原因。
     *
     * @param cause 异常原因
     */
    public InvalidFilePathException(Throwable cause) {
        super(ErrorCode.INVALID_FILE_PATH, cause);
    }
}
