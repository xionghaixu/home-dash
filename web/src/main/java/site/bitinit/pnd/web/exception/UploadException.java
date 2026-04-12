package site.bitinit.pnd.web.exception;

/**
 * 文件上传失败异常。
 * 当文件上传过程中发生错误时抛出此异常。
 * 继承自BusinessException，表示业务层面的文件上传错误。
 *
 * 使用场景：
 * - 文件上传过程中断
 * - 文件分块上传失败
 * - 上传文件大小超限
 * - 上传文件类型不支持
 *
 * @author john
 * @date 2020-01-11
 */
public class UploadException extends BusinessException {
    private static final long serialVersionUID = -1731288043175680003L;

    /**
     * 默认构造函数。
     */
    public UploadException() {
        super(ErrorCode.UPLOAD_FAILED);
    }

    /**
     * 构造函数，指定异常消息。
     *
     * @param message 异常消息
     */
    public UploadException(String message) {
        super(ErrorCode.UPLOAD_FAILED, message);
    }

    /**
     * 构造函数，指定异常消息和原因。
     *
     * @param message 异常消息
     * @param cause   异常原因
     */
    public UploadException(String message, Throwable cause) {
        super(ErrorCode.UPLOAD_FAILED, message, cause);
    }

    /**
     * 构造函数，指定异常原因。
     *
     * @param cause 异常原因
     */
    public UploadException(Throwable cause) {
        super(ErrorCode.UPLOAD_FAILED, cause);
    }
}
