package com.hd.exception;

/**
 * 数据未找到异常。
 * 当请求的数据在数据库中不存在时抛出此异常。
 * 继承自BusinessException，表示业务层面的数据未找到错误。
 *
 * @author john
 * @date 2020-01-11
 */
public class DataNotFoundException extends BusinessException {
    private static final long serialVersionUID = -1731288043175679993L;

    /**
     * 默认构造函数。
     */
    public DataNotFoundException() {
        super(ErrorCode.DATA_NOT_FOUND);
    }

    /**
     * 构造函数，指定异常消息。
     *
     * @param message 异常消息
     */
    public DataNotFoundException(String message) {
        super(ErrorCode.DATA_NOT_FOUND, message);
    }

    /**
     * 构造函数，指定异常消息和原因。
     *
     * @param message 异常消息
     * @param cause   异常原因
     */
    public DataNotFoundException(String message, Throwable cause) {
        super(ErrorCode.DATA_NOT_FOUND, message, cause);
    }

    /**
     * 构造函数，指定异常原因。
     *
     * @param cause 异常原因
     */
    public DataNotFoundException(Throwable cause) {
        super(ErrorCode.DATA_NOT_FOUND, cause);
    }
}
