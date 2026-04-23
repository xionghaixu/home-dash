package com.hd.common.exception;

import com.hd.common.enums.ErrorCode;

/**
 * 数据库操作异常。
 * 当数据库操作失败时抛出此异常。
 * 继承自SystemException，表示系统层面的数据库错误。
 *
 * 使用场景：
 * - 数据库连接失败
 * - SQL执行错误
 * - 数据库事务失败
 * - 数据库约束违反
 *
 * @author john
 * @date 2020-01-11
 */
public class DatabaseException extends SystemException {
    private static final long serialVersionUID = -1731288043175680007L;

    /**
     * 默认构造函数。
     */
    public DatabaseException() {
        super(ErrorCode.DATABASE_ERROR);
    }

    /**
     * 构造函数，指定异常消息。
     *
     * @param message 异常消息
     */
    public DatabaseException(String message) {
        super(ErrorCode.DATABASE_ERROR, message);
    }

    /**
     * 构造函数，指定异常消息和原因。
     *
     * @param message 异常消息
     * @param cause   异常原因
     */
    public DatabaseException(String message, Throwable cause) {
        super(ErrorCode.DATABASE_ERROR, message, cause);
    }

    /**
     * 构造函数，指定异常原因。
     *
     * @param cause 异常原因
     */
    public DatabaseException(Throwable cause) {
        super(ErrorCode.DATABASE_ERROR, cause);
    }
}
