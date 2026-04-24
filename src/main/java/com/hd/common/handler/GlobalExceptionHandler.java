package com.hd.common.handler;
import com.hd.common.enums.ErrorCode;
import com.hd.common.exception.BusinessException;
import com.hd.common.exception.HomeDashException;
import com.hd.common.exception.SystemException;
import com.hd.model.dto.ResponseDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
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
import java.util.stream.Collectors;
/**
 * @author xhx
 * @version 1.0
 * @package com.hd.common.handler
 * @createTime 2026/04/23 23:34
 * @description 全局异常处理器。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(HomeDashException.class)
    public ResponseEntity<ResponseDto> handleHomeDashException(HomeDashException e) {
        if (e instanceof SystemException) {
            log.error("[home-dash-error] code={}, message={}", e.getCode(), e.getDetailedMessage(), e);
        } else {
            log.warn("[home-dash-error] code={}, message={}", e.getCode(), e.getDetailedMessage());
        }
        return ResponseEntity.status(mapErrorCodeToHttpStatus(e.getErrorCode()))
                .body(ResponseDto.fail(e.getErrorCode(), e.getDetailedMessage()));
    }
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ResponseDto> handleBusinessException(BusinessException e) {
        return ResponseEntity.status(mapErrorCodeToHttpStatus(e.getErrorCode()))
                .body(ResponseDto.fail(e.getErrorCode(), e.getDetailedMessage()));
    }
    @ExceptionHandler(SystemException.class)
    public ResponseEntity<ResponseDto> handleSystemException(SystemException e) {
        log.error("[system-error] code={}, message={}", e.getCode(), e.getDetailedMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseDto.fail(e.getErrorCode(), e.getDetailedMessage()));
    }
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ResponseDto> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(ResponseDto.fail(ErrorCode.BAD_REQUEST, message));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseDto> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(ResponseDto.fail(ErrorCode.BAD_REQUEST, message));
    }
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ResponseDto> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(ResponseDto.fail(ErrorCode.BAD_REQUEST, message));
    }
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ResponseDto> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException e) {
        String message = String.format("缺少请求参数: %s", e.getParameterName());
        return ResponseEntity.badRequest().body(ResponseDto.fail(ErrorCode.BAD_REQUEST, message));
    }
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, TypeMismatchException.class})
    public ResponseEntity<ResponseDto> handleTypeMismatchException(Exception e) {
        String message = "请求参数类型不匹配";
        if (e instanceof MethodArgumentTypeMismatchException ex) {
            message = String.format("参数 '%s' 类型不匹配，期望类型: %s",
                    ex.getName(), ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "未知");
        }
        return ResponseEntity.badRequest().body(ResponseDto.fail(ErrorCode.BAD_REQUEST, message));
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ResponseDto> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(ResponseDto.fail(ErrorCode.BAD_REQUEST, "请求体格式错误，无法解析"));
    }
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ResponseDto> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e) {
        String supportedMethods = e.getSupportedMethods() == null
                ? ""
                : String.join(", ", e.getSupportedMethods());
        String message = String.format("不支持 '%s' 请求方法，支持的方法: %s", e.getMethod(), supportedMethods);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ResponseDto.fail(ErrorCode.METHOD_NOT_ALLOWED, message));
    }
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ResponseDto> handleHttpMediaTypeNotSupportedException(
            HttpMediaTypeNotSupportedException e) {
        String message = String.format("不支持的媒体类型: %s", e.getContentType());
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ResponseDto.fail(ErrorCode.UNSUPPORTED_MEDIA_TYPE, message));
    }
    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ResponseDto> handleNotFoundException(Exception e) {
        String message = e instanceof NoHandlerFoundException notFound
                ? String.format("请求资源不存在: %s %s", notFound.getHttpMethod(), notFound.getRequestURL())
                : String.format("请求资源不存在: %s", ((NoResourceFoundException) e).getResourcePath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ResponseDto.fail(ErrorCode.NOT_FOUND, message));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseDto> handleException(Exception e) {
        log.error("[unhandled-error] class={}, message={}", e.getClass().getSimpleName(), e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseDto.fail(ErrorCode.INTERNAL_SERVER_ERROR, "服务器内部错误，请联系管理员"));
    }
    private HttpStatus mapErrorCodeToHttpStatus(ErrorCode errorCode) {
        if (errorCode == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        int code = errorCode.getCode();
        if (code >= 400 && code < 500) {
            return switch (code) {
                case 400 -> HttpStatus.BAD_REQUEST;
                case 401 -> HttpStatus.UNAUTHORIZED;
                case 403 -> HttpStatus.FORBIDDEN;
                case 404 -> HttpStatus.NOT_FOUND;
                case 405 -> HttpStatus.METHOD_NOT_ALLOWED;
                case 409 -> HttpStatus.CONFLICT;
                case 415 -> HttpStatus.UNSUPPORTED_MEDIA_TYPE;
                case 422 -> HttpStatus.UNPROCESSABLE_ENTITY;
                default -> HttpStatus.BAD_REQUEST;
            };
        }
        if (code >= 1001 && code <= 1999) {
            return switch (code) {
                case 1002, 1101, 1201, 1301 -> HttpStatus.NOT_FOUND;
                case 1003, 1102, 1202, 1302 -> HttpStatus.CONFLICT;
                case 1401 -> HttpStatus.FORBIDDEN;
                case 1402, 1403, 1404, 1405 -> HttpStatus.UNAUTHORIZED;
                default -> HttpStatus.BAD_REQUEST;
            };
        }
        if (code >= 501 && code <= 599) {
            return code == 503 ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}