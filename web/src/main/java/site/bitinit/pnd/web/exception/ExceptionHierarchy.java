package site.bitinit.pnd.web.exception;

/**
 * 异常层次结构说明。
 * 本类仅用于文档说明，不包含实际代码。
 *
 * <h2>异常层次结构</h2>
 *
 * <h3>1. 基础异常类</h3>
 * <ul>
 * <li><b>PndException</b> - 所有PND异常的基类，继承自 RuntimeException</li>
 * </ul>
 *
 * <h3>2. 业务异常（BusinessException）</h3>
 * <p>
 * 业务异常表示由于用户操作不当或业务规则限制导致的错误，可以通过修正操作来避免。
 * </p>
 * <ul>
 * <li><b>BusinessException</b> - 业务异常基类，继承自 PndException</li>
 * <li><b>DataNotFoundException</b> - 数据未找到异常</li>
 * <li><b>DataFormatException</b> - 数据格式异常</li>
 * <li><b>FileNotFoundException</b> - 文件未找到异常</li>
 * <li><b>FileAlreadyExistsException</b> - 文件已存在异常</li>
 * <li><b>FileOperationException</b> - 文件操作失败异常</li>
 * <li><b>UploadException</b> - 文件上传失败异常</li>
 * <li><b>DownloadException</b> - 文件下载失败异常</li>
 * <li><b>InvalidFileNameException</b> - 无效文件名异常</li>
 * <li><b>InvalidFilePathException</b> - 无效文件路径异常</li>
 * </ul>
 *
 * <h3>3. 系统异常（SystemException）</h3>
 * <p>
 * 系统异常表示系统级别的错误，通常不是由用户操作引起的，而是由系统环境或配置问题导致的。
 * </p>
 * <ul>
 * <li><b>SystemException</b> - 系统异常基类，继承自 PndException</li>
 * <li><b>DatabaseException</b> - 数据库操作异常</li>
 * <li><b>StorageException</b> - 存储操作异常（IO相关）</li>
 * <li><b>NetworkException</b> - 网络操作异常</li>
 * </ul>
 *
 * <h2>异常使用场景</h2>
 *
 * <h3>业务异常使用场景</h3>
 * <ul>
 * <li><b>DataNotFoundException</b> - 请求的数据在数据库中不存在</li>
 * <li><b>DataFormatException</b> - 请求数据格式不正确或验证失败</li>
 * <li><b>FileNotFoundException</b> - 文件ID不存在、文件路径不存在、文件已被删除</li>
 * <li><b>FileAlreadyExistsException</b> - 创建文件时文件已存在、重命名文件时目标文件名已存在</li>
 * <li><b>FileOperationException</b> - 文件移动失败、文件复制失败、文件删除失败、文件重命名失败</li>
 * <li><b>UploadException</b> - 文件上传过程中断、文件分块上传失败、上传文件大小超限</li>
 * <li><b>DownloadException</b> - 文件下载过程中断、下载文件不存在、下载文件权限不足</li>
 * <li><b>InvalidFileNameException</b> - 文件名包含非法字符、文件名过长、文件名为空</li>
 * <li><b>InvalidFilePathException</b> - 文件路径包含非法字符、文件路径不存在、文件路径格式错误</li>
 * </ul>
 *
 * <h3>系统异常使用场景</h3>
 * <ul>
 * <li><b>DatabaseException</b> - 数据库连接失败、SQL执行错误、数据库事务失败</li>
 * <li><b>StorageException</b> - 文件读写失败、磁盘空间不足、文件系统错误</li>
 * <li><b>NetworkException</b> - 网络连接失败、网络超时、网络传输中断</li>
 * </ul>
 *
 * <h2>异常处理原则</h2>
 *
 * <h3>1. 异常分类原则</h3>
 * <ul>
 * <li>业务异常：用户可以通过修正操作来避免的错误</li>
 * <li>系统异常：需要系统管理员介入或系统自动恢复的错误</li>
 * </ul>
 *
 * <h3>2. 异常信息原则</h3>
 * <ul>
 * <li>异常信息应包含详细的错误原因和上下文信息</li>
 * <li>异常信息应便于问题定位和排查</li>
 * <li>异常信息应对用户友好，避免暴露系统内部细节</li>
 * </ul>
 *
 * <h3>3. 异常日志原则</h3>
 * <ul>
 * <li>业务异常：记录警告日志（WARN级别）</li>
 * <li>系统异常：记录错误日志（ERROR级别），包含完整的堆栈信息</li>
 * </ul>
 *
 * <h3>4. HTTP状态码映射原则</h3>
 * <ul>
 * <li>400 Bad Request：数据格式错误、无效文件名、无效文件路径</li>
 * <li>401 Unauthorized：未授权访问</li>
 * <li>403 Forbidden：禁止访问、权限不足</li>
 * <li>404 Not Found：数据不存在、文件不存在</li>
 * <li>409 Conflict：文件已存在、资源冲突</li>
 * <li>500 Internal Server Error：文件操作失败、上传失败、下载失败</li>
 * <li>503 Service Unavailable：网络错误</li>
 * </ul>
 *
 * <h2>错误码定义</h2>
 * <p>
 * 错误码定义在 {@link ErrorCode} 枚举中，分为以下几类：
 * </p>
 * <ul>
 * <li>2xx：成功状态码</li>
 * <li>4xx：客户端错误</li>
 * <li>5xx：业务错误（1001-1999）</li>
 * <li>6xx：系统错误（500-599）</li>
 * </ul>
 *
 * @author john
 * @date 2020-01-11
 */
public class ExceptionHierarchy {
    // 本类仅用于文档说明，不包含实际代码
}
