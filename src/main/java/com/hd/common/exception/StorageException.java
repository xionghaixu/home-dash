package com.hd.common.exception;

import com.hd.common.enums.ErrorCode;

/**
 * 存储操作异常。
 * 当文件存储操作失败时抛出此异常。
 * 继承自SystemException，表示系统层面的存储错误。
 *
 * 使用场景：
 * - 文件读写失败
 * - 磁盘空间不足
 * - 文件系统错误
 * - 存储路径访问失败
 *
 * @author john
 * @date 2020-01-11
 */
public class StorageException extends SystemException {
    private static final long serialVersionUID = -1731288043175680008L;

    /**
     * 默认构造函数。
     */
    public StorageException() {
        super(ErrorCode.IO_ERROR);
    }

    /**
     * 构造函数，指定异常消息。
     *
     * @param message 异常消息
     */
    public StorageException(String message) {
        super(ErrorCode.IO_ERROR, message);
    }

    /**
     * 构造函数，指定异常消息和原因。
     *
     * @param message 异常消息
     * @param cause   异常原因
     */
    public StorageException(String message, Throwable cause) {
        super(ErrorCode.IO_ERROR, message, cause);
    }

    /**
     * 构造函数，指定异常原因。
     *
     * @param cause 异常原因
     */
    public StorageException(Throwable cause) {
        super(ErrorCode.IO_ERROR, cause);
    }
}
