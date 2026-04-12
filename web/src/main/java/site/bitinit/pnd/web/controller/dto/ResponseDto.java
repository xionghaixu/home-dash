package site.bitinit.pnd.web.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import site.bitinit.pnd.web.exception.ErrorCode;
import site.bitinit.pnd.web.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

/**
 * 统一响应数据传输对象。
 * 封装API接口的响应数据，包含状态码、消息、数据、额外信息和时间戳。
 * 提供成功和失败响应的静态工厂方法。
 *
 * @author john
 * @date 2020-01-05
 */
@Setter
@Getter
@AllArgsConstructor
public class ResponseDto {

    private int code;
    private String msg;
    private Object data;
    private Object extra;
    private String timestamp;

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 创建成功响应对象，不包含数据。
     *
     * @return 成功响应对象
     */
    public static ResponseDto success() {
        return success(Collections.EMPTY_LIST);
    }

    /**
     * 创建成功响应对象，包含数据。
     *
     * @param data 响应数据
     * @return 成功响应对象
     */
    public static ResponseDto success(Object data) {
        return success(data, Collections.EMPTY_LIST);
    }

    /**
     * 创建成功响应对象，包含数据和额外信息。
     *
     * @param data  响应数据
     * @param extra 额外信息
     * @return 成功响应对象
     */
    public static ResponseDto success(Object data, Object extra) {
        return new ResponseDto(ErrorCode.SUCCESS.getCode(), "success", data, extra, getTimestamp());
    }

    /**
     * 创建失败响应对象，包含错误消息。
     *
     * @param msg 错误消息
     * @return 失败响应对象
     */
    public static ResponseDto fail(String msg) {
        return fail(ErrorCode.BAD_REQUEST.getCode(), msg, Collections.EMPTY_LIST);
    }

    /**
     * 创建失败响应对象，包含错误码和消息。
     *
     * @param errorCode 错误码枚举
     * @return 失败响应对象
     */
    public static ResponseDto fail(ErrorCode errorCode) {
        return fail(errorCode.getCode(), errorCode.getMessage(), Collections.EMPTY_LIST);
    }

    /**
     * 创建失败响应对象，包含错误码和自定义消息。
     *
     * @param errorCode 错误码枚举
     * @param message   自定义错误消息
     * @return 失败响应对象
     */
    public static ResponseDto fail(ErrorCode errorCode, String message) {
        return fail(errorCode.getCode(), message, Collections.EMPTY_LIST);
    }

    /**
     * 创建失败响应对象，包含数据。
     *
     * @param data 响应数据
     * @return 失败响应对象
     */
    public static ResponseDto fail(Object data) {
        return fail(ErrorCode.BAD_REQUEST.getCode(), "fail", data);
    }

    /**
     * 创建失败响应对象，包含错误消息和数据。
     *
     * @param msg  错误消息
     * @param data 响应数据
     * @return 失败响应对象
     */
    public static ResponseDto fail(String msg, Object data) {
        return fail(ErrorCode.BAD_REQUEST.getCode(), msg, data);
    }

    /**
     * 创建失败响应对象，包含错误码、消息和数据。
     *
     * @param code 错误码
     * @param msg  错误消息
     * @param data 响应数据
     * @return 失败响应对象
     */
    public static ResponseDto fail(int code, String msg, Object data) {
        return fail(code, msg, data, Collections.EMPTY_LIST);
    }

    /**
     * 创建失败响应对象，包含错误码、消息、数据和额外信息。
     *
     * @param code  错误码
     * @param msg   错误消息
     * @param data  响应数据
     * @param extra 额外信息
     * @return 失败响应对象
     */
    public static ResponseDto fail(int code, String msg, Object data, Object extra) {
        return new ResponseDto(code, msg, data, extra, getTimestamp());
    }

    /**
     * 获取当前时间戳。
     *
     * @return 格式化的时间戳字符串
     */
    private static String getTimestamp() {
        return LocalDateTime.now().format(TIMESTAMP_FORMATTER);
    }
}
