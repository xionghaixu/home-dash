package com.hd.exception;

/**
 * 文件操作失败异常。
 * 当文件操作（移动、复制、删除等）失败时抛出此异常。
 * 继承自BusinessException，表示业务层面的文件操作错误。
 *
 * 使用场景：
 * - 文件移动失败
 * - 文件复制失败
 * - 文件删除失败
 * - 文件重命名失败
 *
 * @author john
 * @date 2020-01-11
 */
public class FileOperationException extends BusinessException {
    private static final long serialVersionUID = -1731288043175680002L;

    /**
     * 默认构造函数。
     */
    public FileOperationException() {
        super(ErrorCode.FILE_OPERATION_FAILED);
    }

    /**
     * 构造函数，指定异常消息。
     *
     * @param message 异常消息
     */
    public FileOperationException(String message) {
        super(ErrorCode.FILE_OPERATION_FAILED, message);
    }

    /**
     * 构造函数，指定异常消息和原因。
     *
     * @param message 异常消息
     * @param cause   异常原因
     */
    public FileOperationException(String message, Throwable cause) {
        super(ErrorCode.FILE_OPERATION_FAILED, message, cause);
    }

    /**
     * 构造函数，指定异常原因。
     *
     * @param cause 异常原因
     */
    public FileOperationException(Throwable cause) {
        super(ErrorCode.FILE_OPERATION_FAILED, cause);
    }
}
