package com.hd.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.hd.config.FileType;
import com.hd.config.PndProperties;
import com.hd.controller.dto.FileCategorySummaryDto;
import com.hd.controller.dto.FileDetailDto;
import com.hd.controller.dto.FolderPathDto;
import com.hd.controller.dto.RecentUploadSummaryDto;
import com.hd.controller.dto.ResponseDto;
import com.hd.dao.FileMapper;
import com.hd.dao.ResourceMapper;
import com.hd.entity.File;
import com.hd.entity.Resource;
import com.hd.exception.DataFormatException;
import com.hd.exception.DataNotFoundException;
import com.hd.exception.DatabaseException;
import com.hd.exception.FileAlreadyExistsException;
import com.hd.exception.FileOperationException;
import com.hd.service.FileService;
import com.hd.util.FileUtils;
import com.hd.util.MD5Utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 文件服务实现类。
 * 实现文件管理的核心业务逻辑，包括文件的增删改查、移动、复制、下载等功能。
 * 在多文件和文件夹删除/复制数据时使用悲观锁，可能会出现死锁，
 * 但考虑到个人部署使用，系统并发量并不大，出现死锁的概率很低。
 *
 * @author john
 * @date 2020-01-11
 */
@Slf4j
@Service
public class FileServiceImpl implements FileService {

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
    private static final List<String> OTHER_EXCLUDED_TYPES = List.of(
            FileType.FOLDER.toString(),
            FileType.PICTURE.toString(),
            FileType.VIDEO.toString(),
            FileType.AUDIO.toString(),
            FileType.PDF.toString(),
            FileType.DOC.toString(),
            FileType.TXT.toString(),
            FileType.PPT.toString(),
            FileType.CODE.toString(),
            FileType.WEB.toString(),
            FileType.COMPRESS_FILE.toString());

    private final FileMapper fileMapper;
    private final ResourceMapper resourceMapper;
    private final PndProperties pndProperties;

    /**
     * 构造函数，注入依赖对象。
     *
     * @param pndProperties  PND配置属性
     * @param fileMapper     文件数据访问对象
     * @param resourceMapper 资源数据访问对象
     */
    @Autowired
    public FileServiceImpl(PndProperties pndProperties,
            FileMapper fileMapper,
            ResourceMapper resourceMapper) {
        this.fileMapper = fileMapper;
        this.pndProperties = pndProperties;
        this.resourceMapper = resourceMapper;
    }

    /**
     * 根据父文件夹ID获取文件列表。
     * 同时返回文件夹路径信息用于面包屑导航。
     *
     * @param parentId  父文件夹ID，0表示根目录
     * @param sortBy    排序字段
     * @param sortOrder 排序方式
     * @return 包含文件列表和文件夹路径的响应对象
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public ResponseDto findByParentId(Long parentId, String sortBy, String sortOrder) {
        List<File> files = fileMapper.findByParentId(parentId, false,
                normalizeSortBy(sortBy), normalizeSortOrder(sortOrder));
        List<FolderPathDto> folderPaths = getFolderTree(parentId);
        return ResponseDto.success(files, folderPaths);
    }

    @Override
    public ResponseDto findRecentFiles(Integer limit) {
        int safeLimit = normalizeLimit(limit);
        return ResponseDto.success(fileMapper.findRecentFiles(safeLimit),
                Map.of("limit", safeLimit));
    }

    @Override
    public ResponseDto getRecentUploadSummary(Integer limit) {
        int safeLimit = normalizeLimit(limit);

        // 获取当前时间的起始点
        Calendar now = Calendar.getInstance();

        // 今日起始时间
        Calendar todayStart = (Calendar) now.clone();
        todayStart.set(Calendar.HOUR_OF_DAY, 0);
        todayStart.set(Calendar.MINUTE, 0);
        todayStart.set(Calendar.SECOND, 0);
        todayStart.set(Calendar.MILLISECOND, 0);

        // 本周起始时间（周一）
        Calendar weekStart = (Calendar) now.clone();
        weekStart.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        weekStart.set(Calendar.HOUR_OF_DAY, 0);
        weekStart.set(Calendar.MINUTE, 0);
        weekStart.set(Calendar.SECOND, 0);
        weekStart.set(Calendar.MILLISECOND, 0);

        // 本月起始时间
        Calendar monthStart = (Calendar) now.clone();
        monthStart.set(Calendar.DAY_OF_MONTH, 1);
        monthStart.set(Calendar.HOUR_OF_DAY, 0);
        monthStart.set(Calendar.MINUTE, 0);
        monthStart.set(Calendar.SECOND, 0);
        monthStart.set(Calendar.MILLISECOND, 0);

        // 查询各时间范围的文件
        List<File> todayFiles = fileMapper.findByCreateTimeBetween(
                todayStart.getTime(), now.getTime(), "createTime", "desc");
        List<File> weekFiles = fileMapper.findByCreateTimeBetween(
                weekStart.getTime(), now.getTime(), "createTime", "desc");
        List<File> monthFiles = fileMapper.findByCreateTimeBetween(
                monthStart.getTime(), now.getTime(), "createTime", "desc");

        // 计算统计数据
        long todaySize = todayFiles.stream().mapToLong(f -> f.getSize() != null ? f.getSize() : 0).sum();
        long weekSize = weekFiles.stream().mapToLong(f -> f.getSize() != null ? f.getSize() : 0).sum();
        long monthSize = monthFiles.stream().mapToLong(f -> f.getSize() != null ? f.getSize() : 0).sum();

        // 获取最近文件列表
        List<File> recentFiles = fileMapper.findRecentFiles(safeLimit);

        // 构建摘要DTO
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
        List<File> files = fileMapper.findAll(normalizeSortBy(sortBy), normalizeSortOrder(sortOrder))
                .stream()
                .filter(file -> !FileType.FOLDER.toString().equals(file.getType()))
                .filter(file -> matchesCategory(file, normalizedCategory))
                .toList();
        return ResponseDto.success(files, Map.of(
                "category", normalizedCategory,
                "count", files.size()));
    }

    @Override
    public ResponseDto categorySummary() {
        Map<String, Integer> categoryCountMap = initCategoryCounter();
        for (File file : fileMapper.getAllFileType()) {
            if (FileType.FOLDER.toString().equals(file.getType())) {
                continue;
            }
            incrementCategoryCount(categoryCountMap, file.getType());
        }

        List<FileCategorySummaryDto> summaries = List.of(
                new FileCategorySummaryDto("picture", "图片", categoryCountMap.get("picture")),
                new FileCategorySummaryDto("video", "视频", categoryCountMap.get("video")),
                new FileCategorySummaryDto("audio", "音频", categoryCountMap.get("audio")),
                new FileCategorySummaryDto("document", "文档", categoryCountMap.get("document")),
                new FileCategorySummaryDto("compress", "压缩包", categoryCountMap.get("compress")),
                new FileCategorySummaryDto("other", "其他", categoryCountMap.get("other")));
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
        File file = findById(fileId);
        if (Objects.isNull(file)) {
            throw new DataNotFoundException(String.format("文件不存在 [fileId=%d]", fileId));
        }

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
        File parentFile = findById(file.getParentId());
        if (Objects.isNull(parentFile) || !FileUtils.equals(parentFile.getType(), FileType.FOLDER)) {
            log.error("父文件夹不存在或不是文件夹类型 [parentId={}, fileName={}]",
                    file.getParentId(), file.getFileName());
            throw new DataFormatException(
                    String.format("父文件夹不存在或不是文件夹类型 [parentId=%d, fileName=%s]",
                            file.getParentId(), file.getFileName()));
        }
        ensureUniqueFileName(file.getParentId(), file.getFileName(), null);
        fileMapper.save(file);

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

        // 参数校验
        if (Objects.isNull(fileName) || fileName.trim().isEmpty()) {
            throw new DataFormatException("文件名不能为空");
        }
        if (Objects.isNull(id)) {
            throw new DataFormatException("文件ID不能为空");
        }

        FileUtils.checkFileName(fileName);
        File existingFile = findById(id);
        if (Objects.isNull(existingFile)) {
            throw new DataNotFoundException(String.format("文件不存在 [fileId=%d]", id));
        }
        ensureUniqueFileName(existingFile.getParentId(), fileName, id);

        File updateFile = File.builder()
                .id(id).fileName(fileName).updateTime(new Date())
                .build();
        fileMapper.update(updateFile);

        log.info("文件重命名成功 [fileId={}, newFileName={}]", id, fileName);
    }

    /**
     * 移动文件到目标文件夹。
     * 
     * 实现原理：
     * 1. 验证目标文件夹是否存在且为文件夹类型
     * 2. 批量查询要移动的文件，验证所有文件是否存在
     * 3. 检查是否存在循环引用（将文件夹移动到其子文件夹）
     * 4. 执行批量更新操作，将文件的parent_id更新为目标文件夹ID
     * 
     * 性能优化：
     * - 使用批量查询验证文件存在性，减少数据库查询次数（从N次减少到1次）
     * - 使用批量更新操作，一次性更新所有文件
     * - 添加循环引用检查，防止数据不一致
     * - 使用Stream API优化数据处理
     * 
     * 注意事项：
     * - 不能将文件夹移动到其子文件夹中
     * - 不能将文件移动到自身
     * - 目标文件夹必须存在且为文件夹类型
     * - 使用事务确保操作的原子性
     *
     * @param ids      要移动的文件ID列表
     * @param targetId 目标文件夹ID
     * @throws DataNotFoundException 当目标文件夹不存在时抛出
     * @throws DataFormatException   当文件不存在或存在循环引用时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void moveFiles(List<Long> ids, Long targetId) {
        long startTime = System.currentTimeMillis();
        log.info("开始移动文件 [fileIds={}, targetId={}, fileCount={}]", ids, targetId, ids != null ? ids.size() : 0);

        // 参数校验
        if (Objects.isNull(ids) || ids.isEmpty()) {
            throw new DataFormatException("要移动的文件ID列表不能为空");
        }
        if (Objects.isNull(targetId)) {
            throw new DataFormatException("目标文件夹ID不能为空");
        }

        // 1. 验证目标文件夹是否存在且为文件夹类型
        File targetFolder = findById(targetId);
        if (Objects.isNull(targetFolder) || !FileType.FOLDER.toString().equals(targetFolder.getType())) {
            log.error("目标文件夹不存在或不是文件夹类型 [targetId={}, fileIds={}]", targetId, ids);
            throw new DataNotFoundException(
                    String.format("目标文件夹不存在或不是文件夹类型 [targetId=%d, fileIds=%s]", targetId, ids));
        }
        log.debug("目标文件夹验证通过 [targetFolderId={}, targetFolderName={}]", targetFolder.getId(),
                targetFolder.getFileName());

        // 2. 批量查询要移动的文件，验证所有文件是否存在（性能优化：使用批量查询）
        long queryStartTime = System.currentTimeMillis();
        List<File> filesToMove = fileMapper.findByIds(ids);
        long queryEndTime = System.currentTimeMillis();
        log.debug("批量查询文件完成 [查询耗时={}ms, 查询数量={}, 期望数量={}]",
                queryEndTime - queryStartTime, filesToMove.size(), ids.size());

        // 验证查询结果数量是否匹配
        if (filesToMove.size() != ids.size()) {
            // 找出不存在的文件ID
            Set<Long> foundIds = filesToMove.stream()
                    .map(File::getId)
                    .collect(java.util.stream.Collectors.toSet());
            List<Long> missingIds = ids.stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(java.util.stream.Collectors.toList());

            log.error("部分要移动的文件不存在 [missingFileIds={}, targetId={}]", missingIds, targetId);
            throw new DataFormatException(
                    String.format("部分要移动的文件不存在 [missingFileIds=%s, targetId=%d]", missingIds, targetId));
        }

        Set<Long> movingIds = new HashSet<>(ids);
        Set<String> targetNames = fileMapper.findByParentId(targetId, true, null, null).stream()
                .filter(existing -> !movingIds.contains(existing.getId()))
                .map(File::getFileName)
                .collect(java.util.stream.Collectors.toCollection(HashSet::new));

        // 3. 检查是否尝试将文件移动到自身或移动到其子文件夹
        long checkStartTime = System.currentTimeMillis();
        for (File file : filesToMove) {
            // 检查是否尝试将文件移动到自身
            if (file.getId().equals(targetId)) {
                log.error("不能将文件移动到自身 [fileId={}, targetId={}]", file.getId(), targetId);
                throw new DataFormatException(
                        String.format("不能将文件移动到自身 [fileId=%d, targetId=%d]", file.getId(), targetId));
            }

            // 如果是文件夹，检查目标文件夹是否是其子文件夹
            if (FileType.FOLDER.toString().equals(file.getType())) {
                if (isDescendant(targetFolder.getId(), file.getId())) {
                    log.error("不能将文件夹移动到其子文件夹中 [sourceFolderId={}, sourceFolderName={}, targetFolderId={}]",
                            file.getId(), file.getFileName(), targetFolder.getId());
                    throw new DataFormatException(
                            String.format("不能将文件夹移动到其子文件夹中 [sourceFolderId=%d, sourceFolderName=%s, targetFolderId=%d]",
                                    file.getId(), file.getFileName(), targetFolder.getId()));
                }
            }

            if (!targetNames.add(file.getFileName())) {
                throw new FileAlreadyExistsException(
                        String.format("目标目录已存在同名文件 [targetId=%d, fileName=%s]",
                                targetId, file.getFileName()));
            }
        }
        long checkEndTime = System.currentTimeMillis();
        log.debug("循环引用检查完成 [检查耗时={}ms]", checkEndTime - checkStartTime);

        // 4. 执行批量更新操作
        long updateStartTime = System.currentTimeMillis();
        Date updateTime = new Date();
        fileMapper.updateParentId(ids, targetId, updateTime);
        long updateEndTime = System.currentTimeMillis();
        log.debug("批量更新文件完成 [更新耗时={}ms]", updateEndTime - updateStartTime);

        long endTime = System.currentTimeMillis();
        log.info("文件移动成功 [fileIds={}, targetId={}, count={}, 总耗时={}ms]",
                ids, targetId, ids.size(), endTime - startTime);
    }

    /**
     * 检查目标文件夹是否是源文件夹的后代（子文件夹）。
     * 
     * 实现原理：
     * 1. 从目标文件夹开始，向上遍历父文件夹
     * 2. 如果在遍历过程中找到源文件夹，说明目标文件夹是源文件夹的后代
     * 3. 如果遍历到根目录仍未找到源文件夹，说明目标文件夹不是源文件夹的后代
     * 
     * 性能考虑：
     * - 使用循环而非递归，避免栈溢出
     * - 最多遍历到根目录，时间复杂度为O(h)，h为文件夹深度
     * 
     * @param targetId 目标文件夹ID
     * @param sourceId 源文件夹ID
     * @return true表示是后代，false表示不是后代
     */
    private boolean isDescendant(Long targetId, Long sourceId) {
        Long currentId = targetId;
        int maxDepth = 1000; // 防止无限循环的安全措施
        int depth = 0;

        while (!currentId.equals(File.ROOT_FILE.getId()) && depth < maxDepth) {
            if (currentId.equals(sourceId)) {
                return true;
            }

            File currentFile = fileMapper.findById(currentId);
            if (Objects.isNull(currentFile)) {
                break;
            }

            currentId = currentFile.getParentId();
            depth++;
        }

        return false;
    }

    /**
     * 复制文件到目标文件夹。
     * 支持批量复制，可以复制到多个目标文件夹。
     *
     * 实现原理：
     * 1. 批量查询要复制的文件信息，减少数据库查询次数（性能优化）
     * 2. 外层循环遍历所有目标文件夹
     * 3. 验证每个目标文件夹是否存在且类型为文件夹
     * 4. 根据文件类型分类处理：
     * - 普通文件：批量更新资源引用计数，批量创建文件记录（性能优化）
     * - 文件夹：调用copyFolder递归复制整个文件夹
     *
     * 性能优化：
     * - 使用批量查询文件信息，减少数据库查询次数（从N次减少到1次）
     * - 对于普通文件，使用批量更新资源引用计数，减少数据库操作次数
     * - 对于普通文件，使用批量插入文件记录，减少数据库操作次数
     * - 使用Stream API优化数据处理
     *
     * 注意事项：
     * - 复制文件夹时会递归复制所有子文件和子文件夹
     * - 复制普通文件只增加资源引用计数，不复制物理文件
     * - 使用事务确保操作的原子性
     *
     * @param fileIds   要复制的文件ID列表
     * @param targetIds 目标文件夹ID列表
     * @throws DataFormatException 当目标文件夹无效或文件不存在时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void copyFiles(List<Long> fileIds, List<Long> targetIds) {
        long startTime = System.currentTimeMillis();
        log.info("开始复制文件 [fileIds={}, targetIds={}, fileCount={}, targetCount={}]",
                fileIds, targetIds, fileIds != null ? fileIds.size() : 0, targetIds != null ? targetIds.size() : 0);

        // 参数校验
        if (Objects.isNull(fileIds) || fileIds.isEmpty()) {
            throw new DataFormatException("要复制的文件ID列表不能为空");
        }
        if (Objects.isNull(targetIds) || targetIds.isEmpty()) {
            throw new DataFormatException("目标文件夹ID列表不能为空");
        }

        // 1. 批量查询要复制的文件信息（性能优化：减少数据库查询次数）
        long queryStartTime = System.currentTimeMillis();
        List<File> filesToCopy = fileMapper.findByIds(fileIds);
        long queryEndTime = System.currentTimeMillis();
        log.debug("批量查询文件完成 [查询耗时={}ms, 查询数量={}, 期望数量={}]",
                queryEndTime - queryStartTime, filesToCopy.size(), fileIds.size());

        // 验证查询结果数量是否匹配
        if (filesToCopy.size() != fileIds.size()) {
            // 找出不存在的文件ID
            Set<Long> foundIds = filesToCopy.stream()
                    .map(File::getId)
                    .collect(java.util.stream.Collectors.toSet());
            List<Long> missingIds = fileIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(java.util.stream.Collectors.toList());

            log.error("部分要复制的文件不存在 [missingFileIds={}, targetIds={}]", missingIds, targetIds);
            throw new DataFormatException(
                    String.format("部分要复制的文件不存在 [missingFileIds=%s, targetIds=%s]", missingIds, targetIds));
        }

        // 2. 外层循环：遍历所有目标文件夹
        for (Long targetId : targetIds) {
            // 验证目标文件夹是否存在且类型为文件夹
            File targetFile = findById(targetId);
            if (Objects.isNull(targetFile) ||
                    !FileType.FOLDER.toString().equals(targetFile.getType())) {
                log.error("目标文件夹不存在或不是文件夹类型 [targetId={}]", targetId);
                throw new DataFormatException(
                        String.format("目标文件夹不存在或不是文件夹类型 [targetId=%d]", targetId));
            }

            // 3. 分类处理：普通文件和文件夹
            // 分离普通文件和文件夹
            Map<Boolean, List<File>> fileGroups = filesToCopy.stream()
                    .collect(java.util.stream.Collectors.partitioningBy(
                            file -> FileType.FOLDER.toString().equals(file.getType())));

            List<File> folders = fileGroups.get(true); // 文件夹
            List<File> commonFiles = fileGroups.get(false); // 普通文件

            log.debug("文件分类完成 [目标文件夹ID={}, 文件夹数量={}, 普通文件数量={}]",
                    targetId, folders.size(), commonFiles.size());

            // 4. 批量处理普通文件（性能优化）
            if (!commonFiles.isEmpty()) {
                long copyStartTime = System.currentTimeMillis();
                batchCopyCommonFiles(commonFiles, targetFile);
                long copyEndTime = System.currentTimeMillis();
                log.debug("批量复制普通文件完成 [目标文件夹ID={}, 文件数量={}, 耗时={}ms]",
                        targetId, commonFiles.size(), copyEndTime - copyStartTime);
            }

            // 5. 递归处理文件夹
            for (File folder : folders) {
                copyFolder(folder, targetFile);
            }
        }

        long endTime = System.currentTimeMillis();
        log.info("文件复制成功 [fileIds={}, targetIds={}, 总耗时={}ms]",
                fileIds, targetIds, endTime - startTime);
    }

    /**
     * 批量复制普通文件。
     * 批量更新资源引用计数，批量创建文件记录。
     *
     * 性能优化：
     * - 批量更新资源引用计数，减少数据库操作次数
     * - 批量插入文件记录，减少数据库操作次数
     * - 使用批量操作替代逐个操作，显著提高性能
     *
     * @param files      要复制的普通文件列表
     * @param parentFile 目标父文件夹对象
     */
    private void batchCopyCommonFiles(List<File> files, File parentFile) {
        log.debug("开始批量复制普通文件 [文件数量={}, 目标父文件夹ID={}]", files.size(), parentFile.getId());

        Set<String> targetNames = fileMapper.findByParentId(parentFile.getId(), true, null, null).stream()
                .map(File::getFileName)
                .collect(java.util.stream.Collectors.toCollection(HashSet::new));
        for (File file : files) {
            if (!targetNames.add(file.getFileName())) {
                throw new FileAlreadyExistsException(
                        String.format("目标目录已存在同名文件 [targetId=%d, fileName=%s]",
                                parentFile.getId(), file.getFileName()));
            }
        }

        // 1. 收集所有资源ID及本次需要增加的引用次数
        Map<Long, Long> copyCountByResourceId = files.stream()
                .map(File::getResourceId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.groupingBy(resourceId -> resourceId, LinkedHashMap::new,
                        java.util.stream.Collectors.counting()));
        List<Long> resourceIds = new ArrayList<>(copyCountByResourceId.keySet());

        if (resourceIds.isEmpty()) {
            log.warn("没有找到有效的资源ID [文件数量={}]", files.size());
            return;
        }

        // 2. 按资源实际复制次数更新引用计数，避免同一资源被多次复制时计数偏差
        List<Resource> resources = resourceMapper.findByIds(resourceIds);
        Map<Long, Resource> resourceMap = resources.stream()
                .collect(java.util.stream.Collectors.toMap(Resource::getId, resource -> resource));
        for (Map.Entry<Long, Long> entry : copyCountByResourceId.entrySet()) {
            Resource resource = resourceMap.get(entry.getKey());
            if (Objects.isNull(resource)) {
                throw new DatabaseException(
                        String.format("资源记录不存在，数据库数据异常 [resourceId=%d]", entry.getKey()));
            }
            resourceMapper.updateLink(resource.getId(), resource.getLink() + entry.getValue().intValue());
        }
        log.debug("资源引用计数更新完成 [资源数量={}]", resourceIds.size());

        // 3. 批量创建文件记录（性能优化：一次性插入所有文件记录）
        Date createTime = new Date();
        List<File> newFiles = files.stream()
                .map(file -> File.builder()
                        .fileName(file.getFileName())
                        .parentId(parentFile.getId())
                        .type(file.getType())
                        .resourceId(file.getResourceId())
                        .createTime(createTime)
                        .updateTime(createTime)
                        .build())
                .collect(java.util.stream.Collectors.toList());

        fileMapper.batchSave(newFiles);
        log.info("批量复制普通文件成功 [源文件数量={}, 新建文件数量={}, 目标父文件夹ID={}]",
                files.size(), newFiles.size(), parentFile.getId());
    }

    /**
     * 批量删除文件或文件夹。
     * 删除文件夹时会递归删除所有子文件和子文件夹。
     *
     * 实现原理：
     * 1. 批量查询所有要删除的文件信息（性能优化：减少数据库查询次数）
     * 2. 分类处理普通文件和文件夹：
     * - 普通文件：批量处理资源引用计数和文件记录删除
     * - 文件夹：使用迭代方式删除整个文件夹及其子文件
     *
     * 性能优化：
     * - 使用批量查询文件信息，减少数据库查询次数（从N次减少到1次）
     * - 对于普通文件，批量处理资源引用计数和文件记录删除
     * - 使用迭代方式替代递归，避免栈溢出
     * - 批量删除文件记录，减少数据库操作次数
     *
     * 注意事项：
     * - 删除文件夹时会递归删除所有子文件和子文件夹
     * - 删除普通文件时会检查资源引用计数，决定是否删除物理文件
     * - 使用事务确保操作的原子性
     * - 使用悲观锁确保数据一致性
     *
     * @param ids 要删除的文件ID列表
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteFiles(List<Long> ids) {
        long startTime = System.currentTimeMillis();
        log.info("开始删除文件 [fileIds={}, fileCount={}]", ids, ids != null ? ids.size() : 0);

        // 参数校验
        if (Objects.isNull(ids) || ids.isEmpty()) {
            throw new DataFormatException("要删除的文件ID列表不能为空");
        }

        // 1. 批量查询所有要删除的文件信息（性能优化：减少数据库查询次数）
        long queryStartTime = System.currentTimeMillis();
        List<File> filesToDelete = fileMapper.findByIds(ids);
        long queryEndTime = System.currentTimeMillis();
        log.debug("批量查询文件完成 [查询耗时={}ms, 查询数量={}, 期望数量={}]",
                queryEndTime - queryStartTime, filesToDelete.size(), ids.size());

        // 验证查询结果数量是否匹配
        if (filesToDelete.size() != ids.size()) {
            // 找出不存在的文件ID
            Set<Long> foundIds = filesToDelete.stream()
                    .map(File::getId)
                    .collect(java.util.stream.Collectors.toSet());
            List<Long> missingIds = ids.stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(java.util.stream.Collectors.toList());

            log.warn("部分要删除的文件不存在 [missingFileIds={}]", missingIds);
        }

        // 2. 分类处理：普通文件和文件夹
        Map<Boolean, List<File>> fileGroups = filesToDelete.stream()
                .collect(java.util.stream.Collectors.partitioningBy(
                        file -> FileType.FOLDER.toString().equals(file.getType())));

        List<File> folders = fileGroups.get(true); // 文件夹
        List<File> commonFiles = fileGroups.get(false); // 普通文件

        log.debug("文件分类完成 [文件夹数量={}, 普通文件数量={}]", folders.size(), commonFiles.size());

        // 3. 批量处理普通文件（性能优化）
        if (!commonFiles.isEmpty()) {
            long deleteStartTime = System.currentTimeMillis();
            batchDeleteCommonFiles(commonFiles);
            long deleteEndTime = System.currentTimeMillis();
            log.debug("批量删除普通文件完成 [文件数量={}, 耗时={}ms]",
                    commonFiles.size(), deleteEndTime - deleteStartTime);
        }

        // 4. 使用迭代方式删除文件夹（性能优化：避免递归导致的栈溢出）
        for (File folder : folders) {
            deleteFolderIterative(folder);
        }

        long endTime = System.currentTimeMillis();
        log.info("文件删除成功 [fileIds={}, 总耗时={}ms]", ids, endTime - startTime);
    }

    /**
     * 加载文件资源用于下载。
     *
     * @param fileId 文件ID
     * @return 包含资源和文件信息的包装对象
     * @throws DataNotFoundException 当文件或资源不存在时抛出
     */
    @Override
    public ResourceWrapper loadResource(Long fileId) {
        com.hd.entity.File file = findById(fileId);
        if (Objects.isNull(file) || Objects.isNull(file.getResourceId())) {
            throw new DataNotFoundException(
                    String.format("文件不存在或文件无关联资源 [fileId=%d]", fileId));
        }
        Resource pndResource = resourceMapper.findById(file.getResourceId());
        if (Objects.isNull(pndResource)) {
            throw new DataNotFoundException(
                    String.format("资源不存在 [fileId=%d, resourceId=%d]", fileId, file.getResourceId()));
        }

        try {
            // 构建文件路径并进行安全检查
            String basePath = pndProperties.getBasicResourcePath();
            String resourcePath = pndResource.getPath();
            
            // 防止路径遍历攻击：检查资源路径是否包含非法字符
            if (resourcePath.contains("..") || resourcePath.contains("\0")) {
                log.error("检测到非法的文件路径 [fileId={}, resourceId={}, resourcePath={}]",
                        fileId, file.getResourceId(), resourcePath);
                throw new DataNotFoundException(
                        String.format("非法的文件路径 [fileId=%d, resourcePath=%s]", fileId, resourcePath));
            }
            
            Path filePath = new java.io.File(basePath + java.io.File.separator + resourcePath).toPath().normalize();
            
            // 确保文件路径在基础目录内
            Path basePathNormalized = new java.io.File(basePath).toPath().normalize();
            if (!filePath.startsWith(basePathNormalized)) {
                log.error("文件路径不在允许的目录内 [fileId={}, filePath={}, basePath={}]",
                        fileId, filePath, basePath);
                throw new DataNotFoundException(
                        String.format("文件路径不在允许的目录内 [fileId=%d]", fileId));
            }
            
            org.springframework.core.io.Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                return new ResourceWrapper(resource, file);
            } else {
                throw new DataNotFoundException(
                        String.format("物理文件不存在 [fileId=%d, resourcePath=%s]", fileId, pndResource.getPath()));
            }
        } catch (MalformedURLException e) {
            log.error("文件URL转换失败 [fileId={}, resourcePath={}]", fileId, pndResource.getPath(), e);
            throw new DataNotFoundException(
                    String.format("文件资源访问失败 [fileId=%d, resourcePath=%s]", fileId, pndResource.getPath()));
        }
    }

    /**
     * 构建文件夹路径树用于面包屑导航。
     *
     * @param parentId 当前文件夹ID
     * @return 文件夹路径列表，从根目录到当前文件夹
     */
    private List<FolderPathDto> getFolderTree(Long parentId) {
        List<FolderPathDto> result = new ArrayList<>();
        buildFolderTree(parentId, result);
        Collections.reverse(result);
        return result;
    }

    /**
     * 递归构建文件夹路径树。
     *
     * 实现原理：
     * 1. 如果当前文件夹ID是根目录（0），添加根目录节点并返回
     * 2. 否则，查询当前文件夹信息
     * 3. 将当前文件夹添加到结果列表
     * 4. 递归查询父文件夹，直到到达根目录
     * 5. 最终结果是从当前文件夹到根目录的路径，需要反转后使用
     *
     * 递归终止条件：
     * - 当前文件夹ID等于根目录ID（0）
     *
     * 注意事项：
     * - 使用递归方式实现，对于深层嵌套的文件夹可能导致栈溢出
     * - 结果列表是倒序的（从当前文件夹到根目录），需要反转后使用
     *
     * @param parentId 父文件夹ID
     * @param result   路径结果列表
     * @throws DataNotFoundException 当文件夹不存在时抛出
     */
    private void buildFolderTree(Long parentId, List<FolderPathDto> result) {
        // 递归终止条件：到达根目录
        if (parentId.equals(File.ROOT_FILE.getId())) {
            result.add(new FolderPathDto(parentId, File.ROOT_FILE.getFileName()));
            return;
        }

        // 查询当前文件夹信息
        File file = findById(parentId);
        if (Objects.nonNull(file) && FileUtils.equals(file.getType(), FileType.FOLDER)) {
            // 将当前文件夹添加到结果列表
            result.add(new FolderPathDto(file.getId(), file.getFileName()));
            // 递归查询父文件夹
            buildFolderTree(file.getParentId(), result);
        } else {
            throw new DataNotFoundException(
                    String.format("文件夹不存在或不是文件夹类型 [parentId=%d]", parentId));
        }
    }

    /**
     * 批量删除普通文件。
     * 批量处理资源引用计数和文件记录删除。
     *
     * 性能优化：
     * - 批量查询资源信息，减少数据库查询次数
     * - 批量更新资源引用计数，减少数据库操作次数
     * - 批量删除文件记录，减少数据库操作次数
     * - 批量删除资源记录，减少数据库操作次数
     * - 使用批量操作替代逐个操作，显著提高性能
     *
     * @param files 要删除的普通文件列表
     */
    private void batchDeleteCommonFiles(List<File> files) {
        log.debug("开始批量删除普通文件 [文件数量={}]", files.size());

        // 1. 收集所有资源ID
        List<Long> resourceIds = files.stream()
                .map(File::getResourceId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(java.util.stream.Collectors.toList());

        if (resourceIds.isEmpty()) {
            log.warn("没有找到有效的资源ID [文件数量={}]", files.size());
            // 即使没有资源ID，也要删除文件记录
            List<Long> fileIds = files.stream()
                    .map(File::getId)
                    .collect(java.util.stream.Collectors.toList());
            fileMapper.deleteByIds(fileIds);
            log.info("批量删除文件记录成功（无资源关联） [文件数量={}]", files.size());
            return;
        }

        // 2. 批量查询资源信息（性能优化：一次性查询所有资源）
        List<Resource> resources = resourceMapper.findByIds(resourceIds);
        Map<Long, Resource> resourceMap = resources.stream()
                .collect(java.util.stream.Collectors.toMap(Resource::getId, r -> r));
        Map<Long, Long> deleteCountByResourceId = files.stream()
                .map(File::getResourceId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.groupingBy(resourceId -> resourceId, LinkedHashMap::new,
                        java.util.stream.Collectors.counting()));

        log.debug("批量查询资源完成 [资源数量={}]", resources.size());

        // 3. 分类处理资源：需要删除物理文件的资源和只需要减少引用计数的资源
        List<Long> fileIdsToDelete = new ArrayList<>();
        Set<Long> resourceIdsToDelete = new LinkedHashSet<>();
        Map<Long, Integer> resourceLinkTargetMap = new LinkedHashMap<>();

        for (File file : files) {
            if (file.getResourceId() == null) {
                // 没有关联资源，直接删除文件记录
                fileIdsToDelete.add(file.getId());
                continue;
            }

            Resource resource = resourceMap.get(file.getResourceId());
            if (resource == null) {
                log.warn("资源不存在 [fileId={}, resourceId={}]", file.getId(), file.getResourceId());
                fileIdsToDelete.add(file.getId());
                continue;
            }

            int remainingLink = resource.getLink() - deleteCountByResourceId.getOrDefault(resource.getId(), 0L).intValue();
            if (remainingLink <= 0) {
                resourceIdsToDelete.add(resource.getId());
            } else {
                resourceLinkTargetMap.put(resource.getId(), remainingLink);
            }

            fileIdsToDelete.add(file.getId());
        }

        log.debug("资源分类完成 [需要删除物理文件的数量={}, 需要减少引用计数的数量={}]",
                resourceIdsToDelete.size(), resourceLinkTargetMap.size());

        // 4. 删除物理文件（性能优化：批量删除）
        if (!resourceIdsToDelete.isEmpty()) {
            List<Resource> resourcesToDelete = resourceIdsToDelete.stream()
                    .map(resourceMap::get)
                    .filter(Objects::nonNull)
                    .collect(java.util.stream.Collectors.toList());

            int deletedCount = 0;
            for (Resource resource : resourcesToDelete) {
                java.io.File resourceFile = new java.io.File(pndProperties.getBasicResourcePath() +
                        java.io.File.separator + resource.getPath());
                if (resourceFile.exists() && resourceFile.delete()) {
                    deletedCount++;
                } else {
                    log.error("物理文件删除失败 [resourceId={}, resourcePath={}, fileExists={}]",
                            resource.getId(), resource.getPath(), resourceFile.exists());
                }
            }
            log.info("批量删除物理文件完成 [成功删除数量={}, 总数量={}]", deletedCount, resourcesToDelete.size());

            // 批量删除资源记录
            resourceMapper.batchDelete(new ArrayList<>(resourceIdsToDelete));
            log.debug("批量删除资源记录完成 [资源数量={}]", resourceIdsToDelete.size());
        }

        // 5. 批量减少资源引用计数（性能优化：批量更新）
        if (!resourceLinkTargetMap.isEmpty()) {
            for (Map.Entry<Long, Integer> entry : resourceLinkTargetMap.entrySet()) {
                resourceMapper.updateLink(entry.getKey(), entry.getValue());
            }
            log.debug("批量减少资源引用计数完成 [资源数量={}]", resourceLinkTargetMap.size());
        }

        // 6. 批量删除文件记录（性能优化：批量删除）
        if (!fileIdsToDelete.isEmpty()) {
            fileMapper.deleteByIds(fileIdsToDelete);
            log.info("批量删除文件记录成功 [文件数量={}]", fileIdsToDelete.size());
        }
    }

    /**
     * 使用迭代方式删除文件夹及其所有子文件和子文件夹。
     *
     * 性能优化：
     * - 使用迭代方式替代递归，避免栈溢出
     * - 使用栈数据结构模拟递归调用
     * - 批量查询和删除操作，减少数据库操作次数
     *
     * 注意事项：
     * - 使用迭代方式实现，避免深层嵌套导致的栈溢出
     * - 使用悲观锁确保数据一致性
     * - 删除操作是原子性的，要么全部成功，要么全部回滚
     *
     * @param folder 要删除的文件夹对象
     */
    private void deleteFolderIterative(File folder) {
        log.debug("开始迭代删除文件夹 [folderId={}, folderName={}]", folder.getId(), folder.getFileName());

        // 使用栈来模拟递归调用，避免栈溢出
        Stack<File> folderStack = new Stack<>();
        folderStack.push(folder);

        // 收集所有需要删除的文件和文件夹
        List<File> allFilesToDelete = new ArrayList<>();
        List<File> allFoldersToDelete = new ArrayList<>();
        allFoldersToDelete.add(folder);

        // 迭代遍历文件夹树，收集所有文件和文件夹
        while (!folderStack.isEmpty()) {
            File currentFolder = folderStack.pop();

            // 查询当前文件夹的所有子文件
            List<File> children = fileMapper.findByParentIdForUpdate(currentFolder.getId());

            for (File child : children) {
                if (FileType.FOLDER.toString().equals(child.getType())) {
                    // 如果是文件夹，添加到栈中继续处理
                    folderStack.push(child);
                    allFoldersToDelete.add(child);
                } else {
                    // 如果是普通文件，添加到删除列表
                    allFilesToDelete.add(child);
                }
            }
        }

        log.debug("文件夹遍历完成 [文件夹数量={}, 普通文件数量={}]",
                allFoldersToDelete.size(), allFilesToDelete.size());

        // 批量删除普通文件
        if (!allFilesToDelete.isEmpty()) {
            batchDeleteCommonFiles(allFilesToDelete);
        }

        // 批量删除文件夹记录（从最深层开始删除，避免外键约束问题）
        // 反转列表，确保从最深层开始删除
        Collections.reverse(allFoldersToDelete);
        List<Long> folderIdsToDelete = allFoldersToDelete.stream()
                .map(File::getId)
                .collect(java.util.stream.Collectors.toList());
        fileMapper.deleteByIds(folderIdsToDelete);

        log.info("迭代删除文件夹成功 [folderId={}, folderName={}, 删除文件夹数量={}, 删除文件数量={}]",
                folder.getId(), folder.getFileName(), allFoldersToDelete.size(), allFilesToDelete.size());
    }

    /**
     * 递归删除文件夹及其所有子文件和子文件夹。
     *
     * 实现原理：
     * 1. 使用悲观锁查询文件夹的所有子文件，防止并发修改
     * 2. 遍历每个子文件：
     * - 如果是文件夹，递归调用deleteFolder继续删除
     * - 如果是普通文件，调用deleteCommonFile删除文件和资源
     * 3. 所有子文件删除完成后，删除文件夹记录本身
     *
     * 注意事项：
     * - 使用递归方式实现，对于深层嵌套的文件夹可能导致栈溢出
     * - 使用悲观锁确保数据一致性，但可能影响并发性能
     * - 删除操作是原子性的，要么全部成功，要么全部回滚
     *
     * @param file 要删除的文件夹对象
     * @deprecated 使用 {@link #deleteFolderIterative(File)} 替代，避免递归导致的栈溢出
     */
    @Deprecated
    private void deleteFolder(File file) {
        // 使用悲观锁查询子文件，防止并发修改导致数据不一致
        // findByParentIdForUpdate 会在数据库层面加行锁
        List<File> children = fileMapper.findByParentIdForUpdate(file.getId());

        // 遍历所有子文件，递归删除
        for (File f : children) {
            if (FileType.FOLDER.toString().equals(f.getType())) {
                // 如果是文件夹，递归删除
                deleteFolder(f);
            } else {
                // 如果是普通文件，删除文件和资源
                deleteCommonFile(f);
            }
        }

        // 所有子文件删除完成后，删除文件夹记录本身
        fileMapper.deleteByIds(Collections.singletonList(file.getId()));
    }

    /**
     * 删除普通文件。
     * 如果资源的引用计数为1，则删除物理文件和资源记录；
     * 否则只减少引用计数。
     *
     * 实现原理：
     * 1. 使用悲观锁查询资源记录，防止并发修改导致数据不一致
     * 2. 检查资源的引用计数（link字段），判断是否还有其他文件引用该资源
     * 3. 如果引用计数<=1，说明这是最后一个引用该资源的文件：
     * - 删除物理文件，释放磁盘空间
     * - 删除资源记录，释放数据库记录
     * 4. 如果引用计数>1，说明还有其他文件引用该资源：
     * - 只减少引用计数，保留物理文件和资源记录
     * - 这样可以实现文件共享，节省存储空间
     * 5. 最后删除文件记录本身
     *
     * 性能优化：
     * - 移除 REQUIRES_NEW 事务传播机制，使用默认的事务传播机制
     * - 在批量删除场景下，使用 batchDeleteCommonFiles 方法进行批量操作
     * - 此方法主要用于单个文件删除场景
     *
     * @param file 要删除的文件对象
     * @throws IllegalStateException 当数据库数据异常或文件删除失败时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteCommonFile(File file) {
        log.debug("开始删除普通文件 [fileId={}, fileName={}, resourceId={}]",
                file.getId(), file.getFileName(), file.getResourceId());

        // 使用悲观锁查询资源，防止并发修改
        // findByIdForUpdate 会在数据库层面加行锁，确保同一时刻只有一个事务可以修改该资源
        Resource resource = resourceMapper.findByIdForUpdate(file.getResourceId());
        if (Objects.isNull(resource)) {
            log.error("资源记录不存在，数据库数据异常 [fileId={}, resourceId={}]",
                    file.getId(), file.getResourceId());
            throw new DatabaseException(
                    String.format("资源记录不存在，数据库数据异常 [fileId=%d, resourceId=%d]",
                            file.getId(), file.getResourceId()));
        }

        // 判断引用计数，决定是否删除物理文件
        if (resource.getLink() <= 1) {
            // 引用计数<=1，说明这是最后一个引用该资源的文件
            // 需要删除物理文件和资源记录
            java.io.File resourceFile = new java.io.File(pndProperties.getBasicResourcePath() +
                    java.io.File.separator + resource.getPath());
            if (resourceFile.exists() && resourceFile.delete()) {
                // 物理文件删除成功，删除资源记录
                resourceMapper.delete(resource.getId());
                log.info("物理文件和资源记录删除成功 [fileId={}, resourceId={}, resourcePath={}]",
                        file.getId(), resource.getId(), resource.getPath());
            } else {
                // 物理文件删除失败，可能是文件被占用或权限不足
                log.error("物理文件删除失败 [fileId={}, resourceId={}, resourcePath={}, fileExists={}]",
                        file.getId(), resource.getId(), resource.getPath(), resourceFile.exists());
                throw new FileOperationException(
                        String.format("物理文件删除失败 [fileId=%d, resourceId=%d, resourcePath=%s, fileExists=%s]",
                                file.getId(), resource.getId(), resource.getPath(), resourceFile.exists()));
            }
        } else {
            // 引用计数>1，说明还有其他文件引用该资源
            // 只减少引用计数，保留物理文件和资源记录
            resourceMapper.updateLink(resource.getId(), resource.getLink() - 1);
            log.info("资源引用计数减少 [fileId={}, resourceId={}, oldLink={}, newLink={}]",
                    file.getId(), resource.getId(), resource.getLink(), resource.getLink() - 1);
        }

        // 删除文件记录本身
        fileMapper.deleteByIds(Collections.singletonList(file.getId()));
        log.debug("文件记录删除成功 [fileId={}, fileName={}]", file.getId(), file.getFileName());
    }

    /**
     * 递归复制文件夹及其所有子文件和子文件夹。
     *
     * 实现原理：
     * 1. 检查是否尝试复制到子文件夹，防止无限循环
     * 2. 创建新的文件夹记录，保持原文件夹名称
     * 3. 查询原文件夹的所有子文件
     * 4. 遍历每个子文件：
     * - 如果是文件夹，递归调用copyFolder继续复制
     * - 如果是普通文件，调用copyCommonFile复制文件和增加资源引用计数
     *
     * 循环复制检查原理：
     * - 检查目标文件夹的父文件夹ID是否等于源文件夹ID
     * - 如果相等，说明目标文件夹是源文件夹的子文件夹
     * - 这样会导致无限递归复制，必须禁止
     *
     * 注意事项：
     * - 使用递归方式实现，对于深层嵌套的文件夹可能导致栈溢出
     * - 复制操作是原子性的，要么全部成功，要么全部回滚
     * - 文件复制只增加资源引用计数，不复制物理文件，节省存储空间
     *
     * @param file       要复制的文件夹对象
     * @param parentFile 目标父文件夹对象
     * @throws DataFormatException 当尝试复制到子文件夹时抛出
     */
    private void copyFolder(File file, File parentFile) {
        // 检查是否尝试复制到子文件夹，防止无限循环
        // 如果目标文件夹的父文件夹ID等于源文件夹ID，说明目标文件夹是源文件夹的子文件夹
        if (parentFile.getParentId() != null && parentFile.getParentId().equals(file.getId())) {
            throw new DataFormatException(
                    String.format("不能复制到子文件夹中 [sourceFolderId=%d, sourceFolderName=%s, targetFolderId=%d]",
                            file.getId(), file.getFileName(), parentFile.getId()));
        }

        // 创建新的文件夹记录
        File newFile = File.builder()
                .type(FileType.FOLDER.toString()).parentId(parentFile.getId())
                .fileName(file.getFileName())
                .build();
        ensureUniqueFileName(parentFile.getId(), file.getFileName(), null);
        fileMapper.save(newFile);

        // 查询原文件夹的所有子文件（不使用锁，因为只是读取）
        List<File> children = fileMapper.findByParentId(file.getId(), true, null, null);

        // 遍历所有子文件，递归复制
        for (File f : children) {
            if (FileType.FOLDER.toString().equals(f.getType())) {
                // 如果是文件夹，递归复制
                copyFolder(f, newFile);
            } else {
                // 如果是普通文件，复制文件记录并增加资源引用计数
                copyCommonFile(f, newFile);
            }
        }
    }

    /**
     * 复制普通文件。
     * 增加资源的引用计数，并在数据库中创建新的文件记录。
     *
     * 实现原理：
     * 1. 使用悲观锁查询资源记录，防止并发修改导致引用计数错误
     * 2. 增加资源的引用计数（link字段），表示多了一个文件引用该资源
     * 3. 创建新的文件记录，指向同一个资源ID
     * 4. 这样多个文件记录可以指向同一个物理文件，节省存储空间
     *
     * 性能优化：
     * - 移除 REQUIRES_NEW 事务传播机制，使用默认的事务传播机制
     * - 在批量复制场景下，使用 batchCopyCommonFiles 方法进行批量操作
     * - 此方法主要用于单个文件复制场景（如递归复制文件夹中的文件）
     *
     * 引用计数机制优势：
     * - 节省存储空间：多个文件记录共享同一个物理文件
     * - 提高复制速度：不需要复制物理文件，只增加引用计数
     * - 自动清理：当引用计数降为0时，自动删除物理文件
     *
     * @param file       要复制的文件对象
     * @param parentFile 目标父文件夹对象
     * @throws IllegalStateException 当数据库数据异常时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void copyCommonFile(File file, File parentFile) {
        log.debug("开始复制普通文件 [fileId={}, fileName={}, targetParentId={}]",
                file.getId(), file.getFileName(), parentFile.getId());

        // 使用悲观锁查询资源，防止并发修改引用计数
        // findByIdForUpdate 会在数据库层面加行锁，确保引用计数的原子性更新
        Resource resource = resourceMapper.findByIdForUpdate(file.getResourceId());
        if (Objects.isNull(resource)) {
            log.error("资源记录不存在，数据库数据异常 [fileId={}, resourceId={}, fileName={}]",
                    file.getId(), file.getResourceId(), file.getFileName());
            throw new DatabaseException(
                    String.format("资源记录不存在，数据库数据异常 [fileId=%d, resourceId=%d, fileName=%s]",
                            file.getId(), file.getResourceId(), file.getFileName()));
        }

        // 增加资源引用计数，表示多了一个文件引用该资源
        resourceMapper.updateLink(resource.getId(), resource.getLink() + 1);

        // 创建新的文件记录，指向同一个资源ID
        // 这样多个文件记录可以共享同一个物理文件，节省存储空间
        ensureUniqueFileName(parentFile.getId(), file.getFileName(), null);
        File newFile = File.builder()
                .fileName(file.getFileName()).parentId(parentFile.getId())
                .type(file.getType()).resourceId(file.getResourceId())
                .build();
        fileMapper.save(newFile);

        log.info("文件复制成功 [sourceFileId={}, newFileId={}, fileName={}, resourceId={}, newLink={}]",
                file.getId(), newFile.getId(), file.getFileName(), resource.getId(), resource.getLink() + 1);
    }

    /**
     * 根据ID查找文件。
     * ID为0时返回根文件对象。
     *
     * @param id 文件ID
     * @return 文件对象，如果不存在则返回null
     */
    private File findById(Long id) {
        if (id == 0) {
            return File.ROOT_FILE;
        } else {
            return fileMapper.findById(id);
        }
    }

    private void ensureUniqueFileName(Long parentId, String fileName, Long excludeId) {
        Integer count = fileMapper.countByParentIdAndFileName(parentId, fileName, excludeId);
        if (count != null && count > 0) {
            File existingFile = fileMapper.findByParentIdAndFileName(parentId, fileName);
            Long existingFileId = existingFile != null ? existingFile.getId() : null;
            String suggestedName = generateUniqueFileName(parentId, fileName, excludeId);
            throw FileAlreadyExistsException.withSuggestion(parentId, fileName, suggestedName);
        }
    }

    /**
     * 生成唯一的文件名。
     * 当目标文件名已存在时，生成一个带序号的新文件名。
     *
     * <p>生成规则：
     * <ul>
     *   <li>原文件名：example.txt</li>
     *   <li>冲突后：example (1).txt</li>
     *   <li>再次冲突：example (2).txt</li>
     *   <li>以此类推...</li>
     * </ul>
     *
     * @param parentId  父目录ID
     * @param fileName  原文件名
     * @param excludeId 排除的文件ID
     * @return 建议的唯一文件名
     */
    private String generateUniqueFileName(Long parentId, String fileName, Long excludeId) {
        // 分离文件名和扩展名
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

        // 尝试生成带序号的新文件名
        for (int i = 1; i <= 1000; i++) {
            String newName = baseName + " (" + i + ")" + extension;
            Integer count = fileMapper.countByParentIdAndFileName(parentId, newName, excludeId);
            if (count == null || count == 0) {
                return newName;
            }
        }

        // 如果尝试1000次都失败，使用UUID作为最后方案
        return baseName + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_RECENT_LIMIT;
        }
        return Math.min(limit, MAX_RECENT_LIMIT);
    }

    private String normalizeSortBy(String sortBy) {
        if (Objects.isNull(sortBy) || sortBy.isBlank()) {
            return "fileName";
        }
        return switch (sortBy) {
            case "size", "createTime", "updateTime", "fileName" -> sortBy;
            default -> "fileName";
        };
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
        return switch (category) {
            case "picture" -> FileType.PICTURE.toString().equals(file.getType());
            case "video" -> FileType.VIDEO.toString().equals(file.getType());
            case "audio" -> FileType.AUDIO.toString().equals(file.getType());
            case "document" -> DOCUMENT_TYPES.contains(file.getType());
            case "compress" -> COMPRESS_TYPES.contains(file.getType());
            case "other" -> !OTHER_EXCLUDED_TYPES.contains(file.getType());
            default -> false;
        };
    }

    private Map<String, Integer> initCategoryCounter() {
        Map<String, Integer> categoryCountMap = new LinkedHashMap<>();
        categoryCountMap.put("picture", 0);
        categoryCountMap.put("video", 0);
        categoryCountMap.put("audio", 0);
        categoryCountMap.put("document", 0);
        categoryCountMap.put("compress", 0);
        categoryCountMap.put("other", 0);
        return categoryCountMap;
    }

    private void incrementCategoryCount(Map<String, Integer> categoryCountMap, String type) {
        if (FileType.PICTURE.toString().equals(type)) {
            categoryCountMap.computeIfPresent("picture", (key, count) -> count + 1);
            return;
        }
        if (FileType.VIDEO.toString().equals(type)) {
            categoryCountMap.computeIfPresent("video", (key, count) -> count + 1);
            return;
        }
        if (FileType.AUDIO.toString().equals(type)) {
            categoryCountMap.computeIfPresent("audio", (key, count) -> count + 1);
            return;
        }
        if (DOCUMENT_TYPES.contains(type)) {
            categoryCountMap.computeIfPresent("document", (key, count) -> count + 1);
            return;
        }
        if (COMPRESS_TYPES.contains(type)) {
            categoryCountMap.computeIfPresent("compress", (key, count) -> count + 1);
            return;
        }
        categoryCountMap.computeIfPresent("other", (key, count) -> count + 1);
    }

    private String buildFolderPath(List<FolderPathDto> navigation) {
        return navigation.stream()
                .map(FolderPathDto::getFileName)
                .reduce((left, right) -> left + " / " + right)
                .orElse(File.ROOT_FILE.getFileName());
    }

    // ==================== MD5校验与秒传功能实现 ====================

    /**
     * 上传文件（支持秒传）。
     *
     * <p>实现流程：
     * <ol>
     *   <li>参数校验和权限检查</li>
     *   <li>计算上传文件的MD5值</li>
     *   <li>查询数据库是否存在相同MD5的资源</li>
     *   <li>如果存在：执行秒传，直接关联已有资源</li>
     *   <li>如果不存在：保存新文件并计算存储MD5值</li>
     * </ol>
     *
     * <p>性能优化：
     * <ul>
     *   <li>使用流式计算MD5，避免内存溢出</li>
     *   <li>通过MD5索引快速查询，O(1)时间复杂度</li>
     *   <li>秒传场景下无需IO操作，毫秒级响应</li>
     * </ul>
     *
     * @param file      上传的文件对象
     * @param parentId  父文件夹ID
     * @return 包含文件信息和秒传状态的响应对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseDto uploadWithMD5(MultipartFile file, Long parentId) {
        long startTime = System.currentTimeMillis();
        log.info("开始上传文件（支持秒传）[fileName={}, fileSize={}, parentId={}]",
                file.getOriginalFilename(), file.getSize(), parentId);

        try {
            // 1. 参数校验
            if (file == null || file.isEmpty()) {
                throw new DataFormatException("上传文件不能为空");
            }
            if (parentId == null) {
                throw new DataFormatException("父文件夹ID不能为空");
            }

            String fileName = file.getOriginalFilename();
            FileUtils.checkFileName(fileName);

            // 验证父文件夹存在且为文件夹类型
            File parentFile = findById(parentId);
            if (Objects.isNull(parentFile) || !FileType.FOLDER.toString().equals(parentFile.getType())) {
                throw new DataFormatException("父文件夹不存在或不是文件夹类型");
            }
            ensureUniqueFileName(parentId, fileName, null);

            // 2. 计算上传文件的MD5值
            log.debug("开始计算上传文件MD5 [fileName={}]", fileName);
            long md5StartTime = System.currentTimeMillis();
            String md5;
            try (InputStream inputStream = file.getInputStream()) {
                md5 = MD5Utils.calculateMD5(inputStream);
            }
            long md5EndTime = System.currentTimeMillis();
            log.info("文件MD5计算完成 [fileName={}, md5={}, 耗时={}ms]",
                    fileName, md5, md5EndTime - md5StartTime);

            if (md5 == null || md5.isEmpty()) {
                throw new FileOperationException("文件MD5计算失败");
            }

            // 3. 查询是否已存在相同MD5的资源（秒传检查）
            Resource existingResource = resourceMapper.findByMd5(md5);
            boolean isInstantUpload = false;

            File newFile;
            if (existingResource != null) {
                // 4a. 执行秒传：直接关联已有资源
                log.info("发现相同MD5的已存在资源，执行秒传 [md5={}, existingResourceId={}, fileName={}]",
                        md5, existingResource.getId(), fileName);

                // 增加已有资源的引用计数
                resourceMapper.updateLink(existingResource.getId(), existingResource.getLink() + 1);

                // 创建新的文件记录，指向已有资源
                newFile = File.builder()
                        .fileName(fileName)
                        .parentId(parentId)
                        .type(FileUtils.getFileType(fileName).toString())
                        .resourceId(existingResource.getId())
                        .size(file.getSize())
                        .build();
                fileMapper.save(newFile);

                isInstantUpload = true;
                log.info("秒传成功 [newFileId={}, fileName={}, resourceId={}, md5={}]",
                        newFile.getId(), fileName, existingResource.getId(), md5);
            } else {
                // 4b. 正常上传：保存新文件并存储MD5值
                log.info("未发现相同MD5的资源，执行正常上传 [md5={}, fileName={}]", md5, fileName);

                // 保存物理文件到磁盘
                Resource savedResource = saveUploadFile(file);

                // 设置MD5值
                savedResource.setMd5(md5);

                // 更新资源的MD5值到数据库
                resourceMapper.updateResourceMD5(savedResource.getId(), md5);

                // 创建文件记录
                newFile = File.builder()
                        .fileName(fileName)
                        .parentId(parentId)
                        .type(FileUtils.getFileType(fileName).toString())
                        .resourceId(savedResource.getId())
                        .size(file.getSize())
                        .build();
                fileMapper.save(newFile);

                log.info("正常上传成功 [newFileId={}, fileName={}, resourceId={}, md5={}]",
                        newFile.getId(), fileName, savedResource.getId(), md5);
            }

            long endTime = System.currentTimeMillis();

            // 构建返回结果
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("file", newFile);
            resultData.put("isInstantUpload", isInstantUpload);
            resultData.put("md5", md5);

            log.info("文件上传完成 [fileName={}, isInstantUpload={}, 总耗时={}ms]",
                    fileName, isInstantUpload, endTime - startTime);

            return ResponseDto.success(resultData);

        } catch (IOException e) {
            log.error("文件上传IO异常", e);
            throw new FileOperationException("文件上传失败：" + e.getMessage());
        } catch (DataFormatException | DataNotFoundException | FileOperationException e) {
            throw e; // 重新抛出业务异常
        } catch (Exception e) {
            log.error("文件上传异常", e);
            throw new FileOperationException("文件上传失败：" + e.getMessage());
        }
    }

    /**
     * 根据MD5值查询资源是否存在（用于秒传预检）。
     *
     * <p>使用场景：
     * <ul>
     *   <li>前端在文件选择后、上传前调用此接口</li>
     *   <li>前端先在本地计算MD5（使用Web Worker避免阻塞UI）</li>
     *   <li>如果返回存在，提示用户"可秒传"，用户确认后调用instantUpload</li>
     *   <li>如果返回不存在，提示用户需要正常上传</li>
     * </ul>
     *
     * @param md5 文件的MD5值
     * @return 包含资源信息的响应对象
     */
    @Override
    public ResponseDto checkFileByMD5(String md5) {
        log.debug("检查文件MD5是否存在 [md5={}]", md5);

        // 参数校验
        if (md5 == null || md5.trim().isEmpty()) {
            throw new DataFormatException("MD5值不能为空");
        }

        // 标准化MD5格式（转小写）
        md5 = md5.trim().toLowerCase();

        // 查询数据库
        Resource resource = resourceMapper.findByMd5(md5);

        if (resource != null) {
            log.info("发现相同MD5的资源 [md5={}, resourceId={}, size={}]", 
                    md5, resource.getId(), resource.getSize());

            Map<String, Object> resultData = new HashMap<>();
            resultData.put("exists", true);
            resultData.put("resource", resource);
            resultData.put("message", "文件已存在，可以秒传");

            return ResponseDto.success(resultData);
        } else {
            log.debug("未发现相同MD5的资源 [md5={}]", md5);

            Map<String, Object> resultData = new HashMap<>();
            resultData.put("exists", false);
            resultData.put("message", "文件不存在，需要正常上传");

            return ResponseDto.success(resultData);
        }
    }

    /**
     * 验证文件的MD5完整性。
     *
     * <p>应用场景：
     * <ul>
     *   <li>用户手动触发文件完整性检查</li>
     *   <li>系统定期自动校验任务</li>
     *   <li>文件下载后的完整性验证</li>
     * </ul>
     *
     * @param fileId 文件ID
     * @return 包含验证结果的响应对象
     */
    @Override
    public ResponseDto verifyFileMD5(Long fileId) {
        log.info("开始验证文件MD5完整性 [fileId={}]", fileId);

        // 参数校验
        if (fileId == null) {
            throw new DataFormatException("文件ID不能为空");
        }

        // 查询文件信息
        File file = findById(fileId);
        if (Objects.isNull(file)) {
            throw new DataNotFoundException(String.format("文件不存在 [fileId=%d]", fileId));
        }

        if (Objects.isNull(file.getResourceId())) {
            throw new DataFormatException("文件没有关联资源，无法验证");
        }

        // 查询资源信息
        Resource resource = resourceMapper.findById(file.getResourceId());
        if (Objects.isNull(resource)) {
            throw new DataNotFoundException("关联的资源记录不存在");
        }

        String expectedMD5 = resource.getMd5();
        if (expectedMD5 == null || expectedMD5.isEmpty()) {
            log.warn("资源没有MD5值，跳过验证 [fileId={}, resourceId={}]", fileId, resource.getId());
            
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("valid", null); // 无法验证
            resultData.put("message", "资源没有MD5值，无法验证完整性");

            return ResponseDto.success(resultData);
        }

        // 构建物理文件路径
        Path filePath = Paths.get(pndProperties.getBasicResourcePath(), resource.getPath()).normalize();

        // 验证文件是否存在
        if (!Files.exists(filePath)) {
            log.error("物理文件不存在 [fileId={}, filePath={}]", fileId, filePath);
            
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("valid", false);
            resultData.put("expected", expectedMD5);
            resultData.put("error", "物理文件不存在");

            return ResponseDto.success(resultData);
        }

        // 计算实际MD5值
        log.debug("开始计算实际MD5值 [fileId={}, filePath={}]", fileId, filePath);
        long startTime = System.currentTimeMillis();
        String actualMD5 = MD5Utils.calculateMD5(filePath);
        long endTime = System.currentTimeMillis();

        if (actualMD5 == null) {
            log.error("计算MD5失败 [fileId={}, filePath={}]", fileId, filePath);
            
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("valid", false);
            resultData.put("expected", expectedMD5);
            resultData.put("error", "MD5计算失败");

            return ResponseDto.success(resultData);
        }

        // 比较MD5值
        boolean isValid = actualMD5.equalsIgnoreCase(expectedMD5);
        
        log.info("MD5验证完成 [fileId={}, valid={}, expected={}, actual={}, 耗时={}ms]",
                fileId, isValid, expectedMD5, actualMD5, endTime - startTime);

        Map<String, Object> resultData = new HashMap<>();
        resultData.put("valid", isValid);
        resultData.put("expected", expectedMD5);
        resultData.put("actual", actualMD5);
        resultData.put("fileName", file.getFileName());
        resultData.put("verifyTimeMs", endTime - startTime);

        return ResponseDto.success(resultData);
    }

    /**
     * 执行秒传操作。
     *
     * <p>实现原理：
     * <ol>
     *   <li>根据MD5查找已有资源</li>
     *   <li>增加资源引用计数</li>
     *   <li>创建新的文件记录指向该资源</li>
     *   <li>无需实际的文件传输操作</li>
     * </ol>
     *
     * <p>性能优势：
     * <ul>
     *   <li>无网络带宽消耗</li>
     *   <li>无磁盘IO操作</li>
     *   <li>毫秒级响应速度</li>
     *   <li>节省服务器存储空间</li>
     * </ul>
     *
     * @param md5      资源的MD5值
     * @param fileName 文件名
     * @param parentId 父文件夹ID
     * @return 包含新建文件信息的响应对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseDto instantUpload(String md5, String fileName, Long parentId) {
        long startTime = System.currentTimeMillis();
        log.info("开始执行秒传 [md5={}, fileName={}, parentId={}]", md5, fileName, parentId);

        // 参数校验
        if (md5 == null || md5.trim().isEmpty()) {
            throw new DataFormatException("MD5值不能为空");
        }
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new DataFormatException("文件名不能为空");
        }
        if (parentId == null) {
            throw new DataFormatException("父文件夹ID不能为空");
        }

        FileUtils.checkFileName(fileName);

        // 标准化MD5格式
        md5 = md5.trim().toLowerCase();

        // 验证父文件夹
        File parentFile = findById(parentId);
        if (Objects.isNull(parentFile) || !FileType.FOLDER.toString().equals(parentFile.getType())) {
            throw new DataFormatException("父文件夹不存在或不是文件夹类型");
        }
        ensureUniqueFileName(parentId, fileName, null);

        // 查找已有资源
        Resource existingResource = resourceMapper.findByMd5(md5);
        if (existingResource == null) {
            log.warn("秒传失败：未找到对应MD5的资源 [md5={}, fileName={}]", md5, fileName);
            throw new DataNotFoundException(
                    String.format("未找到MD5为[%s]的资源，无法秒传", md5));
        }

        log.info("找到目标资源，开始创建文件记录 [resourceId={}, md5={}, size={}]",
                existingResource.getId(), md5, existingResource.getSize());

        // 增加资源引用计数
        resourceMapper.updateLink(existingResource.getId(), existingResource.getLink() + 1);

        // 创建新的文件记录
        File newFile = File.builder()
                .fileName(fileName)
                .parentId(parentId)
                .type(FileUtils.getFileType(fileName).toString())
                .resourceId(existingResource.getId())
                .size(existingResource.getSize())
                .build();
        fileMapper.save(newFile);

        long endTime = System.currentTimeMillis();

        log.info("秒传成功 [newFileId={}, fileName={}, resourceId={}, md5={}, 耗时={}ms]",
                newFile.getId(), fileName, existingResource.getId(), md5, endTime - startTime);

        // 返回结果
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("file", newFile);
        resultData.put("isInstantUpload", true);
        resultData.put("md5", md5);
        resultData.put("originalSize", existingResource.getSize());

        return ResponseDto.success(resultData);
    }

    /**
     * 保存上传的文件到磁盘。
     *
     * <p>实现逻辑：
     * <ol>
     *   <li>生成唯一的存储路径（年/月/UUID.扩展名）</li>
     *   <li>创建目录结构（如不存在）</li>
     *   <li>将上传文件写入磁盘</li>
     *   <li>创建资源记录并返回</li>
     * </ol>
     *
     * @param file 上传的文件对象
     * @return 创建的资源对象
     * @throws IOException 文件读写异常
     */
    private Resource saveUploadFile(MultipartFile file) throws IOException {
        log.debug("开始保存上传文件到磁盘 [fileName={}]", file.getOriginalFilename());

        // 生成存储路径
        String relativePath = generateStoragePath(file.getOriginalFilename());
        Path fullPath = Paths.get(pndProperties.getBasicResourcePath(), relativePath).normalize();

        // 确保目录存在
        Path parentDir = fullPath.getParent();
        if (!Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
            log.debug("创建存储目录 [path={}]", parentDir);
        }

        // 写入文件
        Files.copy(file.getInputStream(), fullPath);
        log.info("文件保存成功 [fileName={}, path={}, size={}]",
                file.getOriginalFilename(), relativePath, file.getSize());

        // 创建资源记录
        Resource resource = Resource.builder()
                .size(file.getSize())
                .path(relativePath)
                .link(1)
                .build();
        resourceMapper.save(resource);

        log.debug("资源记录创建成功 [resourceId={}, path={}]", resource.getId(), relativePath);

        return resource;
    }

    /**
     * 生成文件存储路径。
     * 格式：年/月/UUID.扩展名
     *
     * <p>优势：
     * <ul>
     *   <li>按月份分目录，便于管理和归档</li>
     *   <li>使用UUID确保唯一性，避免文件名冲突</li>
     *   <li>保留原始扩展名，便于识别文件类型</li>
     * </ul>
     *
     * @param originalName 原始文件名
     * @return 相对存储路径
     */
    private String generateStoragePath(String originalName) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int year = calendar.get(java.util.Calendar.YEAR);
        int month = calendar.get(java.util.Calendar.MONTH) + 1;

        // 提取文件扩展名
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }

        // 生成UUID作为文件名
        String uuid = UUID.randomUUID().toString().replace("-", "");

        // 构建路径：年/月/UUID.扩展名
        return String.format("%04d/%02d/%s%s", year, month, uuid, extension);
    }
}
