package com.hd.common;

/**
 * 通用系统常量。
 *
 * <p>仅保留阶段一及基础运行所需的配置键、API 前缀和状态语义。
 */
public class HomeDashConstants {

    public static final String API_VERSION = "/v1";

    public static final String HOME_DASH_HOME = "HomeDash.homeDir";
    public static final String HOME_DASH_USE_MYSQL = "HomeDash.useMysql";
    public static final String HOME_DASH_MYSQL_URL = "HomeDash.mysql.url";
    public static final String HOME_DASH_MYSQL_USERNAME = "HomeDash.mysql.username";
    public static final String HOME_DASH_MYSQL_PASSWORD = "HomeDash.mysql.password";

    public static final String MAX_FILE_UPLOAD_SIZE = "spring.servlet.multipart.max-file-size";
    public static final String MAX_REQUEST_SIZE = "spring.servlet.multipart.max-request-size";

    /** 传输任务状态。 */
    public static final class TransferStatus {

        public static final String WAITING = "waiting";
        public static final String UPLOADING = "uploading";
        public static final String COMPLETED = "completed";
        public static final String FAILED = "failed";
        public static final String CANCELLED = "cancelled";

        private TransferStatus() {
        }
    }

    protected HomeDashConstants() {
    }
}



