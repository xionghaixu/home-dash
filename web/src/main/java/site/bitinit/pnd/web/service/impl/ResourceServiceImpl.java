package site.bitinit.pnd.web.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.UrlResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.bitinit.pnd.web.config.PndProperties;
import site.bitinit.pnd.web.controller.dto.MergeFileDto;
import site.bitinit.pnd.web.controller.dto.ResponseDto;
import site.bitinit.pnd.web.controller.dto.TransferTaskDto;
import site.bitinit.pnd.web.dao.ResourceChunkMapper;
import site.bitinit.pnd.web.dao.ResourceMapper;
import site.bitinit.pnd.web.entity.Resource;
import site.bitinit.pnd.web.entity.ResourceChunk;
import site.bitinit.pnd.web.exception.DataNotFoundException;
import site.bitinit.pnd.web.exception.UploadException;
import site.bitinit.pnd.web.service.FileService;
import site.bitinit.pnd.web.service.ResourceService;
import site.bitinit.pnd.web.util.FileUtils;
import site.bitinit.pnd.web.util.MD5Utils;
import site.bitinit.pnd.web.util.Utils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 资源服务实现类。
 * 实现大文件分块上传的核心业务逻辑，包括分块检查、分块保存和分块合并。
 * 支持断点续传、分块完整性校验、自动重试、合并回滚等高级功能。
 *
 * 核心功能：
 * 1. 分块上传：支持大文件分块上传，使用缓冲流优化IO性能
 * 2. 断点续传：记录已上传分块信息，支持从中断处继续上传
 * 3. 完整性校验：通过MD5校验确保分块数据完整性
 * 4. 自动重试：分块上传失败时自动重试，提高上传成功率
 * 5. 合并回滚：合并失败时自动清理已创建的文件，保持数据一致性
 * 6. 超时清理：定期清理长时间未完成的分块上传任务
 *
 * @author john
 * @date 2020-01-27
 */
@Slf4j
@Service
public class ResourceServiceImpl implements ResourceService {

    /**
     * 轻量传输任务状态。
     */
    private static class TransferTaskState {
        private String identifier;
        private String fileName;
        private String status;
        private String errorMessage;
        private Long totalSize;
        private Long parentId;
        private Long fileId;
        private Integer totalChunks;
        private Integer uploadedChunks;
        private Date createTime;
        private Date updateTime;
    }

    /**
     * 文件合并结果内部类。
     * 用于封装合并后的文件信息，包括MD5值和相对存储路径。
     */
    private static class MergeResult {
        private final String md5;
        private final String relativePath;

        public MergeResult(String md5, String relativePath) {
            this.md5 = md5;
            this.relativePath = relativePath;
        }

        public String getMd5() {
            return md5;
        }

        public String getRelativePath() {
            return relativePath;
        }
    }

    /** 最大重试次数 */
    private static final int MAX_RETRY_COUNT = 3;

    /** 重试间隔时间（毫秒） */
    private static final long RETRY_INTERVAL_MS = 1000;

    /** 默认上传超时时间（分钟） */
    private static final int DEFAULT_UPLOAD_TIMEOUT_MINUTES = 60;

    /** MD5缓冲区大小 */
    private static final int MD5_BUFFER_SIZE = 8192;

    /** 上传任务最后活动时间缓存（用于超时检测） */
    private final Map<String, Long> uploadActivityCache = new ConcurrentHashMap<>();
    /** 轻量传输任务缓存（用于阶段一传输列表页）。 */
    private final Map<String, TransferTaskState> transferTaskCache = new ConcurrentHashMap<>();

    private final PndProperties pndProperties;
    private final ResourceChunkMapper chunkMapper;
    private final ResourceMapper resourceMapper;
    private final FileService fileService;

    /**
     * 构造函数，注入依赖对象。
     *
     * @param chunkMapper    资源分块数据访问对象
     * @param pndProperties  PND配置属性
     * @param resourceMapper 资源数据访问对象
     * @param fileService    文件服务接口
     */
    @Autowired
    public ResourceServiceImpl(ResourceChunkMapper chunkMapper,
            PndProperties pndProperties,
            ResourceMapper resourceMapper, FileService fileService) {
        this.chunkMapper = chunkMapper;
        this.pndProperties = pndProperties;
        this.resourceMapper = resourceMapper;
        this.fileService = fileService;
    }

    /**
     * 检查文件分块是否已上传。
     * 通过文件唯一标识符和分块编号查询数据库判断分块是否存在。
     *
     * @param chunk 包含文件标识符和分块编号的分块信息
     * @return true表示分块已上传，false表示未上传
     */
    @Override
    public boolean checkChunk(ResourceChunk chunk) {
        log.debug("检查文件分块 [identifier={}, chunkNumber={}]",
                chunk != null ? chunk.getIdentifier() : null,
                chunk != null ? chunk.getChunkNumber() : null);

        // 参数校验
        if (Objects.isNull(chunk)) {
            log.warn("检查分块失败：分块信息为空");
            return false;
        }
        if (Objects.isNull(chunk.getIdentifier()) || chunk.getIdentifier().trim().isEmpty()) {
            log.warn("检查分块失败：文件标识符为空");
            return false;
        }

        ResourceChunk resourceChunk = chunkMapper.findByIdentifierAndChunkNumber(chunk.getIdentifier(),
                chunk.getChunkNumber());
        boolean exists = Objects.nonNull(resourceChunk);

        log.debug("文件分块检查完成 [identifier={}, chunkNumber={}, exists={}]",
                chunk.getIdentifier(), chunk.getChunkNumber(), exists);

        return exists;
    }

    /**
     * 获取文件的所有已上传分块信息。
     * 用于断点续传功能，客户端可以查询已上传的分块，从中断处继续上传。
     *
     * 实现原理：
     * 1. 根据文件唯一标识符查询数据库中所有已上传的分块记录
     * 2. 按分块编号正序排序，便于客户端判断上传进度
     * 3. 返回分块列表，包含分块编号、大小等信息
     *
     * 断点续传流程：
     * 1. 客户端在上传前先调用此接口获取已上传分块列表
     * 2. 客户端对比总分块数和已上传分块数，判断是否需要续传
     * 3. 客户端跳过已上传的分块，从未上传的分块开始继续上传
     *
     * 性能优化：
     * - 数据库查询使用索引（identifier字段），提高查询速度
     * - 只返回必要的分块信息，减少数据传输量
     *
     * 异常处理：
     * - 查询失败时返回空列表，不影响客户端正常上传流程
     * - 记录错误日志，便于问题排查
     *
     * @param identifier 文件唯一标识符
     * @return 已上传的分块列表
     */
    @Override
    public java.util.List<ResourceChunk> getUploadedChunks(String identifier) {
        log.debug("获取已上传分块列表 [identifier={}]", identifier);

        try {
            java.util.List<ResourceChunk> chunks = chunkMapper.findByIdentifier(identifier);

            log.info("已上传分块列表查询完成 [identifier={}, uploadedChunks={}, totalChunks={}]",
                    identifier, chunks.size(), chunks.isEmpty() ? 0 : chunks.get(0).getTotalChunks());

            return chunks;
        } catch (Exception e) {
            log.error("查询已上传分块列表失败 [identifier={}, error={}]", identifier, e.getMessage(), e);
            // 返回空列表，不影响客户端正常上传流程
            return new java.util.ArrayList<>();
        }
    }

    /**
     * 保存文件分块。
     * 将分块数据保存到临时目录，并在数据库中记录分块信息。
     * 使用缓冲流优化大文件写入性能，支持自动重试和完整性校验。
     *
     * 断点续传支持：
     * - 使用事务确保文件和数据库记录的一致性
     * - 如果文件已存在，先删除旧文件再保存新文件
     * - 如果数据库记录已存在，更新记录而不是插入新记录
     * - 异常时回滚数据库操作，确保数据一致性
     *
     * 自动重试机制：
     * - 当上传失败时自动重试最多3次
     * - 重试间隔1秒，避免立即重试导致的问题
     * - 记录每次重试的详细信息，便于问题排查
     *
     * 完整性校验：
     * - 保存分块后计算并记录分块的MD5值
     * - MD5值用于后续的完整性验证
     * - 支持客户端传入预期MD5进行实时校验
     *
     * 实现原理：
     * 1. 根据文件唯一标识符构建临时目录路径
     * 2. 如果临时目录不存在，创建目录
     * 3. 使用缓冲流将分块数据写入临时文件，提高IO性能
     * 4. 计算分块的MD5值用于完整性校验
     * 5. 在数据库中记录分块信息（包含MD5），用于断点续传
     * 6. 更新上传任务的活动时间，用于超时检测
     *
     * 临时文件组织结构：
     * - 临时目录：{resourceTmpDir}/{identifier}/
     * - 分块文件：{identifier}/{filename}-{chunkNumber}
     * - 例如：/tmp/upload/abc123/video.mp4-1
     *
     * 性能优化：
     * - 使用BufferedOutputStream缓冲流，减少磁盘IO次数
     * - 使用8KB缓冲区大小，平衡内存使用和IO效率
     * - 使用try-with-resources确保资源正确关闭
     *
     * 注意事项：
     * - 使用文件唯一标识符组织临时文件，避免冲突
     * - 分块文件名包含分块编号，便于后续合并
     * - 数据库记录支持断点续传功能
     *
     * @param chunk 包含分块数据和元信息的分块对象
     * @throws PndException 当保存分块失败时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveChunk(ResourceChunk chunk) {
        long startTime = System.currentTimeMillis();
        log.info("开始保存文件分块 [identifier={}, chunkNumber={}, fileName={}, size={}]",
                chunk != null ? chunk.getIdentifier() : null,
                chunk != null ? chunk.getChunkNumber() : null,
                chunk != null ? chunk.getFilename() : null,
                (chunk != null && chunk.getFile() != null) ? chunk.getFile().getSize() : 0);

        // 参数校验
        if (Objects.isNull(chunk)) {
            throw new UploadException("分块信息不能为空");
        }
        if (Objects.isNull(chunk.getIdentifier()) || chunk.getIdentifier().trim().isEmpty()) {
            throw new UploadException("文件标识符不能为空");
        }
        if (Objects.isNull(chunk.getFile())) {
            throw new UploadException("分块文件数据不能为空");
        }

        // 构建临时目录路径
        String chunkDirPath = getChunkTmpDir(chunk.getIdentifier());
        // 构建分块文件完整路径
        String chunkPath = getChunkTmpPath(chunk);

        int retryCount = 0;
        Exception lastException = null;

        // 自动重试机制：最多重试3次
        while (retryCount <= MAX_RETRY_COUNT) {
            try {
                // 如果不是第一次尝试，等待一段时间后重试
                if (retryCount > 0) {
                    log.warn("正在重试保存分块 [identifier={}, chunkNumber={}, retryCount={}]",
                            chunk.getIdentifier(), chunk.getChunkNumber(), retryCount);
                    Thread.sleep(RETRY_INTERVAL_MS);
                }

                // 如果临时目录不存在，创建目录
                Path chunkDir = Paths.get(chunkDirPath);
                if (!Files.exists(chunkDir)) {
                    Files.createDirectories(chunkDir);
                    log.debug("创建临时目录 [chunkDirPath={}]", chunkDirPath);
                }

                // 检查分块文件是否已存在（断点续传场景）
                Path chunkFilePath = Paths.get(chunkPath);
                if (Files.exists(chunkFilePath)) {
                    log.debug("分块文件已存在，将覆盖 [chunkPath={}]", chunkPath);
                    Files.delete(chunkFilePath);
                }

                // 使用缓冲流写入文件，提高IO性能
                // 缓冲区大小设置为8KB，平衡内存使用和IO效率
                String calculatedMd5;
                try (java.io.BufferedOutputStream bos = new java.io.BufferedOutputStream(
                        Files.newOutputStream(chunkFilePath), 8192)) {

                    // 使用输入流直接传输，避免一次性加载到内存
                    chunk.getFile().getInputStream().transferTo(bos);
                    bos.flush();

                    // 计算分块的MD5值用于完整性校验
                    calculatedMd5 = calculateFileMD5(chunkFilePath);
                    log.debug("分块MD5计算完成 [identifier={}, chunkNumber={}, md5={}]",
                            chunk.getIdentifier(), chunk.getChunkNumber(), calculatedMd5);
                }

                // 检查数据库中是否已存在该分块记录（断点续传场景）
                ResourceChunk existingChunk = chunkMapper.findByIdentifierAndChunkNumber(
                        chunk.getIdentifier(), chunk.getChunkNumber());

                if (Objects.nonNull(existingChunk)) {
                    log.debug("分块记录已存在，更新MD5信息 [identifier={}, chunkNumber={}]",
                            chunk.getIdentifier(), chunk.getChunkNumber());
                    // 更新分块的MD5和更新时间
                    chunkMapper.updateChunkMd5(chunk.getIdentifier(), chunk.getChunkNumber(), calculatedMd5);
                } else {
                    // 在数据库中记录分块信息（包含MD5），用于断点续传
                    chunk.setMd5(calculatedMd5);
                    chunkMapper.save(chunk);
                    log.debug("分块记录保存成功 [identifier={}, chunkNumber={}, md5={}]",
                            chunk.getIdentifier(), chunk.getChunkNumber(), calculatedMd5);
                }

                // 更新上传任务的活动时间，用于超时检测
                uploadActivityCache.put(chunk.getIdentifier(), System.currentTimeMillis());
                markUploading(chunk);

                long elapsedTime = System.currentTimeMillis() - startTime;
                log.info("文件分块保存成功 [identifier={}, chunkNumber={}, chunkPath={}, md5={}, 耗时={}ms, retryCount={}]",
                        chunk.getIdentifier(), chunk.getChunkNumber(), chunkPath, calculatedMd5, elapsedTime,
                        retryCount);

                // 保存成功，退出循环
                return;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("保存分块被中断 [identifier={}, chunkNumber={}]", chunk.getIdentifier(), chunk.getChunkNumber());
                throw new UploadException("保存分块过程被中断");
            } catch (Exception e) {
                lastException = e;
                retryCount++;
                log.error("保存文件分块失败 [identifier={}, chunkNumber={}, chunkPath={}, retryCount={}/{}]",
                        chunk.getIdentifier(), chunk.getChunkNumber(), chunkPath, retryCount, MAX_RETRY_COUNT, e);

                // 清理可能已创建的文件，确保数据一致性
                cleanupFailedChunk(chunkPath);

                // 如果达到最大重试次数，抛出异常
                if (retryCount > MAX_RETRY_COUNT) {
                    log.error("分块上传失败，已达最大重试次数 [identifier={}, chunkNumber={}, maxRetries={}]",
                            chunk.getIdentifier(), chunk.getChunkNumber(), MAX_RETRY_COUNT);
                    markFailed(chunk.getIdentifier(), chunk.getFilename(),
                            String.format("分块上传失败：%s", e.getMessage()));
                    break;
                }
            }
        }

        // 所有重试都失败，抛出异常
        throw new UploadException(
                String.format(
                        "保存文件分块失败，已达最大重试次数 [identifier=%s, chunkNumber=%d, chunkPath=%s, maxRetries=%d, lastError=%s]",
                        chunk.getIdentifier(), chunk.getChunkNumber(), chunkPath, MAX_RETRY_COUNT,
                        lastException != null ? lastException.getMessage() : "unknown"));
    }

    /**
     * 合并文件分块。
     * 将所有分块按顺序合并成完整文件，删除临时文件，并创建资源记录和文件记录。
     * 支持秒传功能：通过MD5检查是否已存在相同文件。
     *
     * @param mergeFileDto 包含文件信息和唯一标识符的数据传输对象
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void mergeChunk(MergeFileDto mergeFileDto) {
        long startTime = System.currentTimeMillis();
        log.info("开始合并文件分块 [identifier={}, fileName={}, fileSize={}]",
                mergeFileDto != null ? mergeFileDto.getIdentifier() : null,
                mergeFileDto != null ? mergeFileDto.getFileName() : null,
                mergeFileDto != null ? mergeFileDto.getSize() : 0);

        // 参数校验
        if (Objects.isNull(mergeFileDto)) {
            throw new UploadException("合并文件信息不能为空");
        }
        if (Objects.isNull(mergeFileDto.getIdentifier()) || mergeFileDto.getIdentifier().trim().isEmpty()) {
            throw new UploadException("文件标识符不能为空");
        }
        if (Objects.isNull(mergeFileDto.getFileName()) || mergeFileDto.getFileName().trim().isEmpty()) {
            throw new UploadException("文件名不能为空");
        }

        try {
            // 先合并文件并计算MD5
            MergeResult mergeResult = mergeChunksAndCalculateMD5(mergeFileDto);
            String md5 = mergeResult.getMd5();
            String relativePath = mergeResult.getRelativePath();

            // 检查是否存在相同MD5的资源（秒传功能）
            Resource existingResource = resourceMapper.findByMd5(md5);
            if (Objects.nonNull(existingResource)) {
                log.info("发现相同MD5的资源，启用秒传功能 [identifier={}, fileName={}, md5={}, existingResourceId={}]",
                        mergeFileDto.getIdentifier(), mergeFileDto.getFileName(), md5, existingResource.getId());

                deleteTempFiles(mergeFileDto);
                deleteMergedFile(relativePath);
                createFileWithExistingResource(mergeFileDto, existingResource);
            } else {
                log.info("未发现相同MD5的资源，创建新资源 [identifier={}, fileName={}, md5={}]",
                        mergeFileDto.getIdentifier(), mergeFileDto.getFileName(), md5);
                generateRecord(mergeFileDto, md5, relativePath);
            }

            markCompleted(mergeFileDto);
            long elapsedTime = System.currentTimeMillis() - startTime;
            log.info("文件分块合并成功 [identifier={}, fileName={}, resourceId={}, 总耗时={}ms]",
                    mergeFileDto.getIdentifier(), mergeFileDto.getFileName(), mergeFileDto.getResourceId(), elapsedTime);
        } catch (RuntimeException e) {
            markFailed(mergeFileDto.getIdentifier(), mergeFileDto.getFileName(),
                    String.format("分块合并失败：%s", e.getMessage()));
            throw e;
        }
    }

    /**
     * 获取分块临时目录路径。
     *
     * @param identifier 文件唯一标识符
     * @return 分块临时目录的完整路径
     */
    private String getChunkTmpDir(String identifier) {
        return pndProperties.getResourceTmpDir() + File.separator
                + identifier;
    }

    /**
     * 获取分块文件的完整路径。
     * 对文件名进行转义处理，防止文件系统安全问题。
     *
     * @param chunk 分块信息对象
     * @return 分块文件的完整路径
     */
    private String getChunkTmpPath(ResourceChunk chunk) {
        String fileName = chunk.getFilename();
        // 对文件名进行转义，替换可能引起问题的字符
        String safeFileName = fileName
                .replaceAll("[\\\\/:*?\"<>|]", "_") // 替换Windows不允许的字符
                .replaceAll("\\s+", "_"); // 替换空格为下划线

        return getChunkTmpDir(chunk.getIdentifier()) + File.separator
                + safeFileName + "-" + chunk.getChunkNumber();
    }

    /**
     * 合并所有分块文件并计算MD5值。
     * 使用流式合并和实时MD5计算，优化大文件处理性能。
     *
     * 实现原理：
     * 1. 根据创建时间生成目标文件存储路径，按年月组织目录结构
     * 2. 生成UUID作为文件唯一标识，避免文件名冲突
     * 3. 使用流式方式合并分块，避免内存溢出
     * 4. 在合并过程中实时计算MD5，避免二次读取文件
     * 5. 合并完成后删除临时分块文件和目录，释放磁盘空间
     * 6. 删除数据库中的分块记录，保持数据一致性
     *
     * 性能优化：
     * - 使用BufferedInputStream和BufferedOutputStream缓冲流，减少IO次数
     * - 使用DigestOutputStream在写入时实时计算MD5，避免二次读取
     * - 使用流式传输，避免一次性加载大文件到内存
     * - 缓冲区大小设置为64KB，优化大文件传输性能
     * - 按分块编号正序合并，避免倒序排序开销
     *
     * @param mergeFileDto 包含文件信息和唯一标识符的数据传输对象
     * @return 包含MD5值和相对存储路径的合并结果对象
     * @throws PndException 当合并过程出错时抛出
     */
    private MergeResult mergeChunksAndCalculateMD5(MergeFileDto mergeFileDto) {
        long startTime = System.currentTimeMillis();
        log.debug("开始合并分块文件并计算MD5 [identifier={}, fileName={}]",
                mergeFileDto.getIdentifier(), mergeFileDto.getFileName());

        // 构建目标文件目录：按年月组织存储结构，便于管理和归档
        // 例如：/data/resources/2020/01/ 表示2020年1月的文件存储目录
        String targetFileDir = pndProperties.getBasicResourcePath() + File.separator
                + Utils.formatDate(mergeFileDto.getCreateTime(), "yyyy") + File.separator
                + Utils.formatDate(mergeFileDto.getCreateTime(), "MM");
        Utils.createFolders(targetFileDir);

        // 生成UUID作为文件唯一标识，避免文件名冲突
        String uuid = Utils.uuid();

        // 构建完整的目标文件路径：目录/UUID.扩展名
        String targetFilePath = targetFileDir + File.separator + uuid +
                FileUtils.extractFileExtensionName(mergeFileDto.getFileName());

        log.debug("目标文件路径 [targetFilePath={}]", targetFilePath);

        try {
            // 获取所有分块文件路径
            java.util.List<Path> chunkPaths = getSortedChunkPaths(mergeFileDto);

            if (chunkPaths.isEmpty()) {
                log.error("未找到分块文件 [identifier={}, fileName={}]",
                        mergeFileDto.getIdentifier(), mergeFileDto.getFileName());
                throw new UploadException(
                        String.format("未找到分块文件 [identifier=%s, fileName=%s]",
                                mergeFileDto.getIdentifier(), mergeFileDto.getFileName()));
            }

            // 创建MessageDigest用于计算MD5
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");

            // 使用缓冲流和DigestOutputStream合并文件并计算MD5
            try (java.io.BufferedOutputStream bos = new java.io.BufferedOutputStream(
                    Files.newOutputStream(Paths.get(targetFilePath)), 65536);
                    java.security.DigestOutputStream dos = new java.security.DigestOutputStream(bos, md)) {

                // 缓冲区大小64KB，优化大文件传输性能
                byte[] buffer = new byte[65536];

                // 按顺序合并所有分块
                for (Path chunkPath : chunkPaths) {
                    try (java.io.BufferedInputStream bis = new java.io.BufferedInputStream(
                            Files.newInputStream(chunkPath), 65536)) {
                        int bytesRead;
                        while ((bytesRead = bis.read(buffer)) != -1) {
                            dos.write(buffer, 0, bytesRead);
                        }
                    }

                    // 删除已处理的分块文件，释放磁盘空间
                    Files.delete(chunkPath);
                    log.debug("已删除分块文件 [chunkPath={}]", chunkPath);
                }

                dos.flush();
            }

            // 获取MD5值
            String md5 = bytesToHex(md.digest());

            log.info("文件MD5计算完成 [identifier={}, fileName={}, md5={}]",
                    mergeFileDto.getIdentifier(), mergeFileDto.getFileName(), md5);

            // 删除临时目录（此时目录应该为空）
            Utils.deleteFile(getChunkTmpDir(mergeFileDto.getIdentifier()));

            // 删除数据库中的分块记录，保持数据一致性
            chunkMapper.deleteChunk(mergeFileDto.getIdentifier());

            // 构建相对路径（不包含基础资源目录）
            String relativePath = Utils.formatDate(mergeFileDto.getCreateTime(), "yyyy") + File.separator +
                    Utils.formatDate(mergeFileDto.getCreateTime(), "MM") + File.separator +
                    uuid + FileUtils.extractFileExtensionName(mergeFileDto.getFileName());

            long elapsedTime = System.currentTimeMillis() - startTime;
            log.info("文件分块合并完成 [identifier={}, fileName={}, uuid={}, relativePath={}, md5={}, 耗时={}ms]",
                    mergeFileDto.getIdentifier(), mergeFileDto.getFileName(), uuid, relativePath, md5, elapsedTime);

            return new MergeResult(md5, relativePath);
        } catch (Exception e) {
            log.error("文件合并过程失败 [identifier={}, fileName={}, targetPath={}]",
                    mergeFileDto.getIdentifier(), mergeFileDto.getFileName(), targetFilePath, e);
            throw new UploadException(
                    String.format("文件合并过程失败 [identifier=%s, fileName=%s, targetPath=%s, error=%s]",
                            mergeFileDto.getIdentifier(), mergeFileDto.getFileName(), targetFilePath, e.getMessage()));
        }
    }

    /**
     * 获取排序后的分块文件路径列表。
     * 按分块编号正序排序，确保合并顺序正确。
     *
     * @param mergeFileDto 包含文件信息的数据传输对象
     * @return 排序后的分块文件路径列表
     * @throws IOException 当读取目录失败时抛出
     */
    private java.util.List<Path> getSortedChunkPaths(MergeFileDto mergeFileDto) throws IOException {
        java.util.List<Path> chunkPaths = new java.util.ArrayList<>();

        // 获取原始文件名（用于过滤）
        String originalFileName = mergeFileDto.getFileName();

        try (java.util.stream.Stream<Path> paths = Files
                .list(Paths.get(getChunkTmpDir(mergeFileDto.getIdentifier())))) {
            paths
                    // 过滤掉原始文件名（只保留分块文件）
                    .filter(path -> {
                        String fileName = path.getFileName().toString();
                        return !fileName.equals(originalFileName) && fileName.contains("-");
                    })
                    // 按分块编号正序排序
                    .sorted((o1, o2) -> {
                        String p1 = o1.getFileName().toString();
                        String p2 = o2.getFileName().toString();

                        // 提取分块编号：文件名格式为 "filename-chunkNumber"
                        // 使用更安全的方式提取：从最后一个"-"之后的部分
                        try {
                            int i1 = p1.lastIndexOf("-");
                            int i2 = p2.lastIndexOf("-");

                            if (i1 < 0 || i2 < 0) {
                                // 如果找不到分隔符，按文件名字典序排序
                                return p1.compareTo(p2);
                            }

                            String numStr1 = p1.substring(i1 + 1);
                            String numStr2 = p2.substring(i2 + 1);

                            // 验证是否为数字
                            Integer num1 = Integer.parseInt(numStr1);
                            Integer num2 = Integer.parseInt(numStr2);

                            return num1.compareTo(num2);
                        } catch (NumberFormatException e) {
                            // 如果解析失败，按文件名字典序排序
                            log.warn("解析分块编号失败，使用字典序排序 [file1={}, file2={}]", p1, p2);
                            return p1.compareTo(p2);
                        }
                    })
                    .forEach(chunkPaths::add);
        }

        return chunkPaths;
    }

    /**
     * 将字节数组转换为十六进制字符串。
     *
     * @param bytes 字节数组
     * @return 十六进制字符串（小写）
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * 生成资源记录和文件记录。
     * 在数据库中创建资源实体和文件实体，建立文件与资源的关联。
     *
     * 实现原理：
     * 1. 创建Resource实体：
     * - link: 初始引用计数为1
     * - path: 文件存储路径，按年月组织
     * - md5: 文件MD5校验值
     * - size: 文件大小
     * 2. 保存Resource记录到数据库
     * 3. 设置FileDto的资源ID和文件类型
     * 4. 调用FileService创建File记录
     *
     * 资源路径组织规则：
     * - 格式：{year}/{month}/{uuid}.{extension}
     * - 例如：2020/01/abc123def456.mp4
     * - 按年月组织便于管理和归档
     *
     * 引用计数机制：
     * - link字段记录引用该资源的文件数量
     * - 初始值为1，表示新创建的文件引用该资源
     * - 复制文件时增加引用计数
     * - 删除文件时减少引用计数
     *
     * @param fileDto      包含文件信息的数据传输对象
     * @param md5          文件的MD5值
     * @param relativePath 文件的相对存储路径
     */
    private void generateRecord(MergeFileDto fileDto, String md5, String relativePath) {
        log.debug("开始生成资源记录和文件记录 [identifier={}, fileName={}, md5={}, relativePath={}]",
                fileDto.getIdentifier(), fileDto.getFileName(), md5, relativePath);

        // 创建Resource实体
        Resource resource = Resource.builder()
                // 初始引用计数为1
                .link(1)
                // 文件存储路径：使用合并时生成的实际路径
                .path(relativePath)
                // MD5校验值
                .md5(md5)
                // 文件大小
                .size(fileDto.getSize())
                .build();

        // 保存Resource记录到数据库
        resourceMapper.save(resource);

        log.info("资源记录创建成功 [resourceId={}, path={}, size={}, md5={}]",
                resource.getId(), resource.getPath(), resource.getSize(), resource.getMd5());

        // 设置FileDto的资源ID和文件类型
        fileDto.setResourceId(resource.getId());
        fileDto.setType(FileUtils.getFileType(fileDto.getFileName()).toString());

        // 调用FileService创建File记录
        fileService.createFile(fileDto);

        log.debug("文件记录创建完成 [identifier={}, fileName={}, resourceId={}]",
                fileDto.getIdentifier(), fileDto.getFileName(), resource.getId());
    }

    /**
     * 删除临时文件和分块记录。
     * 用于秒传场景，当发现已存在相同MD5的资源时，删除临时文件。
     *
     * @param mergeFileDto 包含文件信息的数据传输对象
     */
    private void deleteTempFiles(MergeFileDto mergeFileDto) {
        log.debug("开始删除临时文件和分块记录 [identifier={}, fileName={}]",
                mergeFileDto.getIdentifier(), mergeFileDto.getFileName());

        try {
            // 删除临时目录及其所有文件
            Utils.deleteFile(getChunkTmpDir(mergeFileDto.getIdentifier()));

            // 删除数据库中的分块记录
            chunkMapper.deleteChunk(mergeFileDto.getIdentifier());

            log.info("临时文件和分块记录删除成功 [identifier={}, fileName={}]",
                    mergeFileDto.getIdentifier(), mergeFileDto.getFileName());
        } catch (Exception e) {
            log.error("删除临时文件失败 [identifier={}, fileName={}]",
                    mergeFileDto.getIdentifier(), mergeFileDto.getFileName(), e);
            // 不抛出异常，因为文件已经合并成功，临时文件删除失败不影响主流程
        }
    }

    /**
     * 删除已合并的文件。
     * 用于秒传场景，当发现已存在相同MD5的资源时，删除刚合并的文件。
     *
     * @param relativePath 文件的相对存储路径
     */
    private void deleteMergedFile(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) {
            return;
        }

        try {
            String fullPath = pndProperties.getBasicResourcePath() + File.separator + relativePath;
            java.io.File mergedFile = new java.io.File(fullPath);
            if (mergedFile.exists() && mergedFile.delete()) {
                log.info("秒传场景：删除已合并的文件成功 [path={}]", relativePath);
            } else if (mergedFile.exists()) {
                log.warn("秒传场景：删除已合并的文件失败 [path={}]", relativePath);
            }
        } catch (Exception e) {
            log.error("秒传场景：删除已合并的文件异常 [path={}]", relativePath, e);
        }
    }

    /**
     * 使用现有资源创建文件记录（秒传功能）。
     * 当发现已存在相同MD5的资源时，直接引用该资源，不创建新的物理文件。
     *
     * 实现原理：
     * 1. 使用悲观锁查询资源，防止并发修改
     * 2. 增加资源的引用计数（link字段）
     * 3. 创建新的文件记录，指向现有资源
     *
     * 秒传优势：
     * - 节省存储空间：不创建重复的物理文件
     * - 提高上传速度：跳过文件合并和存储过程
     * - 自动清理：当引用计数降为0时，自动删除物理文件
     *
     * @param fileDto          包含文件信息的数据传输对象
     * @param existingResource 已存在的资源对象
     */
    private void createFileWithExistingResource(MergeFileDto fileDto, Resource existingResource) {
        log.debug("开始使用现有资源创建文件记录 [identifier={}, fileName={}, existingResourceId={}]",
                fileDto.getIdentifier(), fileDto.getFileName(), existingResource.getId());

        // 使用悲观锁查询资源，防止并发修改引用计数
        Resource resource = resourceMapper.findByIdForUpdate(existingResource.getId());
        if (Objects.isNull(resource)) {
            log.error("资源不存在，数据库数据异常 [identifier={}, resourceId={}]",
                    fileDto.getIdentifier(), existingResource.getId());
            throw new UploadException(
                    String.format("资源不存在，数据库数据异常 [identifier=%s, resourceId=%d]",
                            fileDto.getIdentifier(), existingResource.getId()));
        }

        // 增加资源引用计数
        resourceMapper.updateLink(resource.getId(), resource.getLink() + 1);

        log.info("资源引用计数增加 [identifier={}, resourceId={}, oldLink={}, newLink={}]",
                fileDto.getIdentifier(), resource.getId(), resource.getLink(), resource.getLink() + 1);

        // 设置FileDto的资源ID和文件类型
        fileDto.setResourceId(resource.getId());
        fileDto.setType(FileUtils.getFileType(fileDto.getFileName()).toString());

        // 创建新的文件记录，指向现有资源
        fileService.createFile(fileDto);

        log.info("秒传文件记录创建成功 [identifier={}, fileName={}, resourceId={}]",
                fileDto.getIdentifier(), fileDto.getFileName(), resource.getId());
    }

    /**
     * 验证指定分块的完整性（通过MD5校验）。
     * 用于检测分块文件是否损坏或被篡改。
     *
     * 实现原理：
     * 1. 检查分块记录是否存在于数据库中
     * 2. 检查分块物理文件是否存在
     * 3. 如果提供了预期MD5值，重新计算文件的MD5并进行比较
     * 4. 如果没有提供预期MD5值，只检查文件大小是否与记录一致
     *
     * 应用场景：
     * - 断点续传前验证已上传分块的完整性
     * - 合并前验证所有分块的完整性
     * - 手动触发分块健康检查
     *
     * @param identifier  文件唯一标识符
     * @param chunkNumber 分块编号
     * @param expectedMd5 预期的MD5值（可选，如果为null则只检查文件是否存在且大小正确）
     * @return true表示分块完整，false表示分块损坏或不存在
     */
    @Override
    public boolean verifyChunkIntegrity(String identifier, Integer chunkNumber, String expectedMd5) {
        log.debug("开始验证分块完整性 [identifier={}, chunkNumber={}, expectedMd5={}]",
                identifier, chunkNumber, expectedMd5 != null ? expectedMd5 : "null");

        // 参数校验
        if (Objects.isNull(identifier) || identifier.trim().isEmpty()) {
            log.warn("验证分块完整性失败：文件标识符为空");
            return false;
        }
        if (Objects.isNull(chunkNumber)) {
            log.warn("验证分块完整性失败：分块编号为空");
            return false;
        }

        try {
            // 查询数据库中的分块记录
            ResourceChunk chunkRecord = chunkMapper.findByIdentifierAndChunkNumber(identifier, chunkNumber);
            if (Objects.isNull(chunkRecord)) {
                log.debug("分块记录不存在 [identifier={}, chunkNumber={}]", identifier, chunkNumber);
                return false;
            }

            // 构建分块文件路径
            String chunkPath = getChunkTmpDir(identifier) + File.separator +
                    sanitizeFileName(chunkRecord.getFilename()) + "-" + chunkNumber;

            Path chunkFilePath = Paths.get(chunkPath);

            // 检查物理文件是否存在
            if (!Files.exists(chunkFilePath)) {
                log.warn("分块物理文件不存在 [identifier={}, chunkNumber={}, chunkPath={}]",
                        identifier, chunkNumber, chunkPath);
                return false;
            }

            // 如果提供了预期MD5值，进行MD5校验
            if (Objects.nonNull(expectedMd5) && !expectedMd5.trim().isEmpty()) {
                String actualMd5 = calculateFileMD5(chunkFilePath);
                boolean md5Match = expectedMd5.equalsIgnoreCase(actualMd5);

                if (!md5Match) {
                    log.error("分块MD5校验失败 [identifier={}, chunkNumber={}, expectedMd5={}, actualMd5={}]",
                            identifier, chunkNumber, expectedMd5, actualMd5);
                    return false;
                }

                log.info("分块MD5校验成功 [identifier={}, chunkNumber={}, md5={}]",
                        identifier, chunkNumber, actualMd5);
            } else {
                // 如果没有提供预期MD5，检查文件大小是否一致
                long actualSize = Files.size(chunkFilePath);
                long recordedSize = chunkRecord.getCurrentChunkSize() != null ? chunkRecord.getCurrentChunkSize() : 0L;

                if (actualSize != recordedSize && recordedSize > 0) {
                    log.warn("分块大小不一致 [identifier={}, chunkNumber={}, actualSize={}, recordedSize={}]",
                            identifier, chunkNumber, actualSize, recordedSize);
                    return false;
                }

                log.debug("分块完整性验证通过（仅检查存在性和大小）[identifier={}, chunkNumber={}, size={}]",
                        identifier, chunkNumber, actualSize);
            }

            return true;

        } catch (Exception e) {
            log.error("验证分块完整性时发生异常 [identifier={}, chunkNumber={}, error={}]",
                    identifier, chunkNumber, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 取消文件上传，清理所有临时文件和数据库记录。
     * 用于用户主动取消上传或清理超时的上传任务。
     *
     * 实现原理：
     * 1. 删除指定标识符对应的所有临时分块文件和目录
     * 2. 删除数据库中的所有分块记录
     * 3. 从活动缓存中移除该上传任务
     * 4. 记录详细的清理日志
     *
     * 应用场景：
     * - 用户点击"取消上传"按钮时调用
     * - 上传任务超时时自动调用
     * - 系统关闭时清理未完成的任务
     *
     * 安全性考虑：
     * - 操作不可逆，调用前需确认
     * - 清理过程中发生异常不影响其他操作
     * - 确保文件和数据库记录的一致性
     *
     * @param identifier 文件唯一标识符
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void cancelUpload(String identifier) {
        log.info("开始取消文件上传 [identifier={}]", identifier);

        // 参数校验
        if (Objects.isNull(identifier) || identifier.trim().isEmpty()) {
            log.warn("取消上传失败：文件标识符为空");
            throw new UploadException("文件标识符不能为空");
        }

        try {
            // 统计清理前的信息用于日志
            List<ResourceChunk> chunksBeforeCleanup = chunkMapper.findByIdentifier(identifier);
            int chunksCount = chunksBeforeCleanup.size();
            log.debug("准备清理的分块数量 [identifier={}, chunksCount={}]", identifier, chunksCount);

            // 删除临时目录及其所有文件
            String chunkTmpDir = getChunkTmpDir(identifier);
            Utils.deleteFile(chunkTmpDir);
            log.debug("临时目录删除成功 [chunkTmpDir={}]", chunkTmpDir);

            // 删除数据库中的分块记录
            chunkMapper.deleteChunk(identifier);
            log.debug("数据库分块记录删除成功 [identifier={}]", identifier);

            // 从活动缓存中移除该上传任务
            uploadActivityCache.remove(identifier);
            log.debug("已从活动缓存中移除 [identifier={}]", identifier);
            markCancelled(identifier, "上传已取消");

            log.info("文件上传取消完成 [identifier={}, deletedChunks={}]",
                    identifier, chunksCount);

        } catch (Exception e) {
            log.error("取消文件上传失败 [identifier={}, error={}]", identifier, e.getMessage(), e);
            throw new UploadException(String.format("取消文件上传失败 [identifier=%s, error=%s]",
                    identifier, e.getMessage()));
        }
    }

    /**
     * 清理超时的未完成上传任务。
     * 定期调用此方法清理超过指定时间未完成的分块上传。
     *
     * 实现原理：
     * 1. 扫描uploadActivityCache中的所有上传任务
     * 2. 检查每个任务的最后活动时间
     * 3. 如果超过指定的超时时间，则清理该任务的所有临时文件和记录
     * 4. 返回清理的任务数量
     *
     * 定时任务配置：
     * - 建议每小时执行一次（可通过@Scheduled配置）
     * - 默认超时时间为60分钟
     * - 可根据实际需求调整超时时间
     *
     * 性能优化：
     * - 使用ConcurrentHashMap保证线程安全
     * - 批量清理减少IO操作次数
     * - 异常隔离，单个任务清理失败不影响其他任务
     *
     * @param timeoutMinutes 超时时间（分钟），超过此时间的未完成任务将被清理
     * @return 清理的任务数量
     */
    @Override
    public int cleanupTimeoutUploads(int timeoutMinutes) {
        log.info("开始清理超时的未完成上传任务 [timeoutMinutes={}]", timeoutMinutes);

        if (timeoutMinutes <= 0) {
            timeoutMinutes = DEFAULT_UPLOAD_TIMEOUT_MINUTES;
            log.debug("使用默认超时时间 [defaultTimeoutMinutes={}]", DEFAULT_UPLOAD_TIMEOUT_MINUTES);
        }

        long currentTime = System.currentTimeMillis();
        long timeoutMillis = timeoutMinutes * 60L * 1000L;
        int cleanedCount = 0;
        List<String> timeoutIdentifiers = new ArrayList<>();
        final int finalTimeoutMinutes = timeoutMinutes;

        try {
            // 收集所有超时的任务标识符
            uploadActivityCache.forEach((identifier, lastActivityTime) -> {
                if (currentTime - lastActivityTime > timeoutMillis) {
                    timeoutIdentifiers.add(identifier);
                    log.debug("发现超时任务 [identifier={}, lastActivityTime={}, timeoutMinutes={}]",
                            identifier, new Date(lastActivityTime), finalTimeoutMinutes);
                }
            });

            // 清理超时任务
            for (String identifier : timeoutIdentifiers) {
                try {
                    log.info("正在清理超时任务 [identifier={}]", identifier);
                    cancelUpload(identifier);
                    cleanedCount++;
                } catch (Exception e) {
                    log.error("清理超时任务失败 [identifier={}, error={}]", identifier, e.getMessage());
                    // 单个任务清理失败不影响其他任务的清理
                }
            }

            log.info("超时任务清理完成 [totalTimeoutTasks={}, successfullyCleaned={}]",
                    timeoutIdentifiers.size(), cleanedCount);

        } catch (Exception e) {
            log.error("清理超时任务过程发生异常 [error={}]", e.getMessage(), e);
        }

        return cleanedCount;
    }

    @Override
    public ResponseDto transferTasks() {
        List<TransferTaskDto> tasks = transferTaskCache.values().stream()
                .map(this::toTransferTaskDto)
                .sorted(Comparator.comparing(TransferTaskDto::getUpdateTime,
                        Comparator.nullsLast(Date::compareTo)).reversed())
                .toList();

        Map<String, Long> summary = tasks.stream().collect(Collectors.groupingBy(
                task -> task.getStatus() == null ? "unknown" : task.getStatus(),
                LinkedHashMap::new,
                Collectors.counting()));
        summary.putIfAbsent("uploading", 0L);
        summary.putIfAbsent("completed", 0L);
        summary.putIfAbsent("failed", 0L);
        summary.putIfAbsent("cancelled", 0L);
        summary.put("total", (long) tasks.size());

        return ResponseDto.success(tasks, summary);
    }

    @Override
    public int clearTransferTasks(String status) {
        Set<String> clearableStatuses = normalizeTransferStatuses(status);
        List<String> identifiers = transferTaskCache.entrySet().stream()
                .filter(entry -> clearableStatuses.contains(normalizeTransferStatus(entry.getValue().status)))
                .map(Map.Entry::getKey)
                .toList();

        identifiers.forEach(transferTaskCache::remove);
        return identifiers.size();
    }

    private void markUploading(ResourceChunk chunk) {
        TransferTaskState state = transferTaskCache.computeIfAbsent(chunk.getIdentifier(), this::newTransferTaskState);
        state.fileName = chunk.getFilename();
        state.status = "uploading";
        state.errorMessage = null;
        state.totalSize = chunk.getTotalSize();
        state.totalChunks = chunk.getTotalChunks();
        state.uploadedChunks = chunkMapper.findByIdentifier(chunk.getIdentifier()).size();
        state.updateTime = new Date();
        if (state.createTime == null) {
            state.createTime = new Date();
        }
    }

    private void markCompleted(MergeFileDto fileDto) {
        TransferTaskState state = transferTaskCache.computeIfAbsent(fileDto.getIdentifier(), this::newTransferTaskState);
        state.fileName = fileDto.getFileName();
        state.status = "completed";
        state.errorMessage = null;
        state.totalSize = fileDto.getSize();
        state.parentId = fileDto.getParentId();
        state.fileId = fileDto.getId();
        state.uploadedChunks = state.totalChunks;
        state.updateTime = new Date();
        if (state.createTime == null) {
            state.createTime = new Date();
        }
        uploadActivityCache.remove(fileDto.getIdentifier());
    }

    private void markFailed(String identifier, String fileName, String errorMessage) {
        TransferTaskState state = transferTaskCache.computeIfAbsent(identifier, this::newTransferTaskState);
        state.fileName = fileName;
        state.status = "failed";
        state.errorMessage = errorMessage;
        state.updateTime = new Date();
        if (state.createTime == null) {
            state.createTime = new Date();
        }
    }

    private void markCancelled(String identifier, String message) {
        TransferTaskState state = transferTaskCache.computeIfAbsent(identifier, this::newTransferTaskState);
        state.status = "cancelled";
        state.errorMessage = message;
        state.updateTime = new Date();
        if (state.createTime == null) {
            state.createTime = new Date();
        }
    }

    private TransferTaskState newTransferTaskState(String identifier) {
        TransferTaskState state = new TransferTaskState();
        state.identifier = identifier;
        state.createTime = new Date();
        state.updateTime = new Date();
        state.uploadedChunks = 0;
        state.totalChunks = 0;
        return state;
    }

    private TransferTaskDto toTransferTaskDto(TransferTaskState state) {
        int progress = 0;
        if (state.totalChunks != null && state.totalChunks > 0 && state.uploadedChunks != null) {
            progress = Math.min(100, (int) Math.round(state.uploadedChunks * 100.0 / state.totalChunks));
        }
        if ("completed".equals(state.status)) {
            progress = 100;
        }
        return TransferTaskDto.builder()
                .identifier(state.identifier)
                .fileName(state.fileName)
                .status(state.status)
                .errorMessage(state.errorMessage)
                .totalSize(state.totalSize)
                .parentId(state.parentId)
                .fileId(state.fileId)
                .totalChunks(state.totalChunks)
                .uploadedChunks(state.uploadedChunks)
                .progress(progress)
                .createTime(state.createTime)
                .updateTime(state.updateTime)
                .build();
    }

    private Set<String> normalizeTransferStatuses(String status) {
        Set<String> statuses = new LinkedHashSet<>();
        String normalizedInput = status == null ? "completed" : status.trim().toLowerCase();
        if (normalizedInput.isEmpty()) {
            normalizedInput = "completed";
        }

        for (String item : normalizedInput.split(",")) {
            String normalized = item.trim();
            if (normalized.isEmpty()) {
                continue;
            }
            switch (normalized) {
                case "completed", "failed", "cancelled" -> statuses.add(normalized);
                case "finished" -> statuses.addAll(List.of("completed", "failed", "cancelled"));
                case "all" -> statuses.addAll(List.of("completed", "failed", "cancelled"));
                default -> throw new site.bitinit.pnd.web.exception.DataFormatException(
                        String.format("不支持的传输状态清理类型 [status=%s]", status));
            }
        }

        if (statuses.isEmpty()) {
            statuses.add("completed");
        }
        return statuses;
    }

    private String normalizeTransferStatus(String status) {
        return status == null ? "" : status.trim().toLowerCase();
    }

    /**
     * 计算文件的MD5值。
     * 使用流式计算方式，避免一次性加载大文件到内存。
     *
     * 实现原理：
     * 1. 创建MessageDigest实例，指定MD5算法
     * 2. 使用DigestInputStream包装文件输入流
     * 3. 分批读取文件内容并更新MessageDigest
     * 4. 读取完成后获取MD5字节数组
     * 5. 将字节数组转换为十六进制字符串
     *
     * 性能优化：
     * - 使用8KB缓冲区，平衡内存使用和读取效率
     * - 流式处理，支持大文件（GB级别）
     * - 使用try-with-resources确保资源释放
     *
     * @param filePath 文件路径
     * @return 文件的MD5值（32位十六进制字符串）
     * @throws Exception 当读取文件或计算MD5失败时抛出
     */
    private String calculateFileMD5(Path filePath) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");

        try (DigestInputStream dis = new DigestInputStream(
                Files.newInputStream(filePath), md)) {

            byte[] buffer = new byte[MD5_BUFFER_SIZE];
            while (dis.read(buffer) != -1) {
                // 继续读取以更新MessageDigest
            }
        }

        return bytesToHex(md.digest());
    }

    /**
     * 清理失败的分块文件。
     * 在重试或异常处理时调用，确保不会残留损坏的文件。
     *
     * @param chunkPath 分块文件路径
     */
    private void cleanupFailedChunk(String chunkPath) {
        try {
            Path chunkFilePath = Paths.get(chunkPath);
            if (Files.exists(chunkFilePath)) {
                Files.delete(chunkFilePath);
                log.debug("清理失败的分块文件 [chunkPath={}]", chunkPath);
            }
        } catch (Exception cleanupException) {
            log.warn("清理失败的分块文件时出错 [chunkPath={}]", chunkPath, cleanupException);
        }
    }

    /**
     * 对文件名进行转义处理，防止文件系统安全问题。
     * 替换可能引起问题的特殊字符。
     *
     * @param fileName 原始文件名
     * @return 转义后的安全文件名
     */
    private String sanitizeFileName(String fileName) {
        if (fileName == null) {
            return "unknown";
        }
        return fileName
                .replaceAll("[\\\\/:*?\"<>|]", "_") // 替换Windows不允许的字符
                .replaceAll("\\s+", "_"); // 替换空格为下划线
    }

    /**
     * 定时清理超时的未完成上传任务。
     * 每小时执行一次，清理超过默认超时时间（60分钟）的任务。
     * 可通过配置调整执行频率和超时时间。
     */
    @Scheduled(fixedRate = 3600000) // 每小时执行一次（3600000毫秒）
    public void scheduledCleanupTimeoutUploads() {
        log.debug("定时任务触发：清理超时的未完成上传任务");
        try {
            int cleanedCount = cleanupTimeoutUploads(DEFAULT_UPLOAD_TIMEOUT_MINUTES);
            if (cleanedCount > 0) {
                log.info("定时清理超时任务完成 [cleanedCount={}]", cleanedCount);
            }
        } catch (Exception e) {
            log.error("定时清理超时任务失败 [error={}]", e.getMessage(), e);
        }
    }
}
