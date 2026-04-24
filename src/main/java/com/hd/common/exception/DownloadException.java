package com.hd.common.exception;

import com.hd.common.enums.ErrorCode;

/**
 * @author xhx
 * @version 1.0
 * @package com.hd.common.exception
 * @createTime 2026/04/23 23:34
 * @description 文件下载失败异常。当文件下载过程中发生错误时抛出此异常。继承自BusinessException，表示业务层面的文件下载错误。
 *
 * 使用场景：
 * - 文件下载过程中断
 * - 下载文件不存在
 * - 下载文件权限不足
 * - 下载文件损坏
 */
public class DownloadException extends BusinessException {
    private static final long serialVersionUID = -1731288043175680004L;

    /**
     * 默认构造函数。
     */
    public DownloadException() {
        super(ErrorCode.DOWNLOAD_FAILED);
    }

    /**
     * 构造函数，指定异常消息。
     *
     * @param message 异常消息
     */
    public DownloadException(String message) {
        super(ErrorCode.DOWNLOAD_FAILED, message);
    }

    /**
     * 构造函数，指定异常消息和原因。
     *
     * @param message 异常消息
     * @param cause   异常原因
     */
    public DownloadException(String message, Throwable cause) {
        super(ErrorCode.DOWNLOAD_FAILED, message, cause);
    }

    /**
     * 构造函数，指定异常原因。
     *
     * @param cause 异常原因
     */
    public DownloadException(Throwable cause) {
        super(ErrorCode.DOWNLOAD_FAILED, cause);
    }
}
