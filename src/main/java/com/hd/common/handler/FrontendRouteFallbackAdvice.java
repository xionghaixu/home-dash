package com.hd.common.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * 前端路由兜底处理。
 */
@Slf4j
@ControllerAdvice
public class FrontendRouteFallbackAdvice {

    /**
     * 仅将非 API 的 GET 404 转发到首页。
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public String handleNoHandlerFoundException(NoHandlerFoundException e) {
        String requestPath = e.getRequestURL().toString();
        String httpMethod = e.getHttpMethod();

        // 非 API 路径直接转发首页
        if ("GET".equalsIgnoreCase(httpMethod) && !requestPath.startsWith("/v1")) {
            log.info("[route-fallback] Forwarding {} to index.html", requestPath);
            return "forward:/index.html";
        }

        // 其他情况交给全局异常处理
        return null;
    }
}
