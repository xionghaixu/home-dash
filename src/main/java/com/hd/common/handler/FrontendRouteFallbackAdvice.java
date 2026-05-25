package com.hd.common.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * @author xhx
 * @version 1.0
 * @package com.hd.common.handler
 * @createTime 2026/04/23 23:34
 * @description 前端路由兜底处理。支持在 Spring Boot 3 环境下处理 NoResourceFoundException 以完美运行 HTML5 History 路由模式。
 */
@Slf4j
@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FrontendRouteFallbackAdvice {

    private final GlobalExceptionHandler globalExceptionHandler;

    public FrontendRouteFallbackAdvice(GlobalExceptionHandler globalExceptionHandler) {
        this.globalExceptionHandler = globalExceptionHandler;
    }

    /**
     * 仅将非 API 且非静态文件的 GET 404 转发到首页，其他情况委托给 GlobalExceptionHandler 处理。
     */
    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public Object handleNotFoundException(Exception e) {
        String requestPath = null;
        String httpMethod = null;

        if (e instanceof NoHandlerFoundException noHandlerFound) {
            requestPath = noHandlerFound.getRequestURL();
            httpMethod = noHandlerFound.getHttpMethod();
        } else if (e instanceof NoResourceFoundException noResourceFound) {
            requestPath = noResourceFound.getResourcePath();
            httpMethod = noResourceFound.getHttpMethod().name();
        }

        if (requestPath != null && httpMethod != null) {
            // 规范化路径前缀
            String normalizedPath = requestPath.startsWith("/") ? requestPath : "/" + requestPath;

            // 拦截规则：
            // 1. 必须是 GET 请求
            // 2. 不能是 API 路径（不以 /v1 开头）
            // 3. 不能是具体的静态资源文件路径（即路径不应包含“.”后缀，例如 .js, .css, .png 等）
            if ("GET".equalsIgnoreCase(httpMethod)
                    && !normalizedPath.startsWith("/v1")
                    && !normalizedPath.contains(".")) {
                log.info("[route-fallback] Forwarding request {} to index.html", requestPath);
                return "forward:/index.html";
            }
        }

        // 其他情况（如 API 未找到或静态图片/JS文件未找到），委托给全局异常处理器返回 JSON 格式的 404 响应
        log.debug("[route-fallback] Passing exception to GlobalExceptionHandler: {}", e.getMessage());
        return globalExceptionHandler.handleNotFoundException(e);
    }
}
