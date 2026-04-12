package site.bitinit.pnd.web.config;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.type.TypeAliasRegistry;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;
import site.bitinit.pnd.web.entity.File;
import site.bitinit.pnd.web.entity.Resource;
import site.bitinit.pnd.web.entity.ResourceChunk;

import jakarta.servlet.MultipartConfigElement;
import javax.sql.DataSource;

/**
 * 应用配置类。
 * 提供Multipart文件上传配置、MyBatis配置和数据源配置。
 * 支持MySQL和嵌入式数据库两种数据源。
 *
 * @author john
 * @date 2020-01-08
 */
@Configuration
public class Config {

    private final PndProperties pndProperties;

    /**
     * 构造函数，注入PndProperties依赖。
     *
     * @param pndProperties PND配置属性
     */
    @Autowired
    public Config(PndProperties pndProperties) {
        this.pndProperties = pndProperties;
    }

    /**
     * 配置Multipart文件上传参数。
     * 设置最大文件大小和最大请求大小。
     *
     * @return Multipart配置元素
     */
    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize(DataSize.parse(pndProperties.getMaxFileUploadSize()));
        factory.setMaxRequestSize(DataSize.parse(pndProperties.getMaxRequestSize()));
        return factory.createMultipartConfig();
    }

    /**
     * 自定义MyBatis配置。
     * 开启下划线转驼峰命名，注册实体类别名。
     *
     * @return MyBatis配置定制器
     */
    @Bean
    public ConfigurationCustomizer configurationCustomizer() {
        return configuration -> {
            configuration.setMapUnderscoreToCamelCase(true);

            // 注册别名，用于生产环境识别类别名
            TypeAliasRegistry typeAliasRegistry = configuration.getTypeAliasRegistry();
            typeAliasRegistry.registerAlias(File.ALIAS, File.class);
            typeAliasRegistry.registerAlias(Resource.ALIAS, Resource.class);
            typeAliasRegistry.registerAlias(ResourceChunk.ALIAS, ResourceChunk.class);
        };
    }

    /**
     * 构建数据源。
     * 根据配置选择使用MySQL或嵌入式数据库。
     *
     * @return 数据源对象
     */
    @Bean
    public DataSource buildDataSource() {
        if (pndProperties.isUseMysql()) {
            return buildMysqlDatasource();
        } else {
            return buildEmbedDbDatasource();
        }
    }

    /**
     * 构建MySQL数据源。
     * 使用HikariCP连接池。
     *
     * @return MySQL数据源对象
     */
    private DataSource buildMysqlDatasource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(pndProperties.getMysqlDriver());
        dataSource.setJdbcUrl(pndProperties.getMysqlUrl());
        dataSource.setUsername(pndProperties.getMysqlUsername());
        dataSource.setPassword(pndProperties.getMysqlPassword());
        return dataSource;
    }

    /**
     * 构建嵌入式数据库数据源。
     * 使用HikariCP连接池和H2数据库。
     *
     * @return 嵌入式数据库数据源对象
     */
    private DataSource buildEmbedDbDatasource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(pndProperties.getEmbedDbDriver());
        dataSource.setJdbcUrl(pndProperties.getEmbedDbUrl());
        dataSource.setUsername(pndProperties.getEmbedDbUsername());
        dataSource.setPassword(pndProperties.getEmbedDbPassword());
        return dataSource;
    }
}
