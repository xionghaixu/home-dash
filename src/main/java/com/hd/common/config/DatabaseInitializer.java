package com.hd.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.hd.common.config.HomeDashProperties;
import com.hd.common.exception.DatabaseException;
import com.hd.common.util.StringUtils;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 数据库初始化组件。
 *
 * <p>在应用启动时自动执行数据库初始化脚本，创建必要的数据表结构，
 * 支持 MySQL 和嵌入式数据库两种初始化方式。
 * @author john
 * @date 2020-01-10
 */
@Slf4j
@Component
public class DatabaseInitializer {

    private static final String SQL_FILE_NAME = "META-INFO/schema.sql";
    private static final String MYSQL_SQL_FILE_NAME = "META-INFO/schema-mysql.sql";

    private final DataSource dataSource;
    private final HomeDashProperties homeDashProperties;

    /**
     * 构造函数，注入依赖对象。
     *
     * @param dataSource 数据源对象
     * @param homeDashProperties HomeDash 配置属性
     */
    @Autowired
    public DatabaseInitializer(DataSource dataSource, HomeDashProperties homeDashProperties) {
        this.dataSource = dataSource;
        this.homeDashProperties = homeDashProperties;
    }

    /**
     * 初始化数据库。
     *
     * <p>在应用启动时自动执行，创建必要的数据表结构。
     *
     * @throws RuntimeException 当数据源为 null 时抛出
     */
    @PostConstruct
    public void initialization() {

        if (Objects.isNull(dataSource)) {
            throw new DatabaseException(
                    "数据源配置错误，DataSource不能为null [请检查数据库配置]");
        }

        executeSqlFile(dataSource);
    }

    /**
     * 执行 SQL 文件中的建表语句。
     *
     * @param dataSource 数据源对象
     */
    private void executeSqlFile(DataSource dataSource) {
        try (Connection con = dataSource.getConnection();
                Statement statement = con.createStatement()) {

            List<String> sqlList = loadSql();
            for (String sql : sqlList) {
                statement.execute(sql);
            }
        } catch (Exception e) {
            // 表已存在
            log.warn(e.getMessage());
        }
    }

    /**
     * 加载 SQL 文件内容。
     *
     * <p>根据配置选择加载 MySQL 或嵌入式数据库的建表脚本。
     *
     * @return SQL 语句列表
     * @throws Exception 当读取文件失败时抛出
     */
    private List<String> loadSql() throws Exception {
        List<String> sqlList = new ArrayList<>();

        // 选择 SQL 文件
        String sqlFileName = SQL_FILE_NAME;
        if (homeDashProperties.isUseMysql()) {
            sqlFileName = MYSQL_SQL_FILE_NAME;
        }

        // 读取类路径资源
        ClassLoader classLoader = getClass().getClassLoader();
        URL url = classLoader.getResource(sqlFileName);
        if (url == null) {
            throw new DatabaseException(
                    String.format("SQL 文件不存在 [sqlFileName=%s]", sqlFileName));
        }

        log.info("load sql: {}", url.getPath());

        // 读取 SQL 内容
        try (InputStream inputStream = url.openStream()) {
            StringBuilder sqlSb = new StringBuilder();
            byte[] buffer = new byte[1024];
            int byteRead = 0;
            while ((byteRead = inputStream.read(buffer)) != -1) {
                sqlSb.append(new String(buffer, 0, byteRead, StandardCharsets.UTF_8));
            }

            // 按分号切分
            String[] sqlArr = sqlSb.toString().split(";");
            for (String s : sqlArr) {
                // 去掉单行注释
                String sql = s.replaceAll("--.*", "").trim();
                // 去掉多行注释
                sql = sql.replaceAll("/\\*.*", "").trim();
                // 保留有效语句
                if (StringUtils.hasLength(sql)) {
                    sqlList.add(sql);
                }
            }
        }
        return sqlList;
    }
}


