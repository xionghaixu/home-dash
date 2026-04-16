package com.hd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * PND Web应用启动类。
 * Spring Boot应用的入口点，负责启动整个应用程序。
 * 包含开发环境下的CORS跨域配置。
 *
 * @author john
 * @date 2020-01-05
 */
@SpringBootApplication
@EnableScheduling
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
}
