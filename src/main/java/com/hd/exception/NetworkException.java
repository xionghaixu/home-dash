package com.hd.exception;

/**
 * 网络操作异常。
 * 当网络操作失败时抛出此异常。
 * 继承自SystemException，表示系统层面的网络错误。
 *
 * 使用场景：
 * - 网络连接失败
 * - 网络超时
 * - 网络传输中断
 * - 远程服务不可用
 *
 * @author john
 * @date 2020-01-11
 */
public class NetworkException extends SystemException {
    private static final long serialVersionUID = -1731288043175680009L;

    /**
     * 默认构造函数。
     */
    public NetworkException() {
        super(ErrorCode.NETWORK_ERROR);
    }

    /**
     * 构造函数，指定异常消息。
     *
     * @param message 异常消息
     */
    public NetworkException(String message) {
        super(ErrorCode.NETWORK_ERROR, message);
    }

    /**
     * 构造函数，指定异常消息和原因。
     *
     * @param message 异常消息
     * @param cause   异常原因
     */
    public NetworkException(String message, Throwable cause) {
        super(ErrorCode.NETWORK_ERROR, message, cause);
    }

    /**
     * 构造函数，指定异常原因。
     *
     * @param cause 异常原因
     */
    public NetworkException(Throwable cause) {
        super(ErrorCode.NETWORK_ERROR, cause);
    }
}
