package com.hd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.mybatis.spring.annotation.MapperScan;

/**
 * @author xhx
 * @version 1.0
 * @package com.hd
 * @createTime 2026/04/23 23:34
 * @description HomeDash Web 应用入口。作为 Spring Boot 启动类，负责完成应用初始化与组件扫描。
 */
@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan(basePackages = "com.hd.common.config")
@MapperScan("com.hd.dao.mapper")
public class HomeDashApplication {

    /**
     * 应用程序入口方法。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(HomeDashApplication.class, args);
    }
}

