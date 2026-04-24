package com.hd.biz.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.hd.biz.FileBiz;
import com.hd.common.config.HomeDashProperties;
import com.hd.common.enums.FileType;
import com.hd.common.exception.*;
import com.hd.common.util.FileUtils;
import com.hd.common.util.MD5Utils;
import com.hd.dao.entity.File;
import com.hd.dao.entity.Resource;
import com.hd.dao.service.FileDataService;
import com.hd.dao.service.ResourceDataService;
import com.hd.model.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author xhx
 * @version 1.0
 * @package com.hd.biz.impl
 * @createTime 2026/04/24 11:17
 * @description 文件服务实现类。实现文件管理的核心业务逻辑，包括文件的增删改查、移动、复制、下载等功能。在多文件和文件夹删除/复制数据时使用悲观锁，可能会出现死锁，但考虑到个人部署使用，系统并发量并不大，出现死锁的概率很低。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileBizImpl implements FileBiz {

    private static final int DEFAULT_RECENT_LIMIT = 20;
    private static final int MAX_RECENT_LIMIT = 100;
    private static final List<String> DOCUMENT_TYPES = List.of(
            FileType.PDF.toString(),
            FileType.DOC.toString(),
            FileType.TXT.toString(),
            FileType.PPT.toString(),
            FileType.CODE.toString(),
            FileType.WEB.toString());
    private static final List<String> COMPRESS_TYPES = List.of(FileType.COMPRESS_FILE.toString());

    private final FileDataService fileDataService;
    private final ResourceDataService resourceDataService;
    private final HomeDashProperties homeDashProperties;

    /**
     * 根据父文件夹ID获取文件列表。
     * 同时返回文件夹路径信息用于面包屑导航。
     *
     * @param parentId  父文件夹ID，0表示根目录
     * @param sortBy    排序字段
     * @param sortOrder 排序方式
     * @return 包含文件列表和文件夹路径的响应对象
     */
    @Transactional(readOnly = true)
    @Override
    public ResponseDto findByParentId(Long parentId, String sortBy, String sortOrder) {
        boolean isAsc = "asc".equalsIgnoreCase(normalizeSortOrder(sortOrder));
        List<File> files = fileDataService.lambdaQuery()
                .eq(File::getParentId, parentId)
                .orderBy(true, isAsc, getSortColumn(sortBy))
                .list();
        List<FolderPathDto> folderPaths = getFolderTree(parentId);
        return ResponseDto.success(files, folderPaths);
    }

    @Override
    public ResponseDto findRecentFiles(Integer limit) {
        int safeLimit = normalizeLimit(limit);
        List<File> recentFiles = fileDataService.lambdaQuery()
                .orderByDesc(File::getCreateTime)
                .last("LIMIT " + safeLimit)
                .list();
        return ResponseDto.success(recentFiles, Map.of("limit", safeLimit));
    }

    @Override
    public ResponseDto getRecentUploadSummary(Integer limit) {
        int safeLimit = normalizeLimit(limit);

        final LocalDateTime now = LocalDateTime.now();
        final LocalDateTime todayStart = now.with(LocalTime.MIN);
        final LocalDateTime weekStart = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).with(LocalTime.MIN);
        final LocalDateTime monthStart = now.with(TemporalAdjusters.firstDayOfMonth()).with(LocalTime.MIN);

        List<File> todayFiles = queryByTimeRange(todayStart, now);
        List<File> weekFiles = queryByTimeRange(weekStart, now);
        List<File> monthFiles = queryByTimeRange(monthStart, now);

        long todaySize = todayFiles.stream().mapToLong(f -> f.getSize() != null ? f.getSize() : 0).sum();
        long weekSize = weekFiles.stream().mapToLong(f -> f.getSize() != null ? f.getSize() : 0).sum();
        long monthSize = monthFiles.stream().mapToLong(f -> f.getSize() != null ? f.getSize() : 0).sum();

        List<File> recentFiles = fileDataService.lambdaQuery()
                .orderByDesc(File::getCreateTime)
                .last("LIMIT " + safeLimit)
                .list();

        List<RecentUploadSummaryDto.File> recentFileList = recentFiles.stream()
                .map(f -> RecentUploadSummaryDto.File.builder()
                        .id(f.getId())
                        .fileName(f.getFileName())
                        .type(f.getType())
                        .size(f.getSize())
                        .createTime(f.getCreateTime())
                        .parentId(f.getParentId())
                        .build())
                .toList();

        RecentUploadSummaryDto summary = RecentUploadSummaryDto.builder()
                .recentFiles(recentFileList)
                .todayCount(todayFiles.size())
                .weekCount(weekFiles.size())
                .monthCount(monthFiles.size())
                .totalCount(recentFiles.size())
                .todaySize(todaySize)
                .weekSize(weekSize)
                .monthSize(monthSize)
                .querySummary(Map.of("limit", safeLimit))
                .build();

        return ResponseDto.success(summary);
    }

    @Override
    public ResponseDto findFilesByCategory(String category, String sortBy, String sortOrder) {
        String normalizedCategory = normalizeCategory(category);
        boolean isAsc = "asc".equalsIgnoreCase(normalizeSortOrder(sortOrder));

        List<File> files = fileDataService.lambdaQuery()
                .orderBy(true, isAsc, getSortColumn(sortBy))
                .list().stream()
                .filter(file -> !FileType.FOLDER.toString().equals(file.getType()))
                .filter(file -> matchesCategory(file, normalizedCategory))
                .toList();
        return ResponseDto.success(files, Map.of(
                "category", normalizedCategory,
                "count", files.size()));
    }

    @Override
    public ResponseDto categorySummary() {
        Map<String, Long> categoryCountMap = fileDataService.list().stream()
                .filter(file -> !FileType.FOLDER.toString().equals(file.getType()))
                .collect(Collectors.groupingBy(this::getCategoryForFileType, Collectors.counting()));

        List<FileCategorySummaryDto> summaries = List.of(
                new FileCategorySummaryDto("picture", "图片", categoryCountMap.getOrDefault("picture", 0L).intValue()),
                new FileCategorySummaryDto("video", "视频", categoryCountMap.getOrDefault("video", 0L).intValue()),
                new FileCategorySummaryDto("audio", "音频", categoryCountMap.getOrDefault("audio", 0L).intValue()),
                new FileCategorySummaryDto("document", "文档", categoryCountMap.getOrDefault("document", 0L).intValue()),
                new FileCategorySummaryDto("compress", "压缩包", categoryCountMap.getOrDefault("compress", 0L).intValue()),
                new FileCategorySummaryDto("other", "其他", categoryCountMap.getOrDefault("other", 0L).intValue()));
        return ResponseDto.success(summaries);
    }

    /**
     * 根据文件ID获取文件详情。
     *
     * @param fileId 文件ID
     * @return 包含文件详情的响应对象
     * @throws DataNotFoundException 当文件不存在时抛出
     */
    @Override
    public ResponseDto findByFileId(Long fileId) {
        File file = findById(fileId)
                .orElseThrow(() -> new DataNotFoundException(String.format("文件不存在 [fileId=%d]", fileId)));

        List<FolderPathDto> navigation = FileType.FOLDER.toString().equals(file.getType())
                ? getFolderTree(file.getId())
                : getFolderTree(file.getParentId());

        FileDetailDto detail = FileDetailDto.builder()
                .id(file.getId())
                .parentId(file.getParentId())
                .resourceId(file.getResourceId())
                .size(file.getSize())
                .fileName(file.getFileName())
                .type(file.getType())
                .extension(FileUtils.extractFileExtensionName(file.getFileName()))
                .folderPath(buildFolderPath(navigation))
                .downloadable(!FileType.FOLDER.toString().equals(file.getType()))
                .playable(FileType.VIDEO.toString().equals(file.getType()))
                .createTime(file.getCreateTime())
                .updateTime(file.getUpdateTime())
                .navigation(navigation)
                .build();

        return ResponseDto.success(detail);
    }

    /**
     * 创建新文件或文件夹。
     * 验证文件名和类型，并检查父文件夹是否存在。
     *
     * @param file 文件信息对象
     * @throws DataFormatException 当文件名或类型不合法，或父文件夹不存在时抛出
     */
    @Override
    public void createFile(File file) {
        log.info("开始创建文件 [fileName={}, parentId={}, type={}]",
                file.getFileName(), file.getParentId(), file.getType());

        FileUtils.checkFileName(file.getFileName());
        FileUtils.checkFileType(file.getType());
        File parentFile = findById(file.getParentId())
                .orElseThrow(() -> new DataFormatException(
                        String.format("父文件夹不存在 [parentId=%d]", file.getParentId())));

        if (!FileType.FOLDER.toString().equals(parentFile.getType())) {
            log.error("父文件夹不是文件夹类型 [parentId={}, fileName={}]",
                    file.getParentId(), file.getFileName());
            throw new DataFormatException(
                    String.format("父文件夹不是文件夹类型 [parentId=%d, fileName=%s]",
                            file.getParentId(), file.getFileName()));
        }

        ensureUniqueFileName(file.getParentId(), file.getFileName(), null);
        fileDataService.save(file);

        log.info("文件创建成功 [fileId={}, fileName={}, parentId={}]",
                file.getId(), file.getFileName(), file.getParentId());
    }

    /**
     * 重命名文件或文件夹。
     *
     * @param fileName 新文件名
     * @param id       文件ID
     * @throws DataFormatException 当文件名不合法时抛出
     */
    @Override
    public void renameFile(String fileName, Long id) {
        log.info("开始重命名文件 [fileId={}, newFileName={}]", id, fileName);

        Objects.requireNonNull(id, "文件ID不能为空");
        if (Objects.isNull(fileName) || fileName.trim().isEmpty()) {
            throw new DataFormatException("文件名不能为空");
        }

        FileUtils.checkFileName(fileName);
        File existingFile = findById(id)
                .orElseThrow(() -> new DataNotFoundException(String.format("文件不存在 [fileId=%d]", id)));

        ensureUniqueFileName(existingFile.getParentId(), fileName, id);

        File updateFile = File.builder()
                .id(id).fileName(fileName).updateTime(new Date())
                .build();
        fileDataService.updateById(updateFile);

        log.info("文件重命名成功 [fileId={}, newFileName={}]", id, fileName);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void moveFiles(List<Long> ids, Long targetId) {
        long startTime = System.currentTimeMillis();
        log.info("开始移动文件 [fileIds={}, targetId={}, fileCount={}]", ids, targetId, ids != null ? ids.size() : 0);

        Objects.requireNonNull(ids, "要移动的文件ID列表不能为空");
        Objects.requireNonNull(targetId, "目标文件夹ID不能为空");

        File targetFolder = findById(targetId)
                .orElseThrow(() -> new DataNotFoundException(
                        String.format("目标文件夹不存在 [targetId=%d]", targetId)));
        if (!FileType.FOLDER.toString().equals(targetFolder.getType())) {
            throw new DataFormatException(
                    String.format("目标不是文件夹类型 [targetId=%d]", targetId));
        }

        List<File> filesToMove = fileDataService.listByIds(ids);
        if (filesToMove.size() != ids.size()) {
            Set<Long> foundIds = filesToMove.stream().map(File::getId).collect(Collectors.toSet());
            List<Long> missingIds = ids.stream().filter(id -> !foundIds.contains(id)).toList();
            throw new DataNotFoundException(
                    String.format("部分要移动的文件不存在 [missingFileIds=%s]", missingIds));
        }

        Set<String> targetFilenames = fileDataService.lambdaQuery()
                .eq(File::getParentId, targetId)
                .list().stream()
                .map(File::getFileName)
                .collect(Collectors.toSet());

        for (File file : filesToMove) {
            if (file.getId().equals(targetId)) {
                throw new DataFormatException("不能将文件移动到自身");
            }
            if (FileType.FOLDER.toString().equals(file.getType()) && isDescendant(targetId, file.getId())) {
                throw new DataFormatException("不能将文件夹移动到其子文件夹中");
            }
            if (targetFilenames.contains(file.getFileName())) {
                throw new FileAlreadyExistsException(
                        String.format("目标目录已存在同名文件 [fileName=%s]", file.getFileName()));
            }
        }

        fileDataService.lambdaUpdate()
                .in(File::getId, ids)
                .set(File::getParentId, targetId)
                .set(File::getUpdateTime, new Date())
                .update();

        log.info("文件移动成功 [fileIds={}, targetId={}, count={}, 总耗时={}ms]",
                ids, targetId, ids.size(), System.currentTimeMillis() - startTime);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void copyFiles(List<Long> fileIds, List<Long> targetIds) {
        long startTime = System.currentTimeMillis();
        log.info("开始复制文件 [fileIds={}, targetIds={}, fileCount={}, targetCount={}]",
                fileIds, targetIds, fileIds != null ? fileIds.size() : 0, targetIds != null ? targetIds.size() : 0);

        Objects.requireNonNull(fileIds, "要复制的文件ID列表不能为空");
        Objects.requireNonNull(targetIds, "目标文件夹ID列表不能为空");

        List<File> filesToCopy = fileDataService.listByIds(fileIds);
        if (filesToCopy.size() != fileIds.size()) {
            Set<Long> foundIds = filesToCopy.stream().map(File::getId).collect(Collectors.toSet());
            List<Long> missingIds = fileIds.stream().filter(id -> !foundIds.contains(id)).toList();
            throw new DataNotFoundException(
                    String.format("部分要复制的文件不存在 [missingFileIds=%s]", missingIds));
        }

        for (Long targetId : targetIds) {
            File targetFile = findById(targetId)
                    .orElseThrow(() -> new DataNotFoundException(
                            String.format("目标文件夹不存在 [targetId=%d]", targetId)));
            if (!FileType.FOLDER.toString().equals(targetFile.getType())) {
                throw new DataFormatException(
                        String.format("目标不是文件夹类型 [targetId=%d]", targetId));
            }

            Map<Boolean, List<File>> fileGroups = filesToCopy.stream()
                    .collect(Collectors.partitioningBy(
                            file -> FileType.FOLDER.toString().equals(file.getType())));

            List<File> folders = fileGroups.get(true);
            List<File> commonFiles = fileGroups.get(false);

            if (!commonFiles.isEmpty()) {
                batchCopyCommonFiles(commonFiles, targetFile);
            }

            for (File folder : folders) {
                copyFolder(folder, targetFile);
            }
        }

        log.info("文件复制成功 [fileIds={}, targetIds={}, 总耗时={}ms]",
                fileIds, targetIds, System.currentTimeMillis() - startTime);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteFiles(List<Long> ids) {
        long startTime = System.currentTimeMillis();
        log.info("开始删除文件 [fileIds={}, fileCount={}]", ids, ids != null ? ids.size() : 0);

        Objects.requireNonNull(ids, "要删除的文件ID列表不能为空");

        List<File> filesToDelete = fileDataService.listByIds(ids);
        if (filesToDelete.size() != ids.size()) {
            Set<Long> foundIds = filesToDelete.stream().map(File::getId).collect(Collectors.toSet());
            List<Long> missingIds = ids.stream().filter(id -> !foundIds.contains(id)).toList();
            log.warn("部分要删除的文件不存在 [missingFileIds={}]", missingIds);
        }

        Map<Boolean, List<File>> fileGroups = filesToDelete.stream()
                .collect(Collectors.partitioningBy(
                        file -> FileType.FOLDER.toString().equals(file.getType())));

        List<File> folders = fileGroups.get(true);
        List<File> commonFiles = fileGroups.get(false);

        if (!commonFiles.isEmpty()) {
            batchDeleteCommonFiles(commonFiles);
        }

        for (File folder : folders) {
            deleteFolderIterative(folder);
        }

        log.info("文件删除成功 [fileIds={}, 总耗时={}ms]", ids, System.currentTimeMillis() - startTime);
    }

    @Override
    public FileBiz.ResourceWrapper loadResource(Long fileId) {
        File file = findById(fileId)
                .orElseThrow(() -> new DataNotFoundException(
                        String.format("文件不存在或文件无关联资源 [fileId=%d]", fileId)));
        if (Objects.isNull(file.getResourceId())) {
            throw new DataNotFoundException(
                    String.format("文件无关联资源 [fileId=%d]", fileId));
        }

        Resource dbResource = Optional.ofNullable(resourceDataService.getById(file.getResourceId()))
                .orElseThrow(() -> new DataNotFoundException(
                        String.format("资源不存在 [fileId=%d, resourceId=%d]", fileId, file.getResourceId())));

        try {
            Path basePath = Paths.get(homeDashProperties.getBasicResourcePath());
            Path filePath = basePath.resolve(dbResource.getPath()).normalize();

            if (!filePath.startsWith(basePath)) {
                log.error("文件路径不在允许的目录内 [fileId={}, filePath={}, basePath={}]",
                        fileId, filePath, basePath);
                throw new DataNotFoundException(
                        String.format("非法的文件路径 [fileId=%d]", fileId));
            }

            org.springframework.core.io.Resource springResource = new UrlResource(filePath.toUri());

            if (springResource.exists()) {
                return new FileBiz.ResourceWrapper(springResource, file);
            } else {
                throw new DataNotFoundException(
                        String.format("物理文件不存在 [fileId=%d, resourcePath=%s]", fileId, dbResource.getPath()));
            }
        } catch (MalformedURLException e) {
            log.error("文件URL转换失败 [fileId={}, resourcePath={}]", fileId, dbResource.getPath(), e);
            throw new DataNotFoundException(
                    String.format("文件资源访问失败 [fileId=%d]", fileId));
        }
    }

    private List<FolderPathDto> getFolderTree(Long parentId) {
        LinkedList<FolderPathDto> path = new LinkedList<>();
        Long currentId = parentId;

        while (currentId != null && !currentId.equals(File.ROOT_FILE.getId())) {
            final Long idToFind = currentId;
            File currentFile = findById(idToFind)
                    .orElseThrow(() -> new DataNotFoundException(String.format("Folder with ID %d not found.", idToFind)));

            if (!FileType.FOLDER.toString().equals(currentFile.getType())) {
                throw new DataFormatException(String.format("ID %d is not a folder.", currentId));
            }
            path.addFirst(new FolderPathDto(currentFile.getId(), currentFile.getFileName()));
            currentId = currentFile.getParentId();
        }

        path.addFirst(new FolderPathDto(File.ROOT_FILE.getId(), File.ROOT_FILE.getFileName()));
        return path;
    }

    private void batchDeleteCommonFiles(List<File> files) {
        log.debug("开始批量删除普通文件 [文件数量={}]", files.size());

        List<Long> fileIdsToDelete = files.stream().map(File::getId).toList();
        Map<Long, Long> deleteCountByResourceId = files.stream()
                .filter(f -> f.getResourceId() != null)
                .collect(Collectors.groupingBy(File::getResourceId, Collectors.counting()));

        if (deleteCountByResourceId.isEmpty()) {
            if (!fileIdsToDelete.isEmpty()) {
                fileDataService.removeByIds(fileIdsToDelete);
            }
            return;
        }

        List<Resource> resources = resourceDataService.listByIds(deleteCountByResourceId.keySet());
        List<Long> resourceIdsToDelete = new ArrayList<>();
        List<Resource> resourcesToUpdate = new ArrayList<>();
        List<Resource> physicalFilesToDelete = new ArrayList<>();

        for (Resource resource : resources) {
            long count = deleteCountByResourceId.getOrDefault(resource.getId(), 0L);
            if (resource.getLink() - count <= 0) {
                resourceIdsToDelete.add(resource.getId());
                physicalFilesToDelete.add(resource);
            } else {
                resource.setLink(resource.getLink() - (int) count);
                resourcesToUpdate.add(resource);
            }
        }

        if (!physicalFilesToDelete.isEmpty()) {
            physicalFilesToDelete.forEach(resource -> {
                try {
                    Files.deleteIfExists(Paths.get(homeDashProperties.getBasicResourcePath(), resource.getPath()));
                } catch (IOException e) {
                    log.error("物理文件删除失败 [resourceId={}, resourcePath={}]",
                            resource.getId(), resource.getPath(), e);
                }
            });
            resourceDataService.removeByIds(resourceIdsToDelete);
        }

        if (!resourcesToUpdate.isEmpty()) {
            resourceDataService.updateBatchById(resourcesToUpdate);
        }

        if (!fileIdsToDelete.isEmpty()) {
            fileDataService.removeByIds(fileIdsToDelete);
        }
    }

    private void deleteFolderIterative(File folder) {
        log.debug("开始迭代删除文件夹 [folderId={}, folderName={}]", folder.getId(), folder.getFileName());

        Stack<File> folderStack = new Stack<>();
        folderStack.push(folder);

        List<File> allFilesToDelete = new ArrayList<>();
        List<File> allFoldersToDelete = new ArrayList<>();
        allFoldersToDelete.add(folder);

        while (!folderStack.isEmpty()) {
            File currentFolder = folderStack.pop();
            List<File> children = fileDataService.lambdaQuery().eq(File::getParentId, currentFolder.getId()).list();

            for (File child : children) {
                if (FileType.FOLDER.toString().equals(child.getType())) {
                    folderStack.push(child);
                    allFoldersToDelete.add(child);
                } else {
                    allFilesToDelete.add(child);
                }
            }
        }

        if (!allFilesToDelete.isEmpty()) {
            batchDeleteCommonFiles(allFilesToDelete);
        }

        Collections.reverse(allFoldersToDelete);
        List<Long> folderIdsToDelete = allFoldersToDelete.stream().map(File::getId).toList();
        fileDataService.removeByIds(folderIdsToDelete);

        log.info("迭代删除文件夹成功 [folderId={}, folderName={}, 删除文件夹数量={}, 删除文件数量={}]",
                folder.getId(), folder.getFileName(), allFoldersToDelete.size(), allFilesToDelete.size());
    }

    private void copyFolder(File file, File parentFile) {
        if (parentFile.getParentId() != null && parentFile.getParentId().equals(file.getId())) {
            throw new DataFormatException(
                    String.format("不能复制到子文件夹中 [sourceFolderId=%d, targetFolderId=%d]",
                            file.getId(), parentFile.getId()));
        }

        ensureUniqueFileName(parentFile.getId(), file.getFileName(), null);
        File newFile = File.builder()
                .type(FileType.FOLDER.toString()).parentId(parentFile.getId())
                .fileName(file.getFileName())
                .build();
        fileDataService.save(newFile);

        List<File> children = fileDataService.lambdaQuery().eq(File::getParentId, file.getId()).list();
        for (File f : children) {
            if (FileType.FOLDER.toString().equals(f.getType())) {
                copyFolder(f, newFile);
            } else {
                copyCommonFile(f, newFile);
            }
        }
    }

    public void copyCommonFile(File file, File parentFile) {
        log.debug("开始复制普通文件 [fileId={}, fileName={}, targetParentId={}]",
                file.getId(), file.getFileName(), parentFile.getId());

        if (file.getResourceId() == null) {
            // 文件没有物理资源，只复制元数据
            ensureUniqueFileName(parentFile.getId(), file.getFileName(), null);
            File newFile = File.builder()
                    .fileName(file.getFileName()).parentId(parentFile.getId())
                    .type(file.getType()).resourceId(null)
                    .size(file.getSize())
                    .build();
            fileDataService.save(newFile);
            return;
        }

        Resource resource = resourceDataService.getById(file.getResourceId());
        if (Objects.isNull(resource)) {
            throw new DatabaseException(
                    String.format("资源记录不存在 [fileId=%d, resourceId=%d]",
                            file.getId(), file.getResourceId()));
        }

        resourceDataService.lambdaUpdate()
                .eq(Resource::getId, resource.getId())
                .set(Resource::getLink, resource.getLink() + 1)
                .update();

        ensureUniqueFileName(parentFile.getId(), file.getFileName(), null);
        File newFile = File.builder()
                .fileName(file.getFileName()).parentId(parentFile.getId())
                .type(file.getType()).resourceId(file.getResourceId())
                .size(file.getSize())
                .build();
        fileDataService.save(newFile);
    }

    private Optional<File> findById(Long id) {
        if (id == 0) {
            return Optional.of(File.ROOT_FILE);
        } else {
            return Optional.ofNullable(fileDataService.getById(id));
        }
    }

    private void ensureUniqueFileName(Long parentId, String fileName, Long excludeId) {
        LambdaQueryWrapper<File> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(File::getParentId, parentId).eq(File::getFileName, fileName);
        if (excludeId != null) {
            wrapper.ne(File::getId, excludeId);
        }
        if (fileDataService.count(wrapper) > 0) {
            String suggestedName = generateUniqueFileName(parentId, fileName, excludeId);
            throw FileAlreadyExistsException.withSuggestion(parentId, fileName, suggestedName);
        }
    }

    private String generateUniqueFileName(Long parentId, String fileName, Long excludeId) {
        String baseName;
        String extension;
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = fileName.substring(0, dotIndex);
            extension = fileName.substring(dotIndex);
        } else {
            baseName = fileName;
            extension = "";
        }

        for (int i = 1; i <= 1000; i++) {
            String newName = baseName + " (" + i + ")" + extension;
            LambdaQueryWrapper<File> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(File::getParentId, parentId).eq(File::getFileName, newName);
            if (excludeId != null) {
                wrapper.ne(File::getId, excludeId);
            }
            if (fileDataService.count(wrapper) == 0) {
                return newName;
            }
        }

        return baseName + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
    }

    private int normalizeLimit(Integer limit) {
        return (limit == null || limit <= 0) ? DEFAULT_RECENT_LIMIT : Math.min(limit, MAX_RECENT_LIMIT);
    }

    private String normalizeSortBy(String sortBy) {
        if (Objects.isNull(sortBy) || sortBy.isBlank()) {
            return "createTime";
        }
        return sortBy;
    }

    private String normalizeSortOrder(String sortOrder) {
        return "desc".equalsIgnoreCase(sortOrder) ? "desc" : "asc";
    }

    private String normalizeCategory(String category) {
        if (Objects.isNull(category) || category.isBlank()) {
            throw new DataFormatException("分类不能为空");
        }
        String normalized = category.trim().toLowerCase();
        if (!List.of("picture", "video", "audio", "document", "compress", "other").contains(normalized)) {
            throw new DataFormatException(String.format("不支持的分类 [category=%s]", category));
        }
        return normalized;
    }

    private boolean matchesCategory(File file, String category) {
        return getCategoryForFileType(file).equals(category);
    }

    private String getCategoryForFileType(File file) {
        String type = file.getType();
        if (FileType.PICTURE.toString().equals(type)) return "picture";
        if (FileType.VIDEO.toString().equals(type)) return "video";
        if (FileType.AUDIO.toString().equals(type)) return "audio";
        if (DOCUMENT_TYPES.contains(type)) return "document";
        if (COMPRESS_TYPES.contains(type)) return "compress";
        return "other";
    }

    private String buildFolderPath(List<FolderPathDto> navigation) {
        return navigation.stream()
                .map(FolderPathDto::getFileName)
                .collect(Collectors.joining(" / "));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseDto uploadWithMD5(MultipartFile file, Long parentId) {
        long startTime = System.currentTimeMillis();
        log.info("开始上传文件（支持秒传）[fileName={}, fileSize={}, parentId={}]",
                file.getOriginalFilename(), file.getSize(), parentId);

        try {
            Objects.requireNonNull(file, "上传文件不能为空");
            Objects.requireNonNull(parentId, "父文件夹ID不能为空");

            String fileName = file.getOriginalFilename();
            FileUtils.checkFileName(fileName);

            findById(parentId)
                    .orElseThrow(() -> new DataFormatException("父文件夹不存在或不是文件夹类型"));
            ensureUniqueFileName(parentId, fileName, null);

            String md5;
            try (InputStream inputStream = file.getInputStream()) {
                md5 = MD5Utils.calculateMD5(inputStream);
            }

            if (md5 == null || md5.isEmpty()) {
                throw new FileOperationException("文件MD5计算失败");
            }

            Optional<Resource> existingResourceOpt = Optional.ofNullable(resourceDataService.lambdaQuery()
                    .eq(Resource::getMd5, md5)
                    .one());

            File newFile;
            boolean isInstantUpload;
            if (existingResourceOpt.isPresent()) {
                Resource existingResource = existingResourceOpt.get();
                log.info("发现相同MD5的已存在资源，执行秒传 [md5={}, resourceId={}]", md5, existingResource.getId());
                resourceDataService.lambdaUpdate()
                        .eq(Resource::getId, existingResource.getId())
                        .set(Resource::getLink, existingResource.getLink() + 1)
                        .update();

                newFile = File.builder()
                        .fileName(fileName)
                        .parentId(parentId)
                        .type(FileUtils.getFileType(fileName).toString())
                        .resourceId(existingResource.getId())
                        .size(file.getSize())
                        .build();
                fileDataService.save(newFile);
                isInstantUpload = true;
            } else {
                log.info("未发现相同MD5的资源，执行正常上传 [md5={}]", md5);
                Resource savedResource = saveUploadFile(file);
                savedResource.setMd5(md5);
                resourceDataService.updateById(savedResource);

                newFile = File.builder()
                        .fileName(fileName)
                        .parentId(parentId)
                        .type(FileUtils.getFileType(fileName).toString())
                        .resourceId(savedResource.getId())
                        .size(file.getSize())
                        .build();
                fileDataService.save(newFile);
                isInstantUpload = false;
            }

            Map<String, Object> resultData = Map.of(
                    "file", newFile,
                    "isInstantUpload", isInstantUpload,
                    "md5", md5
            );

            log.info("文件上传完成 [fileName={}, isInstantUpload={}, 总耗时={}ms]",
                    fileName, isInstantUpload, System.currentTimeMillis() - startTime);

            return ResponseDto.success(resultData);

        } catch (IOException e) {
            throw new FileOperationException("文件上传失败", e);
        }
    }

    @Override
    public ResponseDto checkFileByMD5(String md5) {
        if (md5 == null || md5.trim().isEmpty()) {
            throw new DataFormatException("MD5值不能为空");
        }
        String normalizedMd5 = md5.trim().toLowerCase();

        return Optional.ofNullable(resourceDataService.lambdaQuery().eq(Resource::getMd5, normalizedMd5).one())
                .map(resource -> {
                    log.info("发现相同MD5的资源 [md5={}, resourceId={}]", normalizedMd5, resource.getId());
                    return ResponseDto.success(Map.of(
                            "exists", true,
                            "resource", resource,
                            "message", "文件已存在，可以秒传"
                    ));
                })
                .orElseGet(() -> {
                    log.debug("未发现相同MD5的资源 [md5={}]", normalizedMd5);
                    return ResponseDto.success(Map.of(
                            "exists", false,
                            "message", "文件不存在，需要正常上传"
                    ));
                });
    }

    @Override
    public ResponseDto verifyFileMD5(Long fileId) {
        Objects.requireNonNull(fileId, "文件ID不能为空");

        File file = findById(fileId)
                .orElseThrow(() -> new DataNotFoundException(String.format("文件不存在 [fileId=%d]", fileId)));

        if (Objects.isNull(file.getResourceId())) {
            throw new DataFormatException("文件没有关联资源，无法验证");
        }

        Resource resource = resourceDataService.getById(file.getResourceId());
        if (Objects.isNull(resource) || Objects.isNull(resource.getMd5())) {
            return ResponseDto.success(Map.of("valid", false, "message", "资源没有MD5值，无法验证"));
        }

        Path filePath = Paths.get(homeDashProperties.getBasicResourcePath(), resource.getPath());
        if (!Files.exists(filePath)) {
            return ResponseDto.success(Map.of("valid", false, "error", "物理文件不存在"));
        }

        long startTime = System.currentTimeMillis();
        String actualMD5 = MD5Utils.calculateMD5(filePath);
        long duration = System.currentTimeMillis() - startTime;

        boolean isValid = resource.getMd5().equalsIgnoreCase(actualMD5);
        log.info("MD5验证完成 [fileId={}, valid={}, 耗时={}ms]", fileId, isValid, duration);

        return ResponseDto.success(Map.of(
                "valid", isValid,
                "expected", resource.getMd5(),
                "actual", String.valueOf(actualMD5),
                "fileName", file.getFileName(),
                "verifyTimeMs", duration
        ));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseDto instantUpload(String md5, String fileName, Long parentId) {
        long startTime = System.currentTimeMillis();
        log.info("开始执行秒传 [md5={}, fileName={}, parentId={}]", md5, fileName, parentId);

        Objects.requireNonNull(md5, "MD5值不能为空");
        Objects.requireNonNull(fileName, "文件名不能为空");
        Objects.requireNonNull(parentId, "父文件夹ID不能为空");

        FileUtils.checkFileName(fileName);
        ensureUniqueFileName(parentId, fileName, null);

        Resource existingResource = Optional.ofNullable(resourceDataService.lambdaQuery().eq(Resource::getMd5, md5.toLowerCase()).one())
                .orElseThrow(() -> new DataNotFoundException(String.format("未找到MD5为[%s]的资源", md5)));

        resourceDataService.lambdaUpdate()
                .eq(Resource::getId, existingResource.getId())
                .set(Resource::getLink, existingResource.getLink() + 1)
                .update();

        File newFile = File.builder()
                .fileName(fileName)
                .parentId(parentId)
                .type(FileUtils.getFileType(fileName).toString())
                .resourceId(existingResource.getId())
                .size(existingResource.getSize())
                .build();
        fileDataService.save(newFile);

        log.info("秒传成功 [newFileId={}, resourceId={}, 耗时={}ms]",
                newFile.getId(), existingResource.getId(), System.currentTimeMillis() - startTime);

        return ResponseDto.success(Map.of(
                "file", newFile,
                "isInstantUpload", true,
                "md5", md5
        ));
    }

    private Resource saveUploadFile(MultipartFile file) throws IOException {
        String relativePath = generateStoragePath(file.getOriginalFilename());
        Path fullPath = Paths.get(homeDashProperties.getBasicResourcePath(), relativePath);

        Files.createDirectories(fullPath.getParent());
        Files.copy(file.getInputStream(), fullPath);

        Resource resource = Resource.builder()
                .size(file.getSize())
                .path(relativePath)
                .link(1)
                .build();
        resourceDataService.save(resource);
        return resource;
    }

    private String generateStoragePath(String originalName) {
        LocalDateTime now = LocalDateTime.now();
        String extension = FileUtils.extractFileExtensionName(originalName);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return String.format("%04d/%02d/%s%s", now.getYear(), now.getMonthValue(), uuid, extension);
    }

    private List<File> queryByTimeRange(LocalDateTime start, LocalDateTime end) {
        return fileDataService.lambdaQuery()
                .between(File::getCreateTime, start, end)
                .orderByDesc(File::getCreateTime)
                .list();
    }

    private SFunction<File, ?> getSortColumn(String sortBy) {
        return switch (normalizeSortBy(sortBy).toLowerCase()) {
            case "filename", "name" -> File::getFileName;
            case "size" -> File::getSize;
            case "updatetime" -> File::getUpdateTime;
            case "type" -> File::getType;
            default -> File::getCreateTime;
        };
    }

    private boolean isDescendant(Long targetId, Long sourceId) {
        Long currentId = targetId;
        int maxDepth = 1000;
        int depth = 0;
        while (currentId != null && !currentId.equals(File.ROOT_FILE.getId()) && depth < maxDepth) {
            if (currentId.equals(sourceId)) {
                return true;
            }
            currentId = findById(currentId).map(File::getParentId).orElse(null);
            depth++;
        }
        return false;
    }

    private void batchCopyCommonFiles(List<File> files, File parentFile) {
        log.debug("开始批量复制普通文件 [文件数量={}, 目标父文件夹ID={}]", files.size(), parentFile.getId());

        Set<String> targetNames = fileDataService.lambdaQuery()
                .eq(File::getParentId, parentFile.getId())
                .list().stream()
                .map(File::getFileName)
                .collect(Collectors.toSet());

        for (File file : files) {
            if (targetNames.contains(file.getFileName())) {
                throw new FileAlreadyExistsException(
                        String.format("目标目录已存在同名文件 [fileName=%s]", file.getFileName()));
            }
        }

        Map<Long, Long> copyCountByResourceId = files.stream()
                .filter(f -> f.getResourceId() != null)
                .collect(Collectors.groupingBy(File::getResourceId, Collectors.counting()));

        if (!copyCountByResourceId.isEmpty()) {
            resourceDataService.listByIds(copyCountByResourceId.keySet()).forEach(resource ->
                    resourceDataService.lambdaUpdate()
                            .eq(Resource::getId, resource.getId())
                            .set(Resource::getLink, resource.getLink() + copyCountByResourceId.get(resource.getId()).intValue())
                            .update()
            );
        }

        List<File> newFiles = files.stream()
                .map(file -> File.builder()
                        .fileName(file.getFileName())
                        .parentId(parentFile.getId())
                        .type(file.getType())
                        .resourceId(file.getResourceId())
                        .size(file.getSize())
                        .build())
                .toList();

        fileDataService.saveBatch(newFiles);
    }
}