package site.bitinit.pnd.web.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import site.bitinit.pnd.web.controller.dto.InstantUploadDto;
import site.bitinit.pnd.web.controller.dto.ResponseDto;
import site.bitinit.pnd.web.entity.File;

import java.nio.file.Path;
import java.util.List;

/**
 * 文件服务接口。
 * 定义文件管理的核心业务操作，包括文件的增删改查、移动、复制、下载、MD5校验等功能。
 *
 * @author john
 * @date 2020-01-11
 */
public interface FileService {

    /**
     * 根据parentId返回文件
     *
     * @param parentId  parentId
     * @param sortBy    排序字段
     * @param sortOrder 排序方式
     * @return list
     */
    ResponseDto findByParentId(Long parentId, String sortBy, String sortOrder);

    /**
     * 获取最近上传文件列表。
     *
     * @param limit 返回数量上限
     * @return 最近上传文件列表
     */
    ResponseDto findRecentFiles(Integer limit);

    /**
     * 获取最近上传摘要统计。
     * 包含今日、本周、本月的上传数量和大小统计，以及最近上传文件列表。
     *
     * @param limit 最近文件列表数量上限
     * @return 最近上传摘要统计
     */
    ResponseDto getRecentUploadSummary(Integer limit);

    /**
     * 根据阶段一分类获取文件列表。
     *
     * @param category  分类名称
     * @param sortBy    排序字段
     * @param sortOrder 排序方式
     * @return 分类文件列表
     */
    ResponseDto findFilesByCategory(String category, String sortBy, String sortOrder);

    /**
     * 获取分类汇总信息。
     *
     * @return 分类数量摘要
     */
    ResponseDto categorySummary();

    /**
     * 根据id返回文件
     *
     * @param fileId fileId
     * @return file response
     */
    ResponseDto findByFileId(Long fileId);

    /**
     * 创建文件
     *
     * @param file file
     */
    void createFile(File file);

    /**
     * 更新文件
     *
     * @param fileName 文件名
     * @param id       文件id
     */
    void renameFile(String fileName, Long id);

    /**
     * 文件移动
     *
     * @param ids      ids
     * @param targetId 目标文件夹
     */
    void moveFiles(List<Long> ids, Long targetId);

    /**
     * 文件复制
     *
     * @param fileIds   fileIds
     * @param targetIds targetIds
     */
    void copyFiles(List<Long> fileIds, List<Long> targetIds);

    /**
     * 删除文件及其子文件
     *
     * @param ids 文件id
     */
    void deleteFiles(List<Long> ids);

    /**
     * 加载资源，下载
     *
     * @param fileId fileId
     * @return resource
     */
    ResourceWrapper loadResource(Long fileId);

    // ==================== MD5校验与秒传功能 ====================

    /**
     * 上传文件（支持秒传）。
     * 实现逻辑：
     * 1. 计算上传文件的MD5值
     * 2. 查询数据库是否已存在相同MD5的资源（秒传检查）
     * 3. 如果存在，直接关联已有资源，实现秒传
     * 4. 如果不存在，保存新文件并计算和存储MD5值
     *
     * <p>性能优势：
     * <ul>
     *   <li>避免重复上传相同文件，节省带宽和存储空间</li>
     *   <li>通过MD5索引快速判断文件是否存在</li>
     *   <li>大文件也能快速完成"上传"</li>
     * </ul>
     *
     * @param file      上传的文件对象
     * @param parentId  父文件夹ID
     * @return 包含文件信息和秒传状态的响应对象
     *         响应数据中包含：
     *         - file: 创建的文件信息
     *         - isInstantUpload: 是否为秒传（true表示秒传，false表示正常上传）
     */
    ResponseDto uploadWithMD5(MultipartFile file, Long parentId);

    /**
     * 根据MD5值查询资源是否存在（用于秒传预检）。
     * 前端可以在上传前先调用此接口，判断是否可以秒传。
     *
     * @param md5 文件的MD5值
     * @return 包含资源信息的响应对象：
     *         - 如果存在：返回资源信息，前端可选择是否执行秒传
     *         - 如果不存在：返回空数据，提示需要正常上传
     */
    ResponseDto checkFileByMD5(String md5);

    /**
     * 验证文件的MD5完整性。
     * 在文件下载后或定期校验时使用，确保文件未被损坏或篡改。
     *
     * @param fileId 文件ID
     * @return 包含验证结果的响应对象：
     *         - valid: true表示MD5匹配，false表示不匹配
     *         - expected: 数据库中存储的MD5值
     *         - actual: 实际计算的MD5值（如果不匹配）
     */
    ResponseDto verifyFileMD5(Long fileId);

    /**
     * 执行秒传操作。
     * 当通过checkFileByMD5确认资源存在后，调用此方法直接创建文件记录并关联已有资源。
     *
     * @param md5      资源的MD5值
     * @param fileName 文件名
     * @param parentId 父文件夹ID
     * @return 包含新建文件信息的响应对象
     */
    ResponseDto instantUpload(String md5, String fileName, Long parentId);

    class ResourceWrapper {
        public Resource resource;
        public File file;

        public ResourceWrapper(Resource resource, File file) {
            this.resource = resource;
            this.file = file;
        }
    }
}
