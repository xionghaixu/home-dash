package com.hd.exception;

/**
 * 统一错误码枚举。
 * 定义系统中所有可能的错误码，便于统一管理和问题定位。
 *
 * <h3>错误码分类：</h3>
 * <ul>
 * <li>2xx：成功状态码</li>
 * <li>4xx：客户端错误（HTTP标准状态码）</li>
 * <li>5xx：业务错误（1001-1999）- 业务逻辑相关错误</li>
 * <li>6xx：系统错误（501-599）- 系统级别错误</li>
 * </ul>
 *
 * @author john
 * @date 2020-01-11
 */
public enum ErrorCode {

    // ========== 成功状态码 ==========
    SUCCESS(200, "操作成功"),

    // ========== 客户端错误 4xx ==========
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权访问"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),
    CONFLICT(409, "资源冲突"),
    UNSUPPORTED_MEDIA_TYPE(415, "不支持的媒体类型"),
    UNPROCESSABLE_ENTITY(422, "无法处理的实体"),
    TOO_MANY_REQUESTS(429, "请求过于频繁"),

    // ========== 业务错误 5xx (1001-1099: 数据相关) ==========
    DATA_FORMAT_ERROR(1001, "数据格式错误"),
    DATA_NOT_FOUND(1002, "数据不存在"),
    DATA_DUPLICATE(1003, "数据重复"),
    DATA_VALIDATION_FAILED(1004, "数据验证失败"),

    // ========== 业务错误 5xx (1100-1199: 文件相关) ==========
    FILE_NOT_FOUND(1101, "文件不存在"),
    FILE_ALREADY_EXISTS(1102, "文件已存在"),
    FILE_OPERATION_FAILED(1103, "文件操作失败"),
    UPLOAD_FAILED(1104, "文件上传失败"),
    DOWNLOAD_FAILED(1105, "文件下载失败"),
    INVALID_FILE_NAME(1106, "无效的文件名"),
    INVALID_FILE_PATH(1107, "无效的文件路径"),
    FILE_SIZE_EXCEEDED(1108, "文件大小超出限制"),
    FILE_TYPE_NOT_ALLOWED(1109, "文件类型不允许"),
    FILE_CORRUPTED(1110, "文件已损坏"),
    FILE_IN_USE(1111, "文件正在使用中"),

    // ========== 业务错误 5xx (1200-1299: 文件夹相关) ==========
    FOLDER_NOT_FOUND(1201, "文件夹不存在"),
    FOLDER_ALREADY_EXISTS(1202, "文件夹已存在"),
    INVALID_FOLDER_NAME(1203, "无效的文件夹名"),
    FOLDER_NOT_EMPTY(1204, "文件夹不为空"),
    FOLDER_OPERATION_FAILED(1205, "文件夹操作失败"),

    // ========== 业务错误 5xx (1300-1399: 资源相关) ==========
    RESOURCE_NOT_FOUND(1301, "资源不存在"),
    RESOURCE_ALREADY_EXISTS(1302, "资源已存在"),
    RESOURCE_OPERATION_FAILED(1303, "资源操作失败"),
    RESOURCE_EXPIRED(1304, "资源已过期"),
    RESOURCE_QUOTA_EXCEEDED(1305, "资源配额超出限制"),

    // ========== 业务错误 5xx (1400-1499: 权限和认证相关) ==========
    PERMISSION_DENIED(1401, "权限不足"),
    TOKEN_EXPIRED(1402, "令牌已过期"),
    TOKEN_INVALID(1403, "令牌无效"),
    ACCOUNT_DISABLED(1404, "账户已禁用"),
    ACCOUNT_LOCKED(1405, "账户已锁定"),

    // ========== 业务错误 5xx (1500-1599: 上传下载相关) ==========
    UPLOAD_INTERRUPTED(1501, "上传中断"),
    UPLOAD_CHUNK_FAILED(1502, "分块上传失败"),
    UPLOAD_SIZE_EXCEEDED(1503, "上传大小超限"),
    UPLOAD_TYPE_NOT_ALLOWED(1504, "上传类型不允许"),
    DOWNLOAD_INTERRUPTED(1505, "下载中断"),
    DOWNLOAD_PERMISSION_DENIED(1506, "下载权限不足"),
    DOWNLOAD_SPEED_LIMITED(1507, "下载速度受限"),

    // ========== 系统错误 6xx ==========
    INTERNAL_SERVER_ERROR(500, "服务器内部错误"),
    DATABASE_ERROR(501, "数据库操作失败"),
    IO_ERROR(502, "IO操作失败"),
    NETWORK_ERROR(503, "网络错误"),
    SYSTEM_BUSY(504, "系统繁忙，请稍后再试"),
    CONFIGURATION_ERROR(505, "配置错误"),
    SERVICE_UNAVAILABLE(506, "服务不可用"),
    TIMEOUT_ERROR(507, "操作超时"),
    INSUFFICIENT_STORAGE(508, "存储空间不足"),
    CONCURRENT_MODIFICATION(509, "并发修改冲突");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 获取错误码数值。
     *
     * @return 错误码数值
     */
    public int getCode() {
        return code;
    }

    /**
     * 获取错误消息。
     *
     * @return 错误消息
     */
    public String getMessage() {
        return message;
    }

    /**
     * 判断是否为成功状态码。
     *
     * @return 如果是成功状态码返回true
     */
    public boolean isSuccess() {
        return this == SUCCESS;
    }

    /**
     * 判断是否为客户端错误（4xx）。
     *
     * @return 如果是客户端错误返回true
     */
    public boolean isClientError() {
        return code >= 400 && code < 500;
    }

    /**
     * 判断是否为业务错误（1001-1999）。
     *
     * @return 如果是业务错误返回true
     */
    public boolean isBusinessError() {
        return code >= 1001 && code <= 1999;
    }

    /**
     * 判断是否为系统错误（501-599）。
     *
     * @return 如果是系统错误返回true
     */
    public boolean isSystemError() {
        return code >= 501 && code <= 599;
    }

    @Override
    public String toString() {
        return String.format("[%d] %s", code, message);
    }
}
