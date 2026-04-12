package site.bitinit.pnd.web.service;

import org.springframework.core.io.Resource;
import site.bitinit.pnd.web.controller.dto.MergeFileDto;
import site.bitinit.pnd.web.controller.dto.ResponseDto;
import site.bitinit.pnd.web.entity.File;
import site.bitinit.pnd.web.entity.ResourceChunk;

import java.util.List;

/**
 * 资源服务接口。
 * 定义大文件分块上传的核心业务操作，包括分块检查、分块保存、分块合并、断点续传等。
 *
 * @author john
 * @date 2020-01-27
 */
public interface ResourceService {

    /**
     * 检查块是否已经上传
     *
     * @param chunk 块
     * @return true： 已经上传了 false：还未上传
     */
    boolean checkChunk(ResourceChunk chunk);

    /**
     * 保存chunk
     *
     * @param chunk chunk
     */
    void saveChunk(ResourceChunk chunk);

    /**
     * 合并chunk
     *
     * @param mergeFileDto 文件信息
     */
    void mergeChunk(MergeFileDto mergeFileDto);

    /**
     * 获取文件的所有已上传分块信息
     * 用于断点续传功能，客户端可以查询已上传的分块，从中断处继续上传
     *
     * @param identifier 文件唯一标识符
     * @return 已上传的分块列表
     */
    List<ResourceChunk> getUploadedChunks(String identifier);

    /**
     * 验证指定分块的完整性（通过MD5校验）
     * 用于检测分块文件是否损坏或被篡改
     *
     * @param identifier  文件唯一标识符
     * @param chunkNumber 分块编号
     * @param expectedMd5 预期的MD5值（可选，如果为null则只检查文件是否存在且大小正确）
     * @return true表示分块完整，false表示分块损坏或不存在
     */
    boolean verifyChunkIntegrity(String identifier, Integer chunkNumber, String expectedMd5);

    /**
     * 取消文件上传，清理所有临时文件和数据库记录
     * 用于用户主动取消上传或清理超时的上传任务
     *
     * @param identifier 文件唯一标识符
     */
    void cancelUpload(String identifier);

    /**
     * 清理超时的未完成上传任务
     * 定期调用此方法清理超过指定时间未完成的分块上传
     *
     * @param timeoutMinutes 超时时间（分钟），超过此时间的未完成任务将被清理
     * @return 清理的任务数量
     */
    int cleanupTimeoutUploads(int timeoutMinutes);

    /**
     * 获取传输任务列表和摘要。
     *
     * @return 传输任务响应
     */
    ResponseDto transferTasks();

    /**
     * 清理指定状态的传输任务记录。
     * 阶段一默认用于清空已完成记录，也支持清理失败/取消记录。
     *
     * @param status 状态，支持completed/failed/cancelled/all/finished，多个状态可逗号分隔
     * @return 清理数量
     */
    int clearTransferTasks(String status);
}
