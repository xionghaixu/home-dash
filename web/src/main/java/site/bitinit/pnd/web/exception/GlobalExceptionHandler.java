package site.bitinit.pnd.web.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import site.bitinit.pnd.web.controller.dto.ResponseDto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * 全局异常处理器。
 * 统一处理控制器层抛出的各类异常，返回标准化的错误响应。
 * 处理的异常类型包括业务异常、系统异常和常见的Spring框架异常。
 *
 * <h3>核心功能：</h3>
 * <ul>
 * <li>统一异常处理机制，减少重复代码</li>
 * <li>详细的日志记录，便于问题定位</li>
 * <li>标准化的错误响应格式</li>
 * <li>支持异常上下文数据的输出</li>
 * </ul>
 *
 * @author john
 * @date 2020-01-11
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理PND基础异常。
     * 作为所有PND业务异常的兜底处理器。
     *
     * @param e PND基础异常
     * @return 包含错误消息的响应对象
     */
    @ExceptionHandler(PndException.class)
    public ResponseEntity<ResponseDto> pndException(PndException e) {
        // 根据异常类型判断是业务异常还是系统异常
        if (e instanceof SystemException) {
            log.error("[pnd-system-error] code={}, message={}, detailedMessage={}",
                    e.getCode(), e.getMessage(), e.getDetailedMessage(), e);
        } else {
            log.warn("[pnd-business-error] code={}, message={}, detailedMessage={}",
                    e.getCode(), e.getMessage(), e.getDetailedMessage());
        }

        HttpStatus httpStatus = mapErrorCodeToHttpStatus(e.getErrorCode());
        return ResponseEntity
                .status(httpStatus)
                .body(ResponseDto.fail(e.getErrorCode(), e.getMessage()));
    }

    /**
     * 处理业务异常。
     * 记录警告日志并返回对应的HTTP状态码和错误消息。
     *
     * @param e 业务异常
     * @return 包含错误消息的响应对象
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ResponseDto> businessException(BusinessException e) {
        log.warn("[business-error] code={}, message={}, detailedMessage={}",
                e.getCode(), e.getMessage(), getDetailedMessage(e));

        HttpStatus httpStatus = mapBusinessExceptionToHttpStatus(e);
        return ResponseEntity
                .status(httpStatus)
                .body(ResponseDto.fail(e.getErrorCode(), e.getMessage()));
    }

    /**
     * 处理系统异常。
     * 记录错误日志并返回HTTP 500状态码。
     *
     * @param e 系统异常
     * @return 包含错误消息的响应对象
     */
    @ExceptionHandler(SystemException.class)
    public ResponseEntity<ResponseDto> systemException(SystemException e) {
        log.error("[system-error] code={}, message={}, detailedMessage={}",
                e.getCode(), e.getMessage(), getDetailedMessage(e), e);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseDto.fail(e.getErrorCode(), e.getMessage()));
    }

    /**
     * 处理数据格式异常。
     * 返回HTTP 400状态码和错误消息。
     *
     * @param e 数据格式异常
     * @return 包含错误消息的响应对象
     */
    @ExceptionHandler(DataFormatException.class)
    public ResponseEntity<ResponseDto> dataFormatException(DataFormatException e) {
        log.warn("[data-format-error] message={}", getDetailedMessage(e));
        return ResponseEntity.badRequest()
                .body(ResponseDto.fail(ErrorCode.DATA_FORMAT_ERROR, e.getMessage()));
    }

    /**
     * 处理数据未找到异常。
     * 返回HTTP 404状态码和错误消息。
     *
     * @param e 数据未找到异常
     * @return 包含错误消息的响应对象
     */
    @ExceptionHandler(DataNotFoundException.class)
    public ResponseEntity<ResponseDto> dataNotFoundException(DataNotFoundException e) {
        log.warn("[data-not-found] message={}", getDetailedMessage(e));
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ResponseDto.fail(ErrorCode.DATA_NOT_FOUND, e.getMessage()));
    }

    /**
     * 处理文件未找到异常。
     * 返回HTTP 404状态码和错误消息。
     *
     * @param e 文件未找到异常
     * @return 包含错误消息的响应对象
     */
    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<ResponseDto> fileNotFoundException(FileNotFoundException e) {
        log.warn("[file-not-found] message={}", getDetailedMessage(e));
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ResponseDto.fail(ErrorCode.FILE_NOT_FOUND, e.getMessage()));
    }

    /**
     * 处理文件已存在异常。
     * 返回HTTP 409状态码和错误消息。
     *
     * @param e 文件已存在异常
     * @return 包含错误消息的响应对象
     */
    @ExceptionHandler(FileAlreadyExistsException.class)
    public ResponseEntity<ResponseDto> fileAlreadyExistsException(FileAlreadyExistsException e) {
        log.warn("[file-already-exists] message={}", getDetailedMessage(e));
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ResponseDto.fail(ErrorCode.FILE_ALREADY_EXISTS, e.getMessage()));
    }

    /**
     * 处理文件操作失败异常。
     * 返回HTTP 500状态码和错误消息。
     *
     * @param e 文件操作失败异常
     * @return 包含错误消息的响应对象
     */
    @ExceptionHandler(FileOperationException.class)
    public ResponseEntity<ResponseDto> fileOperationException(FileOperationException e) {
        log.error("[file-operation-failed] message={}", getDetailedMessage(e), e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseDto.fail(ErrorCode.FILE_OPERATION_FAILED, e.getMessage()));
    }

    /**
     * 处理上传失败异常。
     * 返回HTTP 500状态码和错误消息。
     *
     * @param e 上传失败异常
     * @return 包含错误消息的响应对象
     */
    @ExceptionHandler(UploadException.class)
    public ResponseEntity<ResponseDto> uploadException(UploadException e) {
        log.error("[upload-failed] message={}", getDetailedMessage(e), e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseDto.fail(ErrorCode.UPLOAD_FAILED, e.getMessage()));
    }

    /**
     * 处理下载失败异常。
     * 返回HTTP 500状态码和错误消息。
     *
     * @param e 下载失败异常
     * @return 包含错误消息的响应对象
     */
    @ExceptionHandler(DownloadException.class)
    public ResponseEntity<ResponseDto> downloadException(DownloadException e) {
        log.error("[download-failed] message={}", getDetailedMessage(e), e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseDto.fail(ErrorCode.DOWNLOAD_FAILED, e.getMessage()));
    }

    /**
     * 处理无效文件名异常。
     * 返回HTTP 400状态码和错误消息。
     *
     * @param e 无效文件名异常
     * @return 包含错误消息的响应对象
     */
    @ExceptionHandler(InvalidFileNameException.class)
    public ResponseEntity<ResponseDto> invalidFileNameException(InvalidFileNameException e) {
        log.warn("[invalid-file-name] message={}", getDetailedMessage(e));
        return ResponseEntity.badRequest()
                .body(ResponseDto.fail(ErrorCode.INVALID_FILE_NAME, e.getMessage()));
    }

    /**
     * 处理无效文件路径异常。
     * 返回HTTP 400状态码和错误消息。
     *
     * @param e 无效文件路径异常
     * @return 包含错误消息的响应对象
     */
    @ExceptionHandler(InvalidFilePathException.class)
    public ResponseEntity<ResponseDto> invalidFilePathException(InvalidFilePathException e) {
        log.warn("[invalid-file-path] message={}", getDetailedMessage(e));
        return ResponseEntity.badRequest()
                .body(ResponseDto.fail(ErrorCode.INVALID_FILE_PATH, e.getMessage()));
    }

    /**
     * 处理数据库异常。
     * 记录错误日志并返回HTTP 500状态码。
     *
     * @param e 数据库异常
     * @return 包含错误消息的响应对象
     */
    @ExceptionHandler(DatabaseException.class)
    public ResponseEntity<ResponseDto> databaseException(DatabaseException e) {
        log.error("[database-error] message={}, timestamp={}",
                getDetailedMessage(e), LocalDateTime.now(), e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseDto.fail(ErrorCode.DATABASE_ERROR, "数据库操作失败，请联系管理员"));
    }

    /**
     * 处理存储异常。
     * 记录错误日志并返回HTTP 500状态码。
     *
     * @param e 存储异常
     * @return 包含错误消息的响应对象
     */
    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ResponseDto> storageException(StorageException e) {
        log.error("[storage-error] message={}, timestamp={}",
                getDetailedMessage(e), LocalDateTime.now(), e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseDto.fail(ErrorCode.IO_ERROR, "存储操作失败，请联系管理员"));
    }

    /**
     * 处理网络异常。
     * 记录错误日志并返回HTTP 503状态码。
     *
     * @param e 网络异常
     * @return 包含错误消息的响应对象
     */
    @ExceptionHandler(NetworkException.class)
    public ResponseEntity<ResponseDto> networkException(NetworkException e) {
        log.error("[network-error] message={}, timestamp={}",
                getDetailedMessage(e), LocalDateTime.now(), e);
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ResponseDto.fail(ErrorCode.NETWORK_ERROR, "网络操作失败，请稍后重试"));
    }

    /**
     * 处理请求参数验证失败异常（@Valid验证失败）。
     *
     * @param e 方法参数验证异常
     * @return 包含验证错误信息的响应对象
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseDto> methodArgumentNotValidException(MethodArgumentNotValidException e) {
        String errors = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        log.warn("[validation-error] {}", errors);
        return ResponseEntity.badRequest()
                .body(ResponseDto.fail(ErrorCode.BAD_REQUEST, errors));
    }

    /**
     * 处理绑定异常（表单提交验证失败）。
     *
     * @param e 绑定异常
     * @return 包含验证错误信息的响应对象
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ResponseDto> bindException(BindException e) {
        String errors = e.getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        log.warn("[bind-error] {}", errors);
        return ResponseEntity.badRequest()
                .body(ResponseDto.fail(ErrorCode.BAD_REQUEST, errors));
    }

    /**
     * 处理约束违反异常（@Validated验证失败）。
     *
     * @param e 约束违反异常
     * @return 包含验证错误信息的响应对象
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ResponseDto> constraintViolationException(ConstraintViolationException e) {
        String errors = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));

        log.warn("[constraint-violation] {}", errors);
        return ResponseEntity.badRequest()
                .body(ResponseDto.fail(ErrorCode.BAD_REQUEST, errors));
    }

    /**
     * 处理缺少请求参数异常。
     *
     * @param e 缺少请求参数异常
     * @return 包含错误信息的响应对象
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ResponseDto> missingServletRequestParameterException(
            MissingServletRequestParameterException e) {
        String message = String.format("缺少必需的请求参数: %s", e.getParameterName());
        log.warn("[missing-parameter] {}", message);
        return ResponseEntity.badRequest()
                .body(ResponseDto.fail(ErrorCode.BAD_REQUEST, message));
    }

    /**
     * 处理参数类型不匹配异常。
     *
     * @param e 参数类型不匹配异常
     * @return 包含错误信息的响应对象
     */
    @ExceptionHandler({ MethodArgumentTypeMismatchException.class, TypeMismatchException.class })
    public ResponseEntity<ResponseDto> typeMismatchException(Exception e) {
        String message = "参数类型不匹配";
        if (e instanceof MethodArgumentTypeMismatchException) {
            MethodArgumentTypeMismatchException ex = (MethodArgumentTypeMismatchException) e;
            message = String.format("参数 '%s' 类型不匹配，期望类型: %s",
                    ex.getName(), ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "未知");
        }

        log.warn("[type-mismatch] {}", message);
        return ResponseEntity.badRequest()
                .body(ResponseDto.fail(ErrorCode.BAD_REQUEST, message));
    }

    /**
     * 处理请求体不可读异常（JSON解析错误）。
     *
     * @param e 请求体不可读异常
     * @return 包含错误信息的响应对象
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ResponseDto> httpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("[message-not-readable] {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(ResponseDto.fail(ErrorCode.BAD_REQUEST, "请求体格式错误或无法解析"));
    }

    /**
     * 处理请求方法不支持异常。
     *
     * @param e 请求方法不支持异常
     * @return 包含错误信息的响应对象
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ResponseDto> httpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e) {
        String message = String.format("不支持 '%s' 请求方法，支持的方法: %s",
                e.getMethod(), String.join(", ", e.getSupportedMethods()));

        log.warn("[method-not-supported] {}", message);
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ResponseDto.fail(ErrorCode.METHOD_NOT_ALLOWED, message));
    }

    /**
     * 处理不支持的媒体类型异常。
     *
     * @param e 不支持的媒体类型异常
     * @return 包含错误信息的响应对象
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ResponseDto> httpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e) {
        String message = String.format("不支持的媒体类型: %s", e.getContentType());
        log.warn("[media-type-not-supported] {}", message);
        return ResponseEntity
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ResponseDto.fail(ErrorCode.UNSUPPORTED_MEDIA_TYPE, message));
    }

    /**
     * 处理404异常。
     *
     * @param e 404异常
     * @return 包含错误信息的响应对象
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ResponseDto> noHandlerFoundException(NoHandlerFoundException e) {
        String message = String.format("请求的资源不存在: %s %s", e.getHttpMethod(), e.getRequestURL());
        log.warn("[not-found] {}", message);
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ResponseDto.fail(ErrorCode.NOT_FOUND, message));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ResponseDto> noResourceFoundException(NoResourceFoundException e) {
        String message = String.format("请求的资源不存在: %s", e.getResourcePath());
        log.warn("[resource-not-found] {}", message);
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ResponseDto.fail(ErrorCode.NOT_FOUND, message));
    }

    /**
     * 处理通用异常。
     * 记录错误日志并返回HTTP 500状态码。
     *
     * @param e 异常对象
     * @return 包含通用错误消息的响应对象
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseDto> exception(Exception e) {
        log.error("[unhandled-error] class={}, message={}, timestamp={}",
                e.getClass().getSimpleName(), e.getMessage(), LocalDateTime.now(), e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseDto.fail(ErrorCode.INTERNAL_SERVER_ERROR, "服务器内部错误，请联系管理员"));
    }

    /**
     * 获取异常的详细信息（包含上下文数据）。
     *
     * @param exception PND异常对象
     * @return 包含上下文信息的详细消息
     */
    private String getDetailedMessage(PndException exception) {
        return exception.getDetailedMessage();
    }

    /**
     * 将错误码映射为HTTP状态码。
     * 提供通用的错误码到HTTP状态码的映射逻辑。
     *
     * @param errorCode 错误码枚举
     * @return 对应的HTTP状态码
     */
    private HttpStatus mapErrorCodeToHttpStatus(ErrorCode errorCode) {
        if (errorCode == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }

        // 根据错误码范围映射HTTP状态码
        int code = errorCode.getCode();

        // 4xx 客户端错误
        if (code >= 400 && code < 500) {
            switch (code) {
                case 400: return HttpStatus.BAD_REQUEST;
                case 401: return HttpStatus.UNAUTHORIZED;
                case 403: return HttpStatus.FORBIDDEN;
                case 404: return HttpStatus.NOT_FOUND;
                case 405: return HttpStatus.METHOD_NOT_ALLOWED;
                case 409: return HttpStatus.CONFLICT;
                case 415: return HttpStatus.UNSUPPORTED_MEDIA_TYPE;
                case 422: return HttpStatus.UNPROCESSABLE_ENTITY;
                default: return HttpStatus.BAD_REQUEST;
            }
        }

        // 5xx 业务错误
        if (code >= 1001 && code <= 1999) {
            switch (errorCode) {
                // 数据相关 (1001-1099)
                case DATA_FORMAT_ERROR:
                case DATA_VALIDATION_FAILED:
                    return HttpStatus.BAD_REQUEST;
                case DATA_NOT_FOUND:
                    return HttpStatus.NOT_FOUND;
                case DATA_DUPLICATE:
                    return HttpStatus.CONFLICT;

                // 文件相关 (1100-1199)
                case INVALID_FILE_NAME:
                case INVALID_FILE_PATH:
                case FILE_SIZE_EXCEEDED:
                case FILE_TYPE_NOT_ALLOWED:
                    return HttpStatus.BAD_REQUEST;
                case FILE_NOT_FOUND:
                    return HttpStatus.NOT_FOUND;
                case FILE_ALREADY_EXISTS:
                    return HttpStatus.CONFLICT;
                case FILE_OPERATION_FAILED:
                case UPLOAD_FAILED:
                case DOWNLOAD_FAILED:
                    return HttpStatus.INTERNAL_SERVER_ERROR;

                // 文件夹相关 (1200-1299)
                case INVALID_FOLDER_NAME:
                    return HttpStatus.BAD_REQUEST;
                case FOLDER_NOT_FOUND:
                    return HttpStatus.NOT_FOUND;
                case FOLDER_ALREADY_EXISTS:
                    return HttpStatus.CONFLICT;
                case FOLDER_NOT_EMPTY:
                case FOLDER_OPERATION_FAILED:
                    return HttpStatus.INTERNAL_SERVER_ERROR;

                // 资源相关 (1300-1399)
                case RESOURCE_NOT_FOUND:
                    return HttpStatus.NOT_FOUND;
                case RESOURCE_ALREADY_EXISTS:
                    return HttpStatus.CONFLICT;
                case RESOURCE_EXPIRED:
                case RESOURCE_QUOTA_EXCEEDED:
                case RESOURCE_OPERATION_FAILED:
                    return HttpStatus.INTERNAL_SERVER_ERROR;

                // 权限和认证相关 (1400-1499)
                case PERMISSION_DENIED:
                    return HttpStatus.FORBIDDEN;
                case TOKEN_EXPIRED:
                case TOKEN_INVALID:
                case ACCOUNT_DISABLED:
                case ACCOUNT_LOCKED:
                    return HttpStatus.UNAUTHORIZED;

                // 上传下载相关 (1500-1599)
                case UPLOAD_INTERRUPTED:
                case UPLOAD_CHUNK_FAILED:
                case UPLOAD_SIZE_EXCEEDED:
                case UPLOAD_TYPE_NOT_ALLOWED:
                case DOWNLOAD_INTERRUPTED:
                case DOWNLOAD_PERMISSION_DENIED:
                case DOWNLOAD_SPEED_LIMITED:
                    return HttpStatus.INTERNAL_SERVER_ERROR;

                default:
                    return HttpStatus.BAD_REQUEST;
            }
        }

        // 6xx 系统错误
        if (code >= 501 && code <= 599) {
            switch (code) {
                case 503: return HttpStatus.SERVICE_UNAVAILABLE;
                default: return HttpStatus.INTERNAL_SERVER_ERROR;
            }
        }

        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    /**
     * 将业务异常映射为HTTP状态码。
     *
     * @param e 业务异常
     * @return 对应的HTTP状态码
     */
    private HttpStatus mapBusinessExceptionToHttpStatus(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        return mapErrorCodeToHttpStatus(errorCode);
    }
}
