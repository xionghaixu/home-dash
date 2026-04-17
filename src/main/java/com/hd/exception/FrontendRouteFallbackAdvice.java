package com.hd.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * 前端路由 Fallback 异常处理器。
 * 用于 Vue Router History 模式下，处理刷新前端路由时产生的 404 异常。
 */
@Slf4j
@ControllerAdvice
public class FrontendRouteFallbackAdvice {

    /**
     * 处理 404 异常。
     * 对于 GET 请求且路径不以 /v1 开头（前端路由），转发到 index.html。
     *
     * @param e 404异常
     * @return 视图名称（forward:/index.html）或 null（让其他处理器继续处理）
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public String handleNoHandlerFoundException(NoHandlerFoundException e) {
        String requestPath = e.getRequestURL().toString();
        String httpMethod = e.getHttpMethod();

        // 如果是 GET 请求且不是 API 路径（不以 /v1 开头），认为是前端路由，转发到 index.html
        if ("GET".equalsIgnoreCase(httpMethod) && !requestPath.startsWith("/v1")) {
            log.info("[route-fallback] Forwarding {} to index.html", requestPath);
            return "forward:/index.html";
        }

        // 其他情况返回 null，让 GlobalExceptionHandler 的 @RestControllerAdvice 处理
        return null;
    }
}
