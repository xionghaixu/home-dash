package com.hd;

/**
 * 系统常量定义类。
 * 定义应用程序中使用的各种常量，包括API版本、配置属性键名、状态语义等。
 *
 * @author john
 * @date 2020-01-05
 */
public class Constants {

    public static final String API_VERSION = "/v1";

    // ========== 配置属性键名 ==========
    public static final String PND_HOME = "pnd.homeDir";
    public static final String USE_MYSQL = "pnd.useMysql";
    public static final String MYSQL_URL = "pnd.mysql.url";
    public static final String MYSQL_USERNAME = "pnd.mysql.username";
    public static final String MYSQL_PASSWORD = "pnd.mysql.password";
    public static final String MAX_FILE_UPLOAD_SIZE = "pnd.max.uploadFile.size";
    public static final String MAX_REQUEST_SIZE = "pnd.max.request.size";

    // ========== 文件操作类型 ==========
    /** 移动操作 */
    public static final String OPERATION_TYPE_MOVE = "move";

    /** 复制操作 */
    public static final String OPERATION_TYPE_COPY = "copy";

    // ========== 操作记录类型 ==========
    /**
     * 操作记录类型枚举。
     * 定义系统支持的操作记录类型。
     *
     * <p><b>类型说明：</b>
     * <ul>
     *   <li><b>upload</b> - 文件上传</li>
     *   <li><b>download</b> - 文件下载</li>
     *   <li><b>move</b> - 文件移动</li>
     *   <li><b>copy</b> - 文件复制</li>
     *   <li><b>rename</b> - 文件重命名</li>
     *   <li><b>delete</b> - 文件删除</li>
     *   <li><b>create</b> - 文件/文件夹创建</li>
     * </ul>
     */
    public static final class OperationType {

        /** 文件上传 */
        public static final String UPLOAD = "upload";

        /** 文件下载 */
        public static final String DOWNLOAD = "download";

        /** 文件移动 */
        public static final String MOVE = "move";

        /** 文件复制 */
        public static final String COPY = "copy";

        /** 文件重命名 */
        public static final String RENAME = "rename";

        /** 文件删除 */
        public static final String DELETE = "delete";

        /** 文件/文件夹创建 */
        public static final String CREATE = "create";

        private OperationType() {
        }
    }

    // ========== 文件操作状态语义 ==========
    // 状态定义遵循：简单、明确、可扩展原则
    // 状态值使用小写，便于前端比较

    /**
     * 文件操作状态枚举。
     * 定义阶段一中所有文件相关操作的状态语义。
     *
     * <p><b>状态转换图：</b>
     * <pre>
     *                    ┌─────────┐
     *                    │ pending │  (初始状态)
     *                    └────┬────┘
     *                         │
     *            ┌────────────┼────────────┐
     *            ▼            ▼            ▼
     *      ┌─────────┐  ┌──────────┐  ┌──────────┐
     *      │ success │  │  failed  │  │ skipped  │
     *      └─────────┘  └──────────┘  └──────────┘
     * </pre>
     *
     * <p><b>状态说明：</b>
     * <ul>
     *   <li><b>pending</b> - 操作等待执行，处于队列中</li>
     *   <li><b>success</b> - 操作成功完成</li>
     *   <li><b>failed</b> - 操作失败，附带错误信息</li>
     *   <li><b>skipped</b> - 操作被跳过（如目标不支持）</li>
     * </ul>
     */
    public static final class FileOperationStatus {

        /** 操作等待执行 */
        public static final String PENDING = "pending";

        /** 操作成功 */
        public static final String SUCCESS = "success";

        /** 操作失败 */
        public static final String FAILED = "failed";

        /** 操作跳过 */
        public static final String SKIPPED = "skipped";

        private FileOperationStatus() {
        }
    }

    // ========== 传输任务状态语义 ==========
    /**
     * 传输任务状态枚举。
     * 定义文件上传/下载任务的状态语义。
     *
     * <p><b>状态转换图：</b>
     * <pre>
     *  ┌─────────┐    start     ┌───────────┐
     *  │ waiting │ ──────────► │ uploading │
     *  └─────────┘             └─────┬─────┘
     *                               │
     *                    ┌──────────┼──────────┐
     *                    ▼          ▼          ▼
     *              ┌──────────┐ ┌────────┐ ┌──────────┐
     *              │completed │ │ failed │ │cancelled │
     *              └──────────┘ └────────┘ └──────────┘
     * </pre>
     *
     * <p><b>状态说明：</b>
     * <ul>
     *   <li><b>waiting</b> - 任务等待中，未开始上传</li>
     *   <li><b>uploading</b> - 上传进行中</li>
     *   <li><b>completed</b> - 上传成功完成</li>
     *   <li><b>failed</b> - 上传失败，附带错误信息</li>
     *   <li><b>cancelled</b> - 用户取消上传</li>
     * </ul>
     */
    public static final class TransferStatus {

        /** 任务等待中 */
        public static final String WAITING = "waiting";

        /** 上传进行中 */
        public static final String UPLOADING = "uploading";

        /** 上传成功 */
        public static final String COMPLETED = "completed";

        /** 上传失败 */
        public static final String FAILED = "failed";

        /** 用户取消 */
        public static final String CANCELLED = "cancelled";

        private TransferStatus() {
        }
    }

    // ========== 批量操作错误类型 ==========
    /**
     * 批量操作错误类型枚举。
     * 定义批量操作中可能出现的错误类型，便于前端精确处理。
     *
     * <p><b>错误码范围：</b>
     * <ul>
     *   <li>1101-1199: 文件相关错误</li>
     *   <li>1201-1299: 文件夹相关错误</li>
     *   <li>1301-1399: 资源相关错误</li>
     * </ul>
     */
    public static final class BatchErrorType {

        /** 文件不存在 */
        public static final int FILE_NOT_FOUND = 1101;

        /** 目标目录已存在同名文件 */
        public static final int FILE_ALREADY_EXISTS = 1102;

        /** 不能将文件夹移动到自身或子文件夹 */
        public static final int INVALID_MOVE_TARGET = 1201;

        /** 目标文件夹不存在 */
        public static final int TARGET_FOLDER_NOT_FOUND = 1202;

        /** 资源不存在 */
        public static final int RESOURCE_NOT_FOUND = 1301;

        private BatchErrorType() {
        }
    }

    // ========== 文件名冲突策略 ==========
    /**
     * 文件名冲突策略枚举。
     * 定义当目标位置存在同名文件时的处理策略。
     *
     * <p><b>策略说明：</b>
     * <ul>
     *   <li><b>REJECT</b> - 直接拒绝操作，返回错误（默认策略）</li>
     *   <li><b>RENAME</b> - 自动重命名（如 "file.txt" -> "file (1).txt"）</li>
     *   <li><b>OVERWRITE</b> - 覆盖已有文件（仅适用于相同类型的文件）</li>
     *   <li><b>SKIP</b> - 跳过该文件，继续处理其他文件</li>
     * </ul>
     *
     * <p><b>使用场景：</b>
     * <ul>
     *   <li>REJECT: 适用于用户明确知道不应覆盖的场景</li>
     *   <li>RENAME: 适用于保留两份文件的场景</li>
     *   <li>OVERWRITE: 适用于更新已有文件的场景（需谨慎）</li>
     *   <li>SKIP: 适用于批量操作时希望继续处理其他文件</li>
     * </ul>
     */
    public static final class ConflictStrategy {

        /** 直接拒绝操作，返回错误 */
        public static final String REJECT = "reject";

        /** 自动重命名 */
        public static final String RENAME = "rename";

        /** 覆盖已有文件 */
        public static final String OVERWRITE = "overwrite";

        /** 跳过该文件 */
        public static final String SKIP = "skip";

        private ConflictStrategy() {
        }

        /**
         * 判断是否为有效的冲突策略。
         */
        public static boolean isValid(String strategy) {
            return REJECT.equals(strategy) || RENAME.equals(strategy)
                    || OVERWRITE.equals(strategy) || SKIP.equals(strategy);
        }
    }
}
