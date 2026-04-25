package com.hd.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.File;

/**
 * @author xhx
 * @version 1.0
 * @package com.hd.common.config
 * @createTime 2026/04/23 23:34
 * @description HomeDash 运行属性配置。统一管理运行目录、数据库连接信息和资源目录，避免业务代码直接读取环境变量。
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "home-dash")
public class HomeDashProperties {

    private boolean useMysql;
    private String homeDir = System.getProperty("user.dir") + File.separator + "data";
    private Mysql mysql = new Mysql();

    public String getHomeDashHome() {
        return homeDir;
    }

    public String getHomeDashDataDir() {
        return homeDir;
    }

    public String getDataDir() {
        return homeDir;
    }

    public String getEmbedDbDriver() {
        return "org.h2.Driver";
    }

    public String getEmbedDbUrl() {
        return "jdbc:h2:file:" + getDataDir().replace("\\", "/") + "/"
                + "home_dash;MODE=MySQL;DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1";
    }

    public String getEmbedDbUsername() {
        return "home_dash";
    }

    public String getEmbedDbPassword() {
        return "home_dash";
    }

    public String getMysqlDriver() {
        return "com.mysql.cj.jdbc.Driver";
    }

    public String getMysqlUrl() {
        return mysql.getUrl();
    }

    public String getMysqlUsername() {
        return mysql.getUsername();
    }

    public String getMysqlPassword() {
        return mysql.getPassword();
    }

    public String getBasicResourcePath() {
        return ensureDir(getDataDir() + File.separator + "resources");
    }

    public String getResourceTmpDir() {
        return ensureDir(getBasicResourcePath() + File.separator + "tmp");
    }

    private String ensureDir(String path) {
        File dir = new File(path);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("无法创建目录: " + path);
        }
        return path;
    }

    @Getter
    @Setter
    public static class Mysql {
        private String url = "jdbc:mysql://localhost:3306/home_dash";
        private String username = "root";
        private String password = "";
    }
}
