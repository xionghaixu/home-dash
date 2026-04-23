package com.hd.common.exception;

import com.hd.common.enums.ErrorCode;

/**
 * 数据格式异常。
 * 当请求数据格式不正确或验证失败时抛出此异常。
 * 继承自BusinessException，表示业务层面的数据格式错误。
 *
 * @author john
 * @date 2020-01-11
 */
public class DataFormatException extends BusinessException {
    private static final long serialVersionUID = 586909787278516210L;

    /**
     * 默认构造函数。
     */
    public DataFormatException() {
        super(ErrorCode.DATA_FORMAT_ERROR);
    }

    /**
     * 构造函数，指定异常消息。
     *
     * @param message 异常消息
     */
    public DataFormatException(String message) {
        super(ErrorCode.DATA_FORMAT_ERROR, message);
    }

    /**
     * 构造函数，指定异常消息和原因。
     *
     * @param message 异常消息
     * @param cause   异常原因
     */
    public DataFormatException(String message, Throwable cause) {
        super(ErrorCode.DATA_FORMAT_ERROR, message, cause);
    }

    /**
     * 构造函数，指定异常原因。
     *
     * @param cause 异常原因
     */
    public DataFormatException(Throwable cause) {
        super(ErrorCode.DATA_FORMAT_ERROR, cause);
    }
}
