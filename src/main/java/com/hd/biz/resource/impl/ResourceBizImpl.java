package com.hd.biz.resource.impl;

import com.hd.common.HomeDashConstants;
import com.hd.common.config.HomeDashProperties;
import com.hd.common.exception.DataFormatException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.hd.model.dto.MergeFileDto;
import com.hd.model.dto.ResponseDto;
import com.hd.model.dto.TransferTaskDto;
import com.hd.dao.entity.Resource;
import com.hd.dao.entity.ResourceChunk;
import com.hd.dao.service.FileDataService;
import com.hd.dao.service.ResourceChunkDataService;
import com.hd.dao.service.ResourceDataService;
import com.hd.common.exception.UploadException;
import com.hd.biz.resource.ResourceBiz;
import com.hd.common.util.FileUtils;
import com.hd.common.util.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ResourceBizImpl implements ResourceBiz {

        private static class TransferTaskState {
        private String identifier;
        private String fileName;
        private String fileType;
        private String operationType;
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

        private static final int MAX_RETRY_COUNT = 3;

        private static final long RETRY_INTERVAL_MS = 1000;

        private static final int DEFAULT_UPLOAD_TIMEOUT_MINUTES = 60;

        private static final int MD5_BUFFER_SIZE = 8192;

        private final Map<String, Long> uploadActivityCache = new ConcurrentHashMap<>();
        private final Map<String, TransferTaskState> transferTaskCache = new ConcurrentHashMap<>();

    private final HomeDashProperties homeDashProperties;
    private final ResourceChunkDataService resourceChunkDataService;
    private final ResourceDataService resourceDataService;
    private final FileDataService fileDataService;

        @Autowired
    public ResourceBizImpl(ResourceChunkDataService resourceChunkDataService,
            HomeDashProperties homeDashProperties,
            ResourceDataService resourceDataService, FileDataService fileDataService) {
        this.resourceChunkDataService = resourceChunkDataService;
        this.homeDashProperties = homeDashProperties;
        this.resourceDataService = resourceDataService;
        this.fileDataService = fileDataService;
    }

        @Override
    public boolean checkChunk(ResourceChunk chunk) {
        log.debug("检查文件分块 [identifier={}, chunkNumber={}]",
                chunk != null ? chunk.getIdentifier() : null,
                chunk != null ? chunk.getChunkNumber() : null);

        if (Objects.isNull(chunk)) {
            log.warn("检查分块失败：分块信息为空");
            return false;
        }
        if (Objects.isNull(chunk.getIdentifier()) || chunk.getIdentifier().trim().isEmpty()) {
            log.warn("检查分块失败：文件标识符为空");
            return false;
        }

        ResourceChunk resourceChunk = resourceChunkDataService.findByIdentifierAndChunkNumber(chunk.getIdentifier(),
                chunk.getChunkNumber());
        boolean exists = Objects.nonNull(resourceChunk);

        log.debug("文件分块检查完成 [identifier={}, chunkNumber={}, exists={}]",
                chunk.getIdentifier(), chunk.getChunkNumber(), exists);

        return exists;
    }

        @Override
    public java.util.List<ResourceChunk> getUploadedChunks(String identifier) {
        log.debug("获取已上传分块列表 [identifier={}]", identifier);

        try {
            java.util.List<ResourceChunk> chunks = resourceChunkDataService.findByIdentifier(identifier);

            log.info("已上传分块列表查询完成 [identifier={}, uploadedChunks={}, totalChunks={}]",
                    identifier, chunks.size(), chunks.isEmpty() ? 0 : chunks.get(0).getTotalChunks());

            return chunks;
        } catch (Exception e) {
            log.error("查询已上传分块列表失败 [identifier={}, error={}]", identifier, e.getMessage(), e);
            return new java.util.ArrayList<>();
        }
    }

        @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveChunk(ResourceChunk chunk) {
        long startTime = System.currentTimeMillis();
        log.info("开始保存文件分块 [identifier={}, chunkNumber={}, fileName={}, size={}]",
                chunk != null ? chunk.getIdentifier() : null,
                chunk != null ? chunk.getChunkNumber() : null,
                chunk != null ? chunk.getFilename() : null,
                (chunk != null && chunk.getFile() != null) ? chunk.getFile().getSize() : 0);

        if (Objects.isNull(chunk)) {
            throw new UploadException("分块信息不能为空");
        }
        if (Objects.isNull(chunk.getIdentifier()) || chunk.getIdentifier().trim().isEmpty()) {
            throw new UploadException("文件标识符不能为空");
        }
        if (Objects.isNull(chunk.getFile())) {
            throw new UploadException("分块文件数据不能为空");
        }

        String chunkDirPath = getChunkTmpDir(chunk.getIdentifier());
        String chunkPath = getChunkTmpPath(chunk);

        int retryCount = 0;
        Exception lastException = null;

        while (retryCount <= MAX_RETRY_COUNT) {
            try {
                if (retryCount > 0) {
                    log.warn("正在重试保存分块 [identifier={}, chunkNumber={}, retryCount={}]",
                            chunk.getIdentifier(), chunk.getChunkNumber(), retryCount);
                    Thread.sleep(RETRY_INTERVAL_MS);
                }

                Path chunkDir = Paths.get(chunkDirPath);
                if (!Files.exists(chunkDir)) {
                    Files.createDirectories(chunkDir);
                    log.debug("创建临时目录 [chunkDirPath={}]", chunkDirPath);
                }

                Path chunkFilePath = Paths.get(chunkPath);
                if (Files.exists(chunkFilePath)) {
                    log.debug("分块文件已存在，将覆盖 [chunkPath={}]", chunkPath);
                    Files.delete(chunkFilePath);
                }

                String calculatedMd5;
                try (java.io.BufferedOutputStream bos = new java.io.BufferedOutputStream(
                        Files.newOutputStream(chunkFilePath), 8192)) {

                    chunk.getFile().getInputStream().transferTo(bos);
                    bos.flush();

                    calculatedMd5 = calculateFileMD5(chunkFilePath);
                    log.debug("分块MD5计算完成 [identifier={}, chunkNumber={}, md5={}]",
                            chunk.getIdentifier(), chunk.getChunkNumber(), calculatedMd5);
                }

                ResourceChunk existingChunk = resourceChunkDataService.findByIdentifierAndChunkNumber(
                        chunk.getIdentifier(), chunk.getChunkNumber());

                if (Objects.nonNull(existingChunk)) {
                    log.debug("分块记录已存在，更新MD5信息 [identifier={}, chunkNumber={}]",
                            chunk.getIdentifier(), chunk.getChunkNumber());
                    resourceChunkDataService.updateChunkMd5(chunk.getIdentifier(), chunk.getChunkNumber(), calculatedMd5);
                } else {
                    chunk.setMd5(calculatedMd5);
                    resourceChunkDataService.save(chunk);
                    log.debug("分块记录保存成功 [identifier={}, chunkNumber={}, md5={}]",
                            chunk.getIdentifier(), chunk.getChunkNumber(), calculatedMd5);
                }

                uploadActivityCache.put(chunk.getIdentifier(), System.currentTimeMillis());
                markUploading(chunk);

                long elapsedTime = System.currentTimeMillis() - startTime;
                log.info("文件分块保存成功 [identifier={}, chunkNumber={}, chunkPath={}, md5={}, 耗时={}ms, retryCount={}]",
                        chunk.getIdentifier(), chunk.getChunkNumber(), chunkPath, calculatedMd5, elapsedTime,
                        retryCount);

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

                cleanupFailedChunk(chunkPath);

                if (retryCount > MAX_RETRY_COUNT) {
                    log.error("分块上传失败，已达最大重试次数 [identifier={}, chunkNumber={}, maxRetries={}]",
                            chunk.getIdentifier(), chunk.getChunkNumber(), MAX_RETRY_COUNT);
                    markFailed(chunk.getIdentifier(), chunk.getFilename(),
                            String.format("分块上传失败：%s", e.getMessage()));
                    break;
                }
            }
        }

        throw new UploadException(
                String.format(
                        "保存文件分块失败，已达最大重试次数 [identifier=%s, chunkNumber=%d, chunkPath=%s, maxRetries=%d, lastError=%s]",
                        chunk.getIdentifier(), chunk.getChunkNumber(), chunkPath, MAX_RETRY_COUNT,
                        lastException != null ? lastException.getMessage() : "unknown"));
    }

        @Transactional(rollbackFor = Exception.class)
    @Override
    public void mergeChunk(MergeFileDto mergeFileDto) {
        long startTime = System.currentTimeMillis();
        log.info("开始合并文件分块 [identifier={}, fileName={}, fileSize={}]",
                mergeFileDto != null ? mergeFileDto.getIdentifier() : null,
                mergeFileDto != null ? mergeFileDto.getFileName() : null,
                mergeFileDto != null ? mergeFileDto.getSize() : 0);

        if (Objects.isNull(mergeFileDto)) {
            throw new UploadException("合并文件信息不能为空");
        }
        if (Objects.isNull(mergeFileDto.getIdentifier()) || mergeFileDto.getIdentifier().trim().isEmpty()) {
            throw new UploadException("文件标识符不能为空");
        }
        if (Objects.isNull(mergeFileDto.getFileName()) || mergeFileDto.getFileName().trim().isEmpty()) {
            throw new UploadException("文件名不能为空");
        }
        if (Objects.isNull(mergeFileDto.getCreateTime())) {
            mergeFileDto.setCreateTime(new Date());
            log.debug("合并文件创建时间为空，使用当前时间 [identifier={}]", mergeFileDto.getIdentifier());
        }

        try {
            MergeResult mergeResult = mergeChunksAndCalculateMD5(mergeFileDto);
            String md5 = mergeResult.getMd5();
            String relativePath = mergeResult.getRelativePath();

            Resource existingResource = resourceDataService.lambdaQuery()
                    .eq(Resource::getMd5, md5)
                    .one();
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

        private String getChunkTmpDir(String identifier) {
        return homeDashProperties.getResourceTmpDir() + File.separator
                + identifier;
    }

        private String getChunkTmpPath(ResourceChunk chunk) {
        String fileName = chunk.getFilename();
        String safeFileName = fileName
                .replaceAll("[\\\\/:*?\"<>|]", "_")
                .replaceAll("\\s+", "_");

        return getChunkTmpDir(chunk.getIdentifier()) + File.separator
                + safeFileName + "-" + chunk.getChunkNumber();
    }

        private MergeResult mergeChunksAndCalculateMD5(MergeFileDto mergeFileDto) {
        long startTime = System.currentTimeMillis();
        log.debug("开始合并分块文件并计算MD5 [identifier={}, fileName={}]",
                mergeFileDto.getIdentifier(), mergeFileDto.getFileName());

        String targetFileDir = homeDashProperties.getBasicResourcePath() + File.separator
                + Utils.formatDate(mergeFileDto.getCreateTime(), "yyyy") + File.separator
                + Utils.formatDate(mergeFileDto.getCreateTime(), "MM");
        Utils.createFolders(targetFileDir);

        String uuid = Utils.uuid();

        String targetFilePath = targetFileDir + File.separator + uuid +
                FileUtils.extractFileExtensionName(mergeFileDto.getFileName());

        log.debug("目标文件路径 [targetFilePath={}]", targetFilePath);

        try {
            java.util.List<Path> chunkPaths = getSortedChunkPaths(mergeFileDto);

            if (chunkPaths.isEmpty()) {
                log.error("未找到分块文件 [identifier={}, fileName={}]",
                        mergeFileDto.getIdentifier(), mergeFileDto.getFileName());
                throw new UploadException(
                        String.format("未找到分块文件 [identifier=%s, fileName=%s]",
                                mergeFileDto.getIdentifier(), mergeFileDto.getFileName()));
            }

            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");

            try (java.io.BufferedOutputStream bos = new java.io.BufferedOutputStream(
                    Files.newOutputStream(Paths.get(targetFilePath)), 65536);
                    java.security.DigestOutputStream dos = new java.security.DigestOutputStream(bos, md)) {

                byte[] buffer = new byte[65536];

                for (Path chunkPath : chunkPaths) {
                    try (java.io.BufferedInputStream bis = new java.io.BufferedInputStream(
                            Files.newInputStream(chunkPath), 65536)) {
                        int bytesRead;
                        while ((bytesRead = bis.read(buffer)) != -1) {
                            dos.write(buffer, 0, bytesRead);
                        }
                    }

                    Files.delete(chunkPath);
                    log.debug("已删除分块文件 [chunkPath={}]", chunkPath);
                }

                dos.flush();
            }

            String md5 = bytesToHex(md.digest());

            log.info("文件MD5计算完成 [identifier={}, fileName={}, md5={}]",
                    mergeFileDto.getIdentifier(), mergeFileDto.getFileName(), md5);

            Utils.deleteFile(getChunkTmpDir(mergeFileDto.getIdentifier()));

            resourceChunkDataService.deleteChunk(mergeFileDto.getIdentifier());

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

        private java.util.List<Path> getSortedChunkPaths(MergeFileDto mergeFileDto) throws IOException {
        java.util.List<Path> chunkPaths = new java.util.ArrayList<>();

        String originalFileName = mergeFileDto.getFileName();

        try (java.util.stream.Stream<Path> paths = Files
                .list(Paths.get(getChunkTmpDir(mergeFileDto.getIdentifier())))) {
            paths
                    .filter(path -> {
                        String fileName = path.getFileName().toString();
                        return !fileName.equals(originalFileName) && fileName.contains("-");
                    })
                    .sorted((o1, o2) -> {
                        String p1 = o1.getFileName().toString();
                        String p2 = o2.getFileName().toString();

                        try {
                            int i1 = p1.lastIndexOf("-");
                            int i2 = p2.lastIndexOf("-");

                            if (i1 < 0 || i2 < 0) {
                                return p1.compareTo(p2);
                            }

                            String numStr1 = p1.substring(i1 + 1);
                            String numStr2 = p2.substring(i2 + 1);

                            Integer num1 = Integer.parseInt(numStr1);
                            Integer num2 = Integer.parseInt(numStr2);

                            return num1.compareTo(num2);
                        } catch (NumberFormatException e) {
                            log.warn("解析分块编号失败，使用字典序排序 [file1={}, file2={}]", p1, p2);
                            return p1.compareTo(p2);
                        }
                    })
                    .forEach(chunkPaths::add);
        }

        return chunkPaths;
    }

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

        private void generateRecord(MergeFileDto fileDto, String md5, String relativePath) {
        log.debug("开始生成资源记录和文件记录 [identifier={}, fileName={}, md5={}, relativePath={}]",
                fileDto.getIdentifier(), fileDto.getFileName(), md5, relativePath);

        Resource resource = Resource.builder()
                .link(1)
                .path(relativePath)
                .md5(md5)
                .size(fileDto.getSize())
                .build();

        resourceDataService.save(resource);

        log.info("资源记录创建成功 [resourceId={}, path={}, size={}, md5={}]",
                resource.getId(), resource.getPath(), resource.getSize(), resource.getMd5());

        fileDto.setResourceId(resource.getId());
        fileDto.setType(FileUtils.getFileType(fileDto.getFileName()).toString());

        fileDataService.save(fileDto);

        log.debug("文件记录创建完成 [identifier={}, fileName={}, resourceId={}]",
                fileDto.getIdentifier(), fileDto.getFileName(), resource.getId());
    }

        private void deleteTempFiles(MergeFileDto mergeFileDto) {
        log.debug("开始删除临时文件和分块记录 [identifier={}, fileName={}]",
                mergeFileDto.getIdentifier(), mergeFileDto.getFileName());

        try {
            Utils.deleteFile(getChunkTmpDir(mergeFileDto.getIdentifier()));

            resourceChunkDataService.deleteChunk(mergeFileDto.getIdentifier());

            log.info("临时文件和分块记录删除成功 [identifier={}, fileName={}]",
                    mergeFileDto.getIdentifier(), mergeFileDto.getFileName());
        } catch (Exception e) {
            log.error("删除临时文件失败 [identifier={}, fileName={}]",
                    mergeFileDto.getIdentifier(), mergeFileDto.getFileName(), e);
        }
    }

        private void deleteMergedFile(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) {
            return;
        }

        try {
            String fullPath = homeDashProperties.getBasicResourcePath() + File.separator + relativePath;
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

        private void createFileWithExistingResource(MergeFileDto fileDto, Resource existingResource) {
        log.debug("开始使用现有资源创建文件记录 [identifier={}, fileName={}, existingResourceId={}]",
                fileDto.getIdentifier(), fileDto.getFileName(), existingResource.getId());

        Resource resource = resourceDataService.getById(existingResource.getId());
        if (Objects.isNull(resource)) {
            log.error("资源不存在，数据库数据异常 [identifier={}, resourceId={}]",
                    fileDto.getIdentifier(), existingResource.getId());
            throw new UploadException(
                    String.format("资源不存在，数据库数据异常 [identifier=%s, resourceId=%d]",
                            fileDto.getIdentifier(), existingResource.getId()));
        }

        resourceDataService.lambdaUpdate()
                .eq(Resource::getId, resource.getId())
                .set(Resource::getLink, resource.getLink() + 1)
                .update();

        log.info("资源引用计数增加 [identifier={}, resourceId={}, oldLink={}, newLink={}]",
                fileDto.getIdentifier(), resource.getId(), resource.getLink(), resource.getLink() + 1);

        fileDto.setResourceId(resource.getId());
        fileDto.setType(FileUtils.getFileType(fileDto.getFileName()).toString());

        fileDataService.save(fileDto);

        log.info("秒传文件记录创建成功 [identifier={}, fileName={}, resourceId={}]",
                fileDto.getIdentifier(), fileDto.getFileName(), resource.getId());
    }

        @Override
    public boolean verifyChunkIntegrity(String identifier, Integer chunkNumber, String expectedMd5) {
        log.debug("开始验证分块完整性 [identifier={}, chunkNumber={}, expectedMd5={}]",
                identifier, chunkNumber, expectedMd5 != null ? expectedMd5 : "null");

        if (Objects.isNull(identifier) || identifier.trim().isEmpty()) {
            log.warn("验证分块完整性失败：文件标识符为空");
            return false;
        }
        if (Objects.isNull(chunkNumber)) {
            log.warn("验证分块完整性失败：分块编号为空");
            return false;
        }

        try {
            ResourceChunk chunkRecord = resourceChunkDataService.findByIdentifierAndChunkNumber(identifier, chunkNumber);
            if (Objects.isNull(chunkRecord)) {
                log.debug("分块记录不存在 [identifier={}, chunkNumber={}]", identifier, chunkNumber);
                return false;
            }

            String chunkPath = getChunkTmpDir(identifier) + File.separator +
                    sanitizeFileName(chunkRecord.getFilename()) + "-" + chunkNumber;

            Path chunkFilePath = Paths.get(chunkPath);

            if (!Files.exists(chunkFilePath)) {
                log.warn("分块物理文件不存在 [identifier={}, chunkNumber={}, chunkPath={}]",
                        identifier, chunkNumber, chunkPath);
                return false;
            }

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

        @Transactional(rollbackFor = Exception.class)
    @Override
    public void cancelUpload(String identifier) {
        log.info("开始取消文件上传 [identifier={}]", identifier);

        if (Objects.isNull(identifier) || identifier.trim().isEmpty()) {
            log.warn("取消上传失败：文件标识符为空");
            throw new UploadException("文件标识符不能为空");
        }

        try {
            List<ResourceChunk> chunksBeforeCleanup = resourceChunkDataService.findByIdentifier(identifier);
            int chunksCount = chunksBeforeCleanup.size();
            log.debug("准备清理的分块数量 [identifier={}, chunksCount={}]", identifier, chunksCount);

            String chunkTmpDir = getChunkTmpDir(identifier);
            Utils.deleteFile(chunkTmpDir);
            log.debug("临时目录删除成功 [chunkTmpDir={}]", chunkTmpDir);

            resourceChunkDataService.deleteChunk(identifier);
            log.debug("数据库分块记录删除成功 [identifier={}]", identifier);

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
            uploadActivityCache.forEach((identifier, lastActivityTime) -> {
                if (currentTime - lastActivityTime > timeoutMillis) {
                    timeoutIdentifiers.add(identifier);
                    log.debug("发现超时任务 [identifier={}, lastActivityTime={}, timeoutMinutes={}]",
                            identifier, new Date(lastActivityTime), finalTimeoutMinutes);
                }
            });

            for (String identifier : timeoutIdentifiers) {
                try {
                    log.info("正在清理超时任务 [identifier={}]", identifier);
                    cancelUpload(identifier);
                    cleanedCount++;
                } catch (Exception e) {
                    log.error("清理超时任务失败 [identifier={}, error={}]", identifier, e.getMessage());
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

    @Override
    public boolean clearTransferTask(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return false;
        }
        TransferTaskState removed = transferTaskCache.remove(identifier);
        if (removed != null) {
            log.info("传输任务记录已清除 [identifier={}, fileName={}]", identifier, removed.fileName);
            return true;
        }
        log.debug("传输任务记录不存在 [identifier={}]", identifier);
        return false;
    }

    private void markUploading(ResourceChunk chunk) {
        TransferTaskState state = transferTaskCache.computeIfAbsent(chunk.getIdentifier(), this::newTransferTaskState);
        state.fileName = chunk.getFilename();
        state.operationType = "upload";
        state.status = HomeDashConstants.TransferStatus.UPLOADING;
        state.errorMessage = null;
        state.totalSize = chunk.getTotalSize();
        state.totalChunks = chunk.getTotalChunks();
        state.uploadedChunks = resourceChunkDataService.findByIdentifier(chunk.getIdentifier()).size();
        state.updateTime = new Date();
        if (state.createTime == null) {
            state.createTime = new Date();
        }
    }

    private void markCompleted(MergeFileDto fileDto) {
        TransferTaskState state = transferTaskCache.computeIfAbsent(fileDto.getIdentifier(), this::newTransferTaskState);
        state.fileName = fileDto.getFileName();
        state.status = HomeDashConstants.TransferStatus.COMPLETED;
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
        state.status = HomeDashConstants.TransferStatus.FAILED;
        state.errorMessage = errorMessage;
        state.updateTime = new Date();
        if (state.createTime == null) {
            state.createTime = new Date();
        }
    }

    private void markCancelled(String identifier, String message) {
        TransferTaskState state = transferTaskCache.computeIfAbsent(identifier, this::newTransferTaskState);
        state.status = HomeDashConstants.TransferStatus.CANCELLED;
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
        if (HomeDashConstants.TransferStatus.COMPLETED.equals(state.status)) {
            progress = 100;
        }
        return TransferTaskDto.builder()
                .identifier(state.identifier)
                .fileName(state.fileName)
                .fileType(state.fileType)
                .operationType(state.operationType)
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
        String normalizedInput = status == null ? HomeDashConstants.TransferStatus.COMPLETED : status.trim().toLowerCase();
        if (normalizedInput.isEmpty()) {
            normalizedInput = HomeDashConstants.TransferStatus.COMPLETED;
        }

        for (String item : normalizedInput.split(",")) {
            String normalized = item.trim();
            if (normalized.isEmpty()) {
                continue;
            }
            switch (normalized) {
                case "completed", "failed", "cancelled" -> statuses.add(normalized);
                case "finished" -> statuses.addAll(List.of(
                        HomeDashConstants.TransferStatus.COMPLETED,
                        HomeDashConstants.TransferStatus.FAILED,
                        HomeDashConstants.TransferStatus.CANCELLED));
                case "all" -> statuses.addAll(List.of(
                        HomeDashConstants.TransferStatus.COMPLETED,
                        HomeDashConstants.TransferStatus.FAILED,
                        HomeDashConstants.TransferStatus.CANCELLED));
                default -> throw new DataFormatException(
                        String.format("不支持的传输状态清理类型 [status=%s]", status));
            }
        }

        if (statuses.isEmpty()) {
            statuses.add(HomeDashConstants.TransferStatus.COMPLETED);
        }
        return statuses;
    }

    private String normalizeTransferStatus(String status) {
        return status == null ? "" : status.trim().toLowerCase();
    }

        private String calculateFileMD5(Path filePath) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");

        try (DigestInputStream dis = new DigestInputStream(
                Files.newInputStream(filePath), md)) {

            byte[] buffer = new byte[MD5_BUFFER_SIZE];
            while (dis.read(buffer) != -1) {
            }
        }

        return bytesToHex(md.digest());
    }

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

        private String sanitizeFileName(String fileName) {
        if (fileName == null) {
            return "unknown";
        }
        return fileName
                .replaceAll("[\\\\/:*?\"<>|]", "_")
                .replaceAll("\\s+", "_");
    }

        @Scheduled(fixedRate = 3600000)
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

