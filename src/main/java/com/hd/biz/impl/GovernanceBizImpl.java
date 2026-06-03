package com.hd.biz.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hd.biz.GovernanceBiz;
import com.hd.biz.RecycleBinBiz;
import com.hd.common.enums.FileTypeEnum;
import com.hd.dao.entity.DuplicateGroup;
import com.hd.dao.entity.DuplicateRecord;
import com.hd.dao.entity.File;
import com.hd.dao.entity.RecycleBin;
import com.hd.dao.entity.Resource;
import com.hd.dao.service.DuplicateGroupDataService;
import com.hd.dao.service.DuplicateRecordDataService;
import com.hd.dao.service.RecycleBinDataService;
import com.hd.dao.service.ResourceDataService;
import com.hd.dao.service.FileDataService;
import com.hd.model.dto.StorageAnalysisDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GovernanceBizImpl implements GovernanceBiz {

    @Autowired
    private FileDataService fileDataService;

    @Autowired
    private ResourceDataService resourceDataService;

    @Autowired
    private DuplicateGroupDataService duplicateGroupDataService;

    @Autowired
    private DuplicateRecordDataService duplicateRecordDataService;

    @Autowired
    private RecycleBinBiz recycleBinBiz;

    @Autowired
    private RecycleBinDataService recycleBinDataService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<DuplicateGroup> scanAndGetDuplicates() {
        // Clear previous records
        duplicateRecordDataService.remove(new LambdaQueryWrapper<>());
        duplicateGroupDataService.remove(new LambdaQueryWrapper<>());

        // Group by resource_id having count > 1
        QueryWrapper<File> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("resource_id", "count(1) as fileCount");
        queryWrapper.lambda()
                .eq(File::getIsDeleted, 0)
                .isNotNull(File::getResourceId)
                .groupBy(File::getResourceId)
                .having("count(1) > 1");

        List<Map<String, Object>> duplicateMaps = fileDataService.listMaps(queryWrapper);
        if (duplicateMaps == null || duplicateMaps.isEmpty()) {
            return Collections.emptyList();
        }

        List<DuplicateGroup> groups = new ArrayList<>();
        List<DuplicateRecord> records = new ArrayList<>();

        for (Map<String, Object> map : duplicateMaps) {
            Long resourceId = ((Number) map.get("resource_id")).longValue();
            Integer fileCount = ((Number) map.get("fileCount")).intValue();

            Resource resource = resourceDataService.getById(resourceId);
            if (resource == null) continue;

            DuplicateGroup group = DuplicateGroup.builder()
                    .md5(resource.getMd5())
                    .size(resource.getSize())
                    .fileCount(fileCount)
                    .build();
            duplicateGroupDataService.save(group);
            groups.add(group);

            // Find all files with this resource_id
            List<File> files = fileDataService.lambdaQuery()
                    .eq(File::getResourceId, resourceId)
                    .eq(File::getIsDeleted, 0)
                    .list();

            List<DuplicateRecord> groupRecords = new ArrayList<>();
            for (File file : files) {
                DuplicateRecord record = DuplicateRecord.builder()
                        .groupId(group.getId())
                        .fileId(file.getId())
                        .fileName(file.getFileName())
                        .path(getFullPath(file))
                        .build();
                records.add(record);
                groupRecords.add(record);
            }
            group.setFiles(groupRecords);
        }

        if (!records.isEmpty()) {
            duplicateRecordDataService.saveBatch(records);
        }

        return groups;
    }

    private String getFullPath(File file) {
        StringBuilder path = new StringBuilder("/" + file.getFileName());
        Long parentId = file.getParentId();
        while (parentId != null && parentId != 0L) {
            File parent = fileDataService.getById(parentId);
            if (parent != null) {
                path.insert(0, "/" + parent.getFileName());
                parentId = parent.getParentId();
            } else {
                break;
            }
        }
        return path.toString();
    }

    @Override
    public List<File> getLargeFiles() {
        LambdaQueryWrapper<File> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(File::getIsDeleted, 0)
                .isNotNull(File::getSize)
                .ne(File::getType, FileTypeEnum.FOLDER.toString())
                .orderByDesc(File::getSize)
                .last("LIMIT 100");
        return fileDataService.list(queryWrapper);
    }

    @Override
    public List<File> getEmptyDirectories() {
        LambdaQueryWrapper<File> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(File::getType, FileTypeEnum.FOLDER.toString())
                .eq(File::getIsDeleted, 0)
                .notInSql(File::getId, "SELECT parent_id FROM file WHERE parent_id IS NOT NULL AND is_deleted = 0");
                
        return fileDataService.list(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void smartCleanup() {
        List<DuplicateGroup> groups = scanAndGetDuplicates();
        for (DuplicateGroup group : groups) {
            cleanupDuplicateGroupInternal(group);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cleanupGroup(Long groupId) {
        DuplicateGroup group = duplicateGroupDataService.getById(groupId);
        if (group != null) {
            List<DuplicateRecord> records = duplicateRecordDataService.list(
                    new LambdaQueryWrapper<DuplicateRecord>().eq(DuplicateRecord::getGroupId, groupId)
            );
            group.setFiles(records);
            cleanupDuplicateGroupInternal(group);
        }
    }

    @Override
    public StorageAnalysisDTO getStorageAnalysis() {
        // 1. 总已用空间和文件数
        QueryWrapper<File> totalQuery = new QueryWrapper<>();
        totalQuery.select("COALESCE(SUM(size), 0) as totalSize", "COUNT(*) as totalFileCount");
        totalQuery.lambda()
                .eq(File::getIsDeleted, 0)
                .ne(File::getType, FileTypeEnum.FOLDER.toString());
        Map<String, Object> totalResult = fileDataService.listMaps(totalQuery).get(0);
        Long totalSize = ((Number) totalResult.get("totalSize")).longValue();
        Integer totalFileCount = ((Number) totalResult.get("totalFileCount")).intValue();

        // 2. 按类型分布
        QueryWrapper<File> typeQuery = new QueryWrapper<>();
        typeQuery.select("type", "COUNT(*) as count", "COALESCE(SUM(size), 0) as size");
        typeQuery.lambda()
                .eq(File::getIsDeleted, 0)
                .ne(File::getType, FileTypeEnum.FOLDER.toString())
                .groupBy(File::getType);
        List<Map<String, Object>> typeResults = fileDataService.listMaps(typeQuery);
        List<StorageAnalysisDTO.TypeBreakdown> typeBreakdown = new ArrayList<>();
        for (Map<String, Object> row : typeResults) {
            String type = (String) row.get("type");
            Long size = ((Number) row.get("size")).longValue();
            Integer count = ((Number) row.get("count")).intValue();
            double percent = totalSize > 0 ? (double) size / totalSize * 100 : 0;
            typeBreakdown.add(StorageAnalysisDTO.TypeBreakdown.builder()
                    .type(type)
                    .label(getTypeLabel(type))
                    .size(size)
                    .count(count)
                    .percent(Math.round(percent * 100.0) / 100.0)
                    .build());
        }
        typeBreakdown.sort((a, b) -> Long.compare(b.getSize(), a.getSize()));

        // 3. 目录占用排行 Top10
        QueryWrapper<File> dirQuery = new QueryWrapper<>();
        dirQuery.select("parent_id", "SUM(size) as total_size", "COUNT(*) as file_count");
        dirQuery.lambda()
                .eq(File::getIsDeleted, 0)
                .ne(File::getType, FileTypeEnum.FOLDER.toString())
                .isNotNull(File::getParentId)
                .groupBy(File::getParentId);
        dirQuery.orderByDesc("total_size");
        dirQuery.last("LIMIT 10");
        List<Map<String, Object>> dirResults = fileDataService.listMaps(dirQuery);
        List<StorageAnalysisDTO.DirRanking> topDirs = new ArrayList<>();
        for (Map<String, Object> row : dirResults) {
            Long parentId = ((Number) row.get("parent_id")).longValue();
            Long size = ((Number) row.get("total_size")).longValue();
            Integer fileCount = ((Number) row.get("file_count")).intValue();
            File parentDir = fileDataService.getById(parentId);
            String dirName = parentDir != null ? parentDir.getFileName() : "根目录";
            topDirs.add(StorageAnalysisDTO.DirRanking.builder()
                    .dirId(parentId)
                    .dirName(dirName)
                    .path("/" + dirName)
                    .size(size)
                    .fileCount(fileCount)
                    .build());
        }

        // 4. 大文件排行 Top20 (复用已有方法)
        List<File> largeFiles = getLargeFiles().stream().limit(20).collect(Collectors.toList());
        List<StorageAnalysisDTO.FileRanking> topFiles = largeFiles.stream()
                .map(f -> StorageAnalysisDTO.FileRanking.builder()
                        .fileId(f.getId())
                        .fileName(f.getFileName())
                        .size(f.getSize())
                        .type(f.getType())
                        .updateTime(f.getUpdateTime())
                        .build())
                .collect(Collectors.toList());

        // 5. 可清理空间估算
        // 回收站大小
        List<RecycleBin> recycleBinList = recycleBinDataService.list();
        Long recycleBinSize = 0L;
        for (RecycleBin rb : recycleBinList) {
            File file = fileDataService.selectByIdWithDeleted(rb.getFileId());
            if (file != null && file.getSize() != null) {
                recycleBinSize += file.getSize();
            }
        }

        // 重复文件大小
        List<DuplicateGroup> duplicateGroups = duplicateGroupDataService.list();
        Long duplicateSize = 0L;
        for (DuplicateGroup group : duplicateGroups) {
            if (group.getFileCount() > 1 && group.getSize() != null) {
                duplicateSize += (group.getFileCount() - 1) * group.getSize();
            }
        }

        // 空目录数
        Integer emptyDirCount = getEmptyDirectories().size();

        StorageAnalysisDTO.CleanableEstimate cleanable = StorageAnalysisDTO.CleanableEstimate.builder()
                .recycleBinSize(recycleBinSize)
                .recycleBinCount(recycleBinList.size())
                .duplicateSize(duplicateSize)
                .duplicateGroupCount(duplicateGroups.size())
                .emptyDirCount(emptyDirCount)
                .build();

        // 6. 清理建议
        List<StorageAnalysisDTO.CleanupSuggestion> suggestions = new ArrayList<>();
        if (recycleBinList.size() > 0) {
            suggestions.add(StorageAnalysisDTO.CleanupSuggestion.builder()
                    .type("recycle_bin")
                    .title("回收站有 " + recycleBinList.size() + " 个文件可清理")
                    .description("可释放约 " + formatSize(recycleBinSize) + " 空间")
                    .recoverableSize(recycleBinSize)
                    .actionPath("/governance/recycle")
                    .build());
        }
        if (duplicateGroups.size() > 0) {
            suggestions.add(StorageAnalysisDTO.CleanupSuggestion.builder()
                    .type("duplicate")
                    .title("发现 " + duplicateGroups.size() + " 组重复文件")
                    .description("清理后可释放约 " + formatSize(duplicateSize) + " 空间")
                    .recoverableSize(duplicateSize)
                    .actionPath("/governance/cleanup")
                    .build());
        }
        if (emptyDirCount > 0) {
            suggestions.add(StorageAnalysisDTO.CleanupSuggestion.builder()
                    .type("empty_dir")
                    .title("发现 " + emptyDirCount + " 个空目录")
                    .description("可以安全删除这些空目录")
                    .recoverableSize(0L)
                    .actionPath("/governance/cleanup")
                    .build());
        }

        return StorageAnalysisDTO.builder()
                .totalSize(totalSize)
                .totalFileCount(totalFileCount)
                .typeBreakdown(typeBreakdown)
                .topDirs(topDirs)
                .topFiles(topFiles)
                .cleanable(cleanable)
                .suggestions(suggestions)
                .build();
    }

    private String getTypeLabel(String type) {
        Map<String, String> labels = Map.of(
                "VIDEO", "视频",
                "IMAGE", "图片",
                "AUDIO", "音频",
                "DOCUMENT", "文档",
                "COMPRESS", "压缩包",
                "OTHER", "其他"
        );
        return labels.getOrDefault(type, type);
    }

    private String formatSize(Long size) {
        if (size == null || size == 0) return "0 B";
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double value = size.doubleValue();
        while (value >= 1024 && unitIndex < units.length - 1) {
            value /= 1024;
            unitIndex++;
        }
        return String.format("%.1f %s", value, units[unitIndex]);
    }

    private void cleanupDuplicateGroupInternal(DuplicateGroup group) {
        List<DuplicateRecord> records = group.getFiles();
        if (records == null || records.size() <= 1) {
            return;
        }

        // Sort by fileId ascending (lower ID represents older file)
        records.sort(Comparator.comparing(DuplicateRecord::getFileId));

        List<Long> idsToSoftDelete = new ArrayList<>();
        for (int i = 1; i < records.size(); i++) {
            idsToSoftDelete.add(records.get(i).getFileId());
        }

        if (!idsToSoftDelete.isEmpty()) {
            recycleBinBiz.softDelete(idsToSoftDelete);
        }
    }
}
