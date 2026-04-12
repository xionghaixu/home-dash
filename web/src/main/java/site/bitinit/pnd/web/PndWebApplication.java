package site.bitinit.pnd.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * PND Web应用启动类。
 * Spring Boot应用的入口点，负责启动整个应用程序。
 * 包含开发环境下的CORS跨域配置。
 *
 * @author john
 * @date 2020-01-05
 */
@SpringBootApplication
public class PndWebApplication {

    /**
     * 应用程序入口方法。
     * 启动Spring Boot应用程序。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(PndWebApplication.class, args);
    }

    /**
     * 开发环境Web MVC配置类。
     * 配置CORS跨域支持，允许所有请求方法。
     */
    @Profile("dev")
    @Configuration
    static class DefaultWebMvcConfigurer implements WebMvcConfigurer {
        /**
         * 配置CORS跨域映射。
         * 允许所有路径使用所有HTTP方法。
         *
         * @param registry CORS注册器
         */
        @Override
        public void addCorsMappings(CorsRegistry registry) {
            registry.addMapping("/**").allowedMethods("*");
        }
    }
}
