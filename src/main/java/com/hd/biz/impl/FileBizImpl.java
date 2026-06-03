package com.hd.biz.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.hd.biz.FileBiz;
import com.hd.common.config.HomeDashProperties;
import com.hd.common.enums.ErrorCodeEnum;
import com.hd.common.enums.FileTypeEnum;
import com.hd.common.enums.FileCategoryEnum;
import com.hd.common.exception.*;
import com.hd.common.util.FileUtils;
import com.hd.common.util.MD5Utils;
import com.hd.dao.entity.File;
import com.hd.dao.entity.Favorite;
import com.hd.dao.entity.FileRemark;
import com.hd.dao.entity.FileTagRelation;
import com.hd.dao.entity.Resource;
import com.hd.dao.service.FavoriteDataService;
import com.hd.dao.service.FileDataService;
import com.hd.dao.service.FileRemarkDataService;
import com.hd.dao.service.FileTagRelationDataService;
import com.hd.dao.service.ResourceDataService;
import com.hd.biz.RecycleBinBiz;
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
    private static final List<String> SYSTEM_INITIALIZED_FOLDERS = List.of("图片", "视频", "音频", "文档", "压缩包", "其他");
    private static final List<String> DOCUMENT_TYPES = List.of(
            FileTypeEnum.PDF.toString(),
            FileTypeEnum.DOC.toString(),
            FileTypeEnum.TXT.toString(),
            FileTypeEnum.PPT.toString(),
            FileTypeEnum.CODE.toString(),
            FileTypeEnum.WEB.toString());
    private static final List<String> COMPRESS_TYPES = List.of(FileTypeEnum.COMPRESS_FILE.toString());

    private final FileDataService fileDataService;
    private final ResourceDataService resourceDataService;
    private final FavoriteDataService favoriteDataService;
    private final FileTagRelationDataService fileTagRelationDataService;
    private final FileRemarkDataService fileRemarkDataService;
    private final HomeDashProperties homeDashProperties;

    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    private RecycleBinBiz recycleBinBiz;

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
    public ResponseDTO findByParentId(Long parentId, String sortBy, String sortOrder) {
        boolean isAsc = "asc".equalsIgnoreCase(normalizeSortOrder(sortOrder));
        List<File> files = fileDataService.lambdaQuery()
                .eq(File::getParentId, parentId)
                .orderBy(true, isAsc, getSortColumn(sortBy))
                .list();
        fillFolderSizes(files);
        List<FolderPathDTO> folderPaths = getFolderTree(parentId);
        return ResponseDTO.success(files, folderPaths);
    }

    @Override
    public ResponseDTO getFolderSize(Long folderId) {
        File folder = findById(folderId)
                .orElseThrow(() -> new DataNotFoundException(String.format("文件夹不存在 [folderId=%d]", folderId)));
        if (!FileTypeEnum.FOLDER.toString().equals(folder.getType())) {
            throw new DataFormatException(String.format("目标不是文件夹 [folderId=%d]", folderId));
        }
        return ResponseDTO.success(Map.of("size", calculateFolderSize(folderId)));
    }

    @Override
    public ResponseDTO findRecentFiles(Integer limit) {
        int safeLimit = normalizeLimit(limit);
        LambdaQueryWrapper<File> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.ne(File::getType, FileTypeEnum.FOLDER.toString())
                .or(sub -> sub.eq(File::getType, FileTypeEnum.FOLDER.toString())
                        .and(cond -> cond.ne(File::getParentId, File.ROOT_FILE.getId())
                                .or().notIn(File::getFileName, SYSTEM_INITIALIZED_FOLDERS)))
        );
        wrapper.orderByDesc(File::getCreateTime);
        wrapper.last("LIMIT " + safeLimit);
        List<File> recentFiles = fileDataService.list(wrapper);
        return ResponseDTO.success(recentFiles, Map.of("limit", safeLimit));
    }

    @Override
    public ResponseDTO getRecentUploadSummary(Integer limit) {
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

        LambdaQueryWrapper<File> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.ne(File::getType, FileTypeEnum.FOLDER.toString())
                .or(sub -> sub.eq(File::getType, FileTypeEnum.FOLDER.toString())
                        .and(cond -> cond.ne(File::getParentId, File.ROOT_FILE.getId())
                                .or().notIn(File::getFileName, SYSTEM_INITIALIZED_FOLDERS)))
        );
        wrapper.orderByDesc(File::getCreateTime);
        wrapper.last("LIMIT " + safeLimit);
        List<File> recentFiles = fileDataService.list(wrapper);

        List<RecentUploadSummaryDTO.File> recentFileList = recentFiles.stream()
                .map(f -> RecentUploadSummaryDTO.File.builder()
                        .id(f.getId())
                        .fileName(f.getFileName())
                        .type(f.getType())
                        .size(f.getSize())
                        .createTime(f.getCreateTime())
                        .parentId(f.getParentId())
                        .build())
                .toList();

        RecentUploadSummaryDTO summary = RecentUploadSummaryDTO.builder()
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

        return ResponseDTO.success(summary);
    }

    @Override
    public ResponseDTO findFilesByCategory(String category, String sortBy, String sortOrder) {
        String normalizedCategory = normalizeCategory(category);
        boolean isAsc = "asc".equalsIgnoreCase(normalizeSortOrder(sortOrder));

        LambdaQueryWrapper<File> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(File::getType, FileTypeEnum.FOLDER.toString());
        switch (normalizedCategory) {
            case "picture":
                wrapper.eq(File::getType, FileTypeEnum.PICTURE.toString());
                break;
            case "video":
                wrapper.eq(File::getType, FileTypeEnum.VIDEO.toString());
                break;
            case "audio":
                wrapper.eq(File::getType, FileTypeEnum.AUDIO.toString());
                break;
            case "document":
                wrapper.in(File::getType, DOCUMENT_TYPES);
                break;
            case "compress":
                wrapper.in(File::getType, COMPRESS_TYPES);
                break;
            case "other":
                List<String> knownTypes = new ArrayList<>();
                knownTypes.add(FileTypeEnum.FOLDER.toString());
                knownTypes.add(FileTypeEnum.PICTURE.toString());
                knownTypes.add(FileTypeEnum.VIDEO.toString());
                knownTypes.add(FileTypeEnum.AUDIO.toString());
                knownTypes.addAll(DOCUMENT_TYPES);
                knownTypes.addAll(COMPRESS_TYPES);
                wrapper.notIn(File::getType, knownTypes);
                break;
            default:
                break;
        }
        wrapper.orderBy(true, isAsc, getSortColumn(sortBy));
        List<File> files = fileDataService.list(wrapper);
        return ResponseDTO.success(files, Map.of(
                "category", normalizedCategory,
                "count", files.size()));
    }

    @Override
    public ResponseDTO categorySummary() {
        long pictureCount = fileDataService.count(
                new LambdaQueryWrapper<File>().eq(File::getType, FileTypeEnum.PICTURE.toString())
        );
        long videoCount = fileDataService.count(
                new LambdaQueryWrapper<File>().eq(File::getType, FileTypeEnum.VIDEO.toString())
        );
        long audioCount = fileDataService.count(
                new LambdaQueryWrapper<File>().eq(File::getType, FileTypeEnum.AUDIO.toString())
        );
        long docCount = fileDataService.count(
                new LambdaQueryWrapper<File>().in(File::getType, DOCUMENT_TYPES)
        );
        long compressCount = fileDataService.count(
                new LambdaQueryWrapper<File>().in(File::getType, COMPRESS_TYPES)
        );

        List<String> knownTypes = new ArrayList<>();
        knownTypes.add(FileTypeEnum.FOLDER.toString());
        knownTypes.add(FileTypeEnum.PICTURE.toString());
        knownTypes.add(FileTypeEnum.VIDEO.toString());
        knownTypes.add(FileTypeEnum.AUDIO.toString());
        knownTypes.addAll(DOCUMENT_TYPES);
        knownTypes.addAll(COMPRESS_TYPES);
        long otherCount = fileDataService.count(
                new LambdaQueryWrapper<File>().notIn(File::getType, knownTypes)
        );

        List<FileCategorySummaryDTO> summaries = List.of(
                new FileCategorySummaryDTO(FileCategoryEnum.PICTURE.getCode(), FileCategoryEnum.PICTURE.getName(), (int) pictureCount),
                new FileCategorySummaryDTO(FileCategoryEnum.VIDEO.getCode(), FileCategoryEnum.VIDEO.getName(), (int) videoCount),
                new FileCategorySummaryDTO(FileCategoryEnum.AUDIO.getCode(), FileCategoryEnum.AUDIO.getName(), (int) audioCount),
                new FileCategorySummaryDTO(FileCategoryEnum.DOCUMENT.getCode(), FileCategoryEnum.DOCUMENT.getName(), (int) docCount),
                new FileCategorySummaryDTO(FileCategoryEnum.COMPRESS.getCode(), FileCategoryEnum.COMPRESS.getName(), (int) compressCount),
                new FileCategorySummaryDTO(FileCategoryEnum.OTHER.getCode(), FileCategoryEnum.OTHER.getName(), (int) otherCount));
        return ResponseDTO.success(summaries);
    }

    /**
     * 根据文件ID获取文件详情。
     *
     * @param fileId 文件ID
     * @return 包含文件详情的响应对象
     * @throws DataNotFoundException 当文件不存在时抛出
     */
    @Override
    public ResponseDTO findByFileId(Long fileId) {
        File file = findById(fileId)
                .orElseThrow(() -> new DataNotFoundException(String.format("文件不存在 [fileId=%d]", fileId)));

        List<FolderPathDTO> navigation = FileTypeEnum.FOLDER.toString().equals(file.getType())
                ? getFolderTree(file.getId())
                : getFolderTree(file.getParentId());

        FileDetailDTO detail = FileDetailDTO.builder()
                .id(file.getId())
                .parentId(file.getParentId())
                .resourceId(file.getResourceId())
                .size(file.getSize())
                .fileName(file.getFileName())
                .type(file.getType())
                .extension(FileUtils.extractFileExtensionName(file.getFileName()))
                .folderPath(buildFolderPath(navigation))
                .downloadable(!FileTypeEnum.FOLDER.toString().equals(file.getType()))
                .playable(FileTypeEnum.VIDEO.toString().equals(file.getType()))
                .createTime(file.getCreateTime())
                .updateTime(file.getUpdateTime())
                .navigation(navigation)
                .build();

        return ResponseDTO.success(detail);
    }

    /**
     * 创建新文件或文件夹。
     * 验证文件名和类型，并检查父文件夹是否存在。
     *
     * @param file 文件信息对象
     * @throws DataFormatException 当文件名或类型不合法，或父文件夹不存在时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void createFile(CreateFileDTO dto) {
        log.info("开始创建文件 [fileName={}, parentId={}, type={}]",
                dto.getFileName(), dto.getParentId(), dto.getType());

        FileUtils.checkFileName(dto.getFileName());
        FileUtils.checkFileType(dto.getType());
        File parentFile = findById(dto.getParentId())
                .orElseThrow(() -> new DataFormatException(
                        String.format("父文件夹不存在 [parentId=%d]", dto.getParentId())));

        if (!FileTypeEnum.FOLDER.toString().equals(parentFile.getType())) {
            log.error("父文件夹不是文件夹类型 [parentId={}, fileName={}]",
                    dto.getParentId(), dto.getFileName());
            throw new DataFormatException(
                    String.format("父文件夹不是文件夹类型 [parentId=%d, fileName=%s]",
                            dto.getParentId(), dto.getFileName()));
        }

        ensureUniqueFileName(dto.getParentId(), dto.getFileName(), null);
        
        File file = File.builder()
                .parentId(dto.getParentId())
                .fileName(dto.getFileName())
                .type(dto.getType())
                .build();
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
    @Transactional(rollbackFor = Exception.class)
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
        if (!FileTypeEnum.FOLDER.toString().equals(targetFolder.getType())) {
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
            if (FileTypeEnum.FOLDER.toString().equals(file.getType()) && isDescendant(targetId, file.getId())) {
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
            if (!FileTypeEnum.FOLDER.toString().equals(targetFile.getType())) {
                throw new DataFormatException(
                        String.format("目标不是文件夹类型 [targetId=%d]", targetId));
            }

            Map<Boolean, List<File>> fileGroups = filesToCopy.stream()
                    .collect(Collectors.partitioningBy(
                            file -> FileTypeEnum.FOLDER.toString().equals(file.getType())));

            List<File> folders = fileGroups.get(true);
            List<File> commonFiles = fileGroups.get(false);

            if (!commonFiles.isEmpty()) {
                batchCopyCommonFiles(commonFiles, targetFile);
            }

            for (File folder : folders) {
                if (isDescendant(targetId, folder.getId())) {
                    throw new BusinessException("不能将文件夹复制到自身或其子文件夹中");
                }
                copyFolder(folder, targetFile);
            }
        }

        log.info("文件复制成功 [fileIds={}, targetIds={}, 总耗时={}ms]",
                fileIds, targetIds, System.currentTimeMillis() - startTime);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteFiles(List<Long> ids) {
        log.info("开始逻辑删除文件并移入回收站 [fileIds={}]", ids);
        Objects.requireNonNull(ids, "要逻辑删除的文件ID列表不能为空");
        recycleBinBiz.softDelete(ids);
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

    private List<FolderPathDTO> getFolderTree(Long parentId) {
        LinkedList<FolderPathDTO> path = new LinkedList<>();
        Long currentId = parentId;

        while (currentId != null && !currentId.equals(File.ROOT_FILE.getId())) {
            final Long idToFind = currentId;
            File currentFile = findById(idToFind)
                    .orElseThrow(() -> new DataNotFoundException(String.format("Folder with ID %d not found.", idToFind)));

            if (!FileTypeEnum.FOLDER.toString().equals(currentFile.getType())) {
                throw new DataFormatException(String.format("ID %d is not a folder.", currentId));
            }
            path.addFirst(new FolderPathDTO(currentFile.getId(), currentFile.getFileName()));
            currentId = currentFile.getParentId();
        }

        path.addFirst(new FolderPathDTO(File.ROOT_FILE.getId(), File.ROOT_FILE.getFileName()));
        return path;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void permanentlyDelete(List<Long> ids) {
        long startTime = System.currentTimeMillis();
        log.info("开始物理彻底删除文件 [fileIds={}, fileCount={}]", ids, ids != null ? ids.size() : 0);

        Objects.requireNonNull(ids, "要删除的文件ID列表不能为空");

        // Clean up related records (favorites, tags, remarks) before permanent deletion
        cleanupRelatedRecords(ids);

        List<File> filesToDelete = new ArrayList<>();
        for (Long id : ids) {
            File f = fileDataService.selectByIdWithDeleted(id);
            if (f != null) {
                filesToDelete.add(f);
            }
        }

        Map<Boolean, List<File>> fileGroups = filesToDelete.stream()
                .collect(Collectors.partitioningBy(
                        file -> FileTypeEnum.FOLDER.toString().equals(file.getType())));

        List<File> folders = fileGroups.get(true);
        List<File> commonFiles = fileGroups.get(false);

        if (!commonFiles.isEmpty()) {
            batchPermanentlyDeleteCommonFiles(commonFiles);
        }

        for (File folder : folders) {
            permanentlyDeleteFolderIterative(folder);
        }

        log.info("彻底物理删除文件成功 [fileIds={}, 总耗时={}ms]", ids, System.currentTimeMillis() - startTime);
    }

    private void batchPermanentlyDeleteCommonFiles(List<File> files) {
        log.debug("开始批量物理彻底删除普通文件 [文件数量={}]", files.size());

        List<Long> fileIdsToDelete = files.stream().map(File::getId).toList();
        Map<Long, Long> deleteCountByResourceId = files.stream()
                .filter(f -> f.getResourceId() != null)
                .collect(Collectors.groupingBy(File::getResourceId, Collectors.counting()));

        if (deleteCountByResourceId.isEmpty()) {
            if (!fileIdsToDelete.isEmpty()) {
                for (Long fileId : fileIdsToDelete) {
                    fileDataService.permanentlyDelete(fileId);
                }
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
                resourcesToUpdate.add(resource);
            }
        }

        if (!physicalFilesToDelete.isEmpty()) {
            physicalFilesToDelete.forEach(resource -> {
                try {
                    Files.deleteIfExists(FileUtils.resolveSecurePath(homeDashProperties.getBasicResourcePath(), resource.getPath()));
                } catch (IOException e) {
                    log.error("物理文件删除失败 [resourceId={}, resourcePath={}]",
                            resource.getId(), resource.getPath(), e);
                }
            });
            resourceDataService.removeByIds(resourceIdsToDelete);
        }

        if (!resourcesToUpdate.isEmpty()) {
            for (Resource resource : resourcesToUpdate) {
                long count = deleteCountByResourceId.getOrDefault(resource.getId(), 0L);
                resourceDataService.lambdaUpdate()
                        .eq(Resource::getId, resource.getId())
                        .setSql("link = GREATEST(link - " + count + ", 0)")
                        .update();
            }
        }

        if (!fileIdsToDelete.isEmpty()) {
            for (Long fileId : fileIdsToDelete) {
                fileDataService.permanentlyDelete(fileId);
            }
        }
    }

    private void permanentlyDeleteFolderIterative(File folder) {
        log.debug("开始迭代物理彻底删除文件夹 [folderId={}, folderName={}]", folder.getId(), folder.getFileName());

        Stack<File> folderStack = new Stack<>();
        folderStack.push(folder);

        List<File> allFilesToDelete = new ArrayList<>();
        List<File> allFoldersToDelete = new ArrayList<>();
        allFoldersToDelete.add(folder);

        while (!folderStack.isEmpty()) {
            File currentFolder = folderStack.pop();
            List<File> children = fileDataService.selectAllChildrenByParentId(currentFolder.getId());

            for (File child : children) {
                if (FileTypeEnum.FOLDER.toString().equals(child.getType())) {
                    folderStack.push(child);
                    allFoldersToDelete.add(child);
                } else {
                    allFilesToDelete.add(child);
                }
            }
        }

        if (!allFilesToDelete.isEmpty()) {
            batchPermanentlyDeleteCommonFiles(allFilesToDelete);
        }

        Collections.reverse(allFoldersToDelete);
        for (File f : allFoldersToDelete) {
            fileDataService.permanentlyDelete(f.getId());
        }

        log.info("迭代物理彻底删除文件夹成功 [folderId={}, folderName={}, 删除文件夹数量={}, 删除文件数量={}]",
                folder.getId(), folder.getFileName(), allFoldersToDelete.size(), allFilesToDelete.size());
    }

    /**
     * 清理文件关联的收藏、标签关联和备注记录，防止删除后产生孤立数据。
     *
     * @param fileIds 要清理关联记录的文件ID列表
     */
    private void cleanupRelatedRecords(List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }
        for (Long fileId : fileIds) {
            File file = fileDataService.selectByIdWithDeleted(fileId);
            if (file == null) {
                continue;
            }
            // Remove tag relations by fileId
            fileTagRelationDataService.remove(
                    new LambdaQueryWrapper<FileTagRelation>().eq(FileTagRelation::getFileId, fileId));
            // Remove favorites and remarks by resourceId
            if (file.getResourceId() != null) {
                favoriteDataService.remove(
                        new LambdaQueryWrapper<Favorite>().eq(Favorite::getResourceId, file.getResourceId()));
                fileRemarkDataService.remove(
                        new LambdaQueryWrapper<FileRemark>().eq(FileRemark::getResourceId, file.getResourceId()));
            }
        }
    }

    private void copyFolder(File file, File parentFile) {
        if (isDescendant(parentFile.getId(), file.getId())) {
            throw new DataFormatException(
                    String.format("不能复制到子文件夹中 [sourceFolderId=%d, targetFolderId=%d]",
                            file.getId(), parentFile.getId()));
        }

        ensureUniqueFileName(parentFile.getId(), file.getFileName(), null);
        File newFile = File.builder()
                .type(FileTypeEnum.FOLDER.toString()).parentId(parentFile.getId())
                .fileName(file.getFileName())
                .build();
        fileDataService.save(newFile);

        List<File> children = fileDataService.lambdaQuery().eq(File::getParentId, file.getId()).list();
        for (File f : children) {
            if (FileTypeEnum.FOLDER.toString().equals(f.getType())) {
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
                .setSql("link = link + 1")
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
        FileCategoryEnum fileCategory = FileCategoryEnum.fromCode(normalized);
        if (fileCategory == FileCategoryEnum.OTHER && !"other".equalsIgnoreCase(normalized)) {
            throw new DataFormatException(String.format("不支持的分类 [category=%s]", category));
        }
        return fileCategory.getCode();
    }

    private boolean matchesCategory(File file, String category) {
        return getCategoryForFileType(file).equals(category);
    }

    private String getCategoryForFileType(File file) {
        String type = file.getType();
        if (FileTypeEnum.PICTURE.toString().equals(type)) return FileCategoryEnum.PICTURE.getCode();
        if (FileTypeEnum.VIDEO.toString().equals(type)) return FileCategoryEnum.VIDEO.getCode();
        if (FileTypeEnum.AUDIO.toString().equals(type)) return FileCategoryEnum.AUDIO.getCode();
        if (DOCUMENT_TYPES.contains(type)) return FileCategoryEnum.DOCUMENT.getCode();
        if (COMPRESS_TYPES.contains(type)) return FileCategoryEnum.COMPRESS.getCode();
        return FileCategoryEnum.OTHER.getCode();
    }

    private String buildFolderPath(List<FolderPathDTO> navigation) {
        return navigation.stream()
                .map(FolderPathDTO::getFileName)
                .collect(Collectors.joining(" / "));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO uploadWithMD5(MultipartFile file, Long parentId) {
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
                        .setSql("link = link + 1")
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

            return ResponseDTO.success(resultData);

        } catch (IOException e) {
            throw new FileOperationException("文件上传失败", e);
        }
    }

    @Override
    public ResponseDTO checkFileByMD5(String md5) {
        if (md5 == null || md5.trim().isEmpty()) {
            throw new DataFormatException("MD5值不能为空");
        }
        String normalizedMd5 = md5.trim().toLowerCase();

        return Optional.ofNullable(resourceDataService.lambdaQuery().eq(Resource::getMd5, normalizedMd5).one())
                .map(resource -> {
                    log.info("发现相同MD5的资源 [md5={}, resourceId={}]", normalizedMd5, resource.getId());
                    return ResponseDTO.success(Map.of(
                            "exists", true,
                            "resource", resource,
                            "message", "文件已存在，可以秒传"
                    ));
                })
                .orElseGet(() -> {
                    log.debug("未发现相同MD5的资源 [md5={}]", normalizedMd5);
                    return ResponseDTO.success(Map.of(
                            "exists", false,
                            "message", "文件不存在，需要正常上传"
                    ));
                });
    }

    @Override
    public ResponseDTO verifyFileMD5(Long fileId) {
        Objects.requireNonNull(fileId, "文件ID不能为空");

        File file = findById(fileId)
                .orElseThrow(() -> new DataNotFoundException(String.format("文件不存在 [fileId=%d]", fileId)));

        if (Objects.isNull(file.getResourceId())) {
            throw new DataFormatException("文件没有关联资源，无法验证");
        }

        Resource resource = resourceDataService.getById(file.getResourceId());
        if (Objects.isNull(resource) || Objects.isNull(resource.getMd5())) {
            return ResponseDTO.success(Map.of("valid", false, "message", "资源没有MD5值，无法验证"));
        }

        Path filePath = FileUtils.resolveSecurePath(homeDashProperties.getBasicResourcePath(), resource.getPath());
        if (!Files.exists(filePath)) {
            return ResponseDTO.success(Map.of("valid", false, "error", "物理文件不存在"));
        }

        long startTime = System.currentTimeMillis();
        String actualMD5 = MD5Utils.calculateMD5(filePath);
        long duration = System.currentTimeMillis() - startTime;

        boolean isValid = resource.getMd5().equalsIgnoreCase(actualMD5);
        log.info("MD5验证完成 [fileId={}, valid={}, 耗时={}ms]", fileId, isValid, duration);

        return ResponseDTO.success(Map.of(
                "valid", isValid,
                "expected", resource.getMd5(),
                "actual", String.valueOf(actualMD5),
                "fileName", file.getFileName(),
                "verifyTimeMs", duration
        ));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO instantUpload(String md5, String fileName, Long parentId) {
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
                .setSql("link = link + 1")
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

        return ResponseDTO.success(Map.of(
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
        LambdaQueryWrapper<File> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(File::getCreateTime, start, end)
                .and(w -> w.ne(File::getType, FileTypeEnum.FOLDER.toString())
                        .or(sub -> sub.eq(File::getType, FileTypeEnum.FOLDER.toString())
                                .and(cond -> cond.ne(File::getParentId, File.ROOT_FILE.getId())
                                        .or().notIn(File::getFileName, SYSTEM_INITIALIZED_FOLDERS)))
                )
                .orderByDesc(File::getCreateTime);
        return fileDataService.list(wrapper);
    }

    private boolean isRecentUploadCandidate(File file) {
        if (file == null) {
            return false;
        }
        return !(FileTypeEnum.FOLDER.toString().equals(file.getType())
                && Objects.equals(file.getParentId(), File.ROOT_FILE.getId())
                && SYSTEM_INITIALIZED_FOLDERS.contains(file.getFileName()));
    }

    private void fillFolderSizeIfNecessary(File file) {
        if (file != null && FileTypeEnum.FOLDER.toString().equals(file.getType())) {
            file.setFolderSize(calculateFolderSize(file.getId()));
        }
    }

    private void fillFolderSizes(List<File> files) {
        if (files == null || files.isEmpty()) {
            return;
        }
        List<File> folders = files.stream()
                .filter(f -> f != null && FileTypeEnum.FOLDER.toString().equals(f.getType()))
                .toList();
        if (folders.isEmpty()) {
            return;
        }

        Map<Long, Long> childToRootMap = new HashMap<>();
        Map<Long, Long> rootSizes = new HashMap<>();
        List<Long> currentLevelFolderIds = new ArrayList<>();

        for (File f : folders) {
            currentLevelFolderIds.add(f.getId());
            childToRootMap.put(f.getId(), f.getId());
            rootSizes.put(f.getId(), 0L);
        }

        while (!currentLevelFolderIds.isEmpty()) {
            List<File> children = fileDataService.lambdaQuery()
                    .in(File::getParentId, currentLevelFolderIds)
                    .eq(File::getIsDeleted, 0)
                    .list();

            if (children.isEmpty()) {
                break;
            }

            List<Long> nextLevelFolderIds = new ArrayList<>();
            for (File child : children) {
                Long parentId = child.getParentId();
                Long rootId = childToRootMap.get(parentId);
                if (rootId == null) {
                    continue;
                }

                if (FileTypeEnum.FOLDER.toString().equals(child.getType())) {
                    nextLevelFolderIds.add(child.getId());
                    childToRootMap.put(child.getId(), rootId);
                } else {
                    long size = child.getSize() != null ? child.getSize() : 0L;
                    rootSizes.put(rootId, rootSizes.get(rootId) + size);
                }
            }
            currentLevelFolderIds = nextLevelFolderIds;
        }

        for (File f : folders) {
            f.setFolderSize(rootSizes.getOrDefault(f.getId(), 0L));
        }
    }

    private long calculateFolderSize(Long folderId) {
        if (folderId == null) {
            return 0L;
        }
        long totalSize = 0L;
        List<Long> currentLevelFolderIds = new ArrayList<>();
        currentLevelFolderIds.add(folderId);

        while (!currentLevelFolderIds.isEmpty()) {
            List<File> children = fileDataService.lambdaQuery()
                    .in(File::getParentId, currentLevelFolderIds)
                    .eq(File::getIsDeleted, 0)
                    .list();

            if (children.isEmpty()) {
                break;
            }

            List<Long> nextLevelFolderIds = new ArrayList<>();
            for (File child : children) {
                if (FileTypeEnum.FOLDER.toString().equals(child.getType())) {
                    nextLevelFolderIds.add(child.getId());
                } else {
                    totalSize += child.getSize() != null ? child.getSize() : 0L;
                }
            }
            currentLevelFolderIds = nextLevelFolderIds;
        }
        return totalSize;
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

    @Override
    public String getTextFileContent(Long fileId) {
        log.info("获取文本文件内容 [fileId={}]", fileId);
        File file = findById(fileId)
                .orElseThrow(() -> new BusinessException(ErrorCodeEnum.FILE_NOT_FOUND));

        if (file.getResourceId() == null) {
            throw new BusinessException(ErrorCodeEnum.RESOURCE_NOT_FOUND);
        }

        Resource resource = resourceDataService.getById(file.getResourceId());
        if (resource == null) {
            throw new BusinessException(ErrorCodeEnum.RESOURCE_NOT_FOUND);
        }

        try {
            Path filePath = FileUtils.resolveSecurePath(homeDashProperties.getBasicResourcePath(), resource.getPath());
            return Files.readString(filePath);
        } catch (IOException e) {
            log.error("读取文本文件内容失败 [fileId={}]", fileId, e);
            throw new FileOperationException("读取文件内容失败", e);
        }
    }

    @Override
    public String getAudioFileUrl(Long fileId) {
        log.info("获取音频文件播放URL [fileId={}]", fileId);
        File file = findById(fileId)
                .orElseThrow(() -> new BusinessException(ErrorCodeEnum.FILE_NOT_FOUND));

        if (file.getResourceId() == null) {
            throw new BusinessException(ErrorCodeEnum.RESOURCE_NOT_FOUND);
        }

        Resource resource = resourceDataService.getById(file.getResourceId());
        if (resource == null) {
            throw new BusinessException(ErrorCodeEnum.RESOURCE_NOT_FOUND);
        }

        return "/v1/resource/" + resource.getId() + "/download";
    }
}