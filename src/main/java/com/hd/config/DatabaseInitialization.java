package com.hd.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.hd.exception.DatabaseException;
import com.hd.util.StringUtils;

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
 * 在应用启动时自动执行数据库初始化脚本，创建必要的数据表结构。
 * 支持MySQL和嵌入式数据库两种数据库的初始化。
 *
 * @author john
 * @date 2020-01-10
 */
@Slf4j
@Component
public class DatabaseInitialization {

    private static final String SQL_FILE_NAME = "META-INFO/schema.sql";
    private static final String MYSQL_SQL_FILE_NAME = "META-INFO/schema-mysql.sql";

    private final DataSource dataSource;
    private final PndProperties pndProperties;

    /**
     * 构造函数，注入依赖对象。
     *
     * @param dataSource    数据源对象
     * @param pndProperties PND配置属性
     */
    @Autowired
    public DatabaseInitialization(DataSource dataSource, PndProperties pndProperties) {
        this.dataSource = dataSource;
        this.pndProperties = pndProperties;
    }

    /**
     * 初始化数据库。
     * 在应用启动时自动执行，创建必要的数据表结构。
     *
     * @throws RuntimeException 当数据源为null时抛出
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
     * 执行SQL文件中的建表语句。
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
            // 表已经存在
            log.warn(e.getMessage());
        }
    }

    /**
     * 加载SQL文件内容。
     * 根据配置选择加载MySQL或嵌入式数据库的建表脚本。
     *
     * 实现原理：
     * 1. 根据配置判断使用MySQL还是嵌入式数据库
     * 2. 从类路径加载对应的SQL文件
     * 3. 读取文件内容并按分号分割成多条SQL语句
     * 4. 过滤掉注释行（以--或/*开头的行）
     * 5. 返回有效的SQL语句列表
     *
     * SQL文件格式要求：
     * - 每条SQL语句以分号结尾
     * - 支持单行注释（以--开头）
     * - 支持多行注释（以/*开头）
     *
     * @return SQL语句列表
     * @throws Exception 当读取文件失败时抛出
     */
    private List<String> loadSql() throws Exception {
        List<String> sqlList = new ArrayList<>();

        // 根据配置选择SQL文件
        String sqlFileName = SQL_FILE_NAME;
        if (pndProperties.isUseMysql()) {
            sqlFileName = MYSQL_SQL_FILE_NAME;
        }

        // 从类路径加载SQL文件
        ClassLoader classLoader = getClass().getClassLoader();
        URL url = classLoader.getResource(sqlFileName);
        if (url == null) {
            throw new DatabaseException(
                    String.format("SQL文件不存在 [sqlFileName=%s]", sqlFileName));
        }

        log.info("load sql: {}", url.getPath());

        // 使用 try-with-resources 确保输入流正确关闭
        try (InputStream inputStream = url.openStream()) {
            // 读取文件内容
            StringBuilder sqlSb = new StringBuilder();
            byte[] buffer = new byte[1024];
            int byteRead = 0;
            while ((byteRead = inputStream.read(buffer)) != -1) {
                sqlSb.append(new String(buffer, 0, byteRead, StandardCharsets.UTF_8));
            }

            // 按分号分割SQL语句
            String[] sqlArr = sqlSb.toString().split(";");
            for (String s : sqlArr) {
                // 移除单行注释（以--开头）
                String sql = s.replaceAll("--.*", "").trim();
                // 移除多行注释（以/*开头）
                sql = sql.replaceAll("/\\*.*", "").trim();
                // 只保留非空的SQL语句
                if (StringUtils.hasLength(sql)) {
                    sqlList.add(sql);
                }
            }
        }
        return sqlList;
    }
}
