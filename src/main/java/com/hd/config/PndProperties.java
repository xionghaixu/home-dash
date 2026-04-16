package com.hd.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import com.hd.Constants;
import com.hd.util.StringUtils;
import com.hd.util.Utils;

import jakarta.annotation.PostConstruct;
import java.io.File;

/**
 * PND应用属性配置类。
 * 管理应用程序的配置属性，包括数据库配置（MySQL/嵌入式数据库）、
 * 文件存储路径、文件上传大小限制等。
 * 支持通过环境变量或配置文件进行属性注入。
 *
 * @author john
 * @date 2020-01-10
 */
@Getter
@Setter
@org.springframework.context.annotation.Configuration
public class PndProperties {

    // ==================== 数据库配置 ====================

    /**
     * 是否使用MySQL数据库。
     * true表示使用MySQL，false表示使用嵌入式H2数据库。
     */
    private boolean useMysql;

    /** MySQL数据库连接URL。 */
    private String mysqlUrl;

    /** MySQL数据库用户名。 */
    private String mysqlUsername;

    /** MySQL数据库密码。 */
    private String mysqlPassword;

    /** MySQL JDBC驱动类名，默认为MySQL 8.x驱动。 */
    private String mysqlDriver = "com.mysql.cj.jdbc.Driver";

    // ==================== 嵌入式数据库配置 ====================

    /** 嵌入式数据库驱动类名，默认使用H2数据库（JDK 21兼容）。 */
    private String embedDbDriver = "org.h2.Driver";

    /** 嵌入式数据库连接URL，在初始化时自动生成。使用H2数据库。 */
    private String embedDbUrl;

    /** 嵌入式数据库用户名，默认为"home_dash"。 */
    private String embedDbUsername = "home_dash";

    /** 嵌入式数据库密码，默认为"home_dash"。 */
    private String embedDbPassword = "home_dash";

    // ==================== 文件存储路径配置 ====================

    /**
     * PND应用主目录。
     * 默认为用户主目录下的pnd文件夹（~/pnd）。
     * 所有数据文件都存储在此目录下。
     */
    private String pndHome;

    /**
     * PND数据存储目录。
     * 存储数据库文件、资源文件等数据，默认为{pndHome}。
     */
    private String pndDataDir;

    // ==================== 文件上传配置 ====================

    /**
     * 单个文件最大上传大小。
     * 支持的单位：KB、MB、GB等，默认为10MB。
     */
    private String maxFileUploadSize;

    /**
     * 最大请求大小。
     * 包括所有文件和表单数据的总大小，默认为12MB。
     */
    private String maxRequestSize;

    /** Spring环境对象，用于读取配置属性。 */
    private final Environment env;

    /**
     * 构造函数，注入Environment依赖。
     *
     * @param env Spring环境对象
     */
    @Autowired
    public PndProperties(Environment env) {
        this.env = env;
    }

    /**
     * 初始化配置属性。
     * 从环境变量或系统属性中加载配置，设置默认值。
     */
    @PostConstruct
    public void init() {
        setPndHome(getEnvProperty(Constants.PND_HOME,
                System.getProperty("user.dir") + File.separator + "data"));
        setPndDataDir(getPndHome());
        setEmbedDbUrl("jdbc:h2:file:" + getPndDataDir() + File.separator + "home_dash;MODE=MySQL;DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1");

        setUseMysql(Boolean.valueOf(getEnvProperty(Constants.USE_MYSQL, "false")));
        setMysqlUrl(getEnvProperty(Constants.MYSQL_URL, StringUtils.EMPTY));
        setMysqlUsername(getEnvProperty(Constants.MYSQL_USERNAME, StringUtils.EMPTY));
        setMysqlPassword(getEnvProperty(Constants.MYSQL_PASSWORD, StringUtils.EMPTY));

        setMaxFileUploadSize(getEnvProperty(Constants.MAX_FILE_UPLOAD_SIZE, "10MB"));
        setMaxRequestSize(getEnvProperty(Constants.MAX_REQUEST_SIZE, "12MB"));
    }

    /**
     * 获取资源文件存储的基础路径。
     * 如果目录不存在则自动创建。
     *
     * @return 资源文件存储路径
     */
    public String getBasicResourcePath() {
        String basicPath = getPndDataDir() + File.separator + "resources";
        Utils.createFolders(basicPath);
        return basicPath;
    }

    /**
     * 获取资源临时文件目录路径。
     * 用于存储上传过程中的临时分块文件。
     * 如果目录不存在则自动创建。
     *
     * @return 资源临时文件目录路径
     */
    public String getResourceTmpDir() {
        String resourceTmpPath = getBasicResourcePath() + File.separator + "tmp";
        Utils.createFolders(resourceTmpPath);
        return resourceTmpPath;
    }

    /**
     * 从环境变量或配置文件中获取属性值。
     * 如果属性值为空则返回默认值。
     *
     * @param key        属性键名
     * @param defaultVal 默认值
     * @return 属性值或默认值
     */
    private String getEnvProperty(String key, String defaultVal) {
        String val = env.getProperty(key, "");
        if (StringUtils.isBlank(val)) {
            return defaultVal;
        }
        return val;
    }

    /**
     * 从系统属性中获取属性值。
     * 如果属性值为空则返回默认值。
     *
     * @param key        系统属性键名
     * @param defaultVal 默认值
     * @return 系统属性值或默认值
     */
    private static String getSystemProperty(String key, String defaultVal) {
        String val = System.getProperty(key, "");
        if (StringUtils.isBlank(val)) {
            return defaultVal;
        }
        return val;
    }
}
