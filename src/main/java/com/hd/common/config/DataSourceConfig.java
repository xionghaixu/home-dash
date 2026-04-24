package com.hd.common.config;

import com.hd.common.config.HomeDashProperties;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.sql.DataSource;

/**
 * @author xhx
 * @version 1.0
 * @package com.hd.common.config
 * @createTime 2026/04/23 23:34
 * @description 应用配置类。根据阶段一配置选择 H2 或 MySQL 数据源。
 */
@Configuration
public class DataSourceConfig {

    @Bean
    public DataSource dataSource(HomeDashProperties homeDashProperties) {
        HikariDataSource dataSource = new HikariDataSource();
        if (homeDashProperties.isUseMysql()) {
            dataSource.setDriverClassName(homeDashProperties.getMysqlDriver());
            dataSource.setJdbcUrl(homeDashProperties.getMysqlUrl());
            dataSource.setUsername(homeDashProperties.getMysqlUsername());
            dataSource.setPassword(homeDashProperties.getMysqlPassword());
            return dataSource;
        }

        dataSource.setDriverClassName(homeDashProperties.getEmbedDbDriver());
        dataSource.setJdbcUrl(homeDashProperties.getEmbedDbUrl());
        dataSource.setUsername(homeDashProperties.getEmbedDbUsername());
        dataSource.setPassword(homeDashProperties.getEmbedDbPassword());
        return dataSource;
    }
}


