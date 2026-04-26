package com.hd.biz.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hd.biz.SearchBiz;
import com.hd.common.enums.FileType;
import com.hd.dao.entity.*;
import com.hd.dao.service.*;
import com.hd.model.dto.SearchRequestDto;
import com.hd.model.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 搜索业务实现类。
 * 实现文件搜索、筛选、历史记录等功能。
 * Biz层通过Service访问数据，不直接调用Mapper。
 *
 * @author team-lead
 * @version 1.0
 * @createTime 2026/04/25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchBizImpl implements SearchBiz {

    private final FileDataService fileDataService;
    private final ResourceDataService resourceDataService;
    private final SearchHistoryDataService searchHistoryDataService;
    private final FavoriteDataService favoriteDataService;
    private final FileTagDataService fileTagDataService;
    private final FileTagRelationDataService fileTagRelationDataService;
    private final RecentUseDataService recentUseDataService;
    private final FileRemarkDataService fileRemarkDataService;

    @Override
    public List<FileDetailVo> searchFiles(SearchRequestDto dto) {
        log.info("开始搜索文件 [keyword={}, fileTypes={}]", dto.getKeyword(), dto.getFileTypes());

        int page = dto.getPage() != null ? dto.getPage() : 1;
        int pageSize = dto.getPageSize() != null ? dto.getPageSize() : 20;
        Page<File> pageParam = new Page<>(page, pageSize);

        IPage<File> pageResult = fileDataService.page(pageParam, buildSearchQuery(dto));
        List<File> files = pageResult.getRecords();

        List<FileDetailVo> result = files.stream()
                .map(this::convertToFileDetailVo)
                .collect(Collectors.toList());

        log.info("搜索完成 [keyword={}, resultCount={}]", dto.getKeyword(), result.size());
        return result;
    }

    @Override
    public SearchResultWithCount searchFilesWithCount(SearchRequestDto dto) {
        log.info("开始搜索文件（带总数）[keyword={}, fileTypes={}]", dto.getKeyword(), dto.getFileTypes());

        int page = dto.getPage() != null ? dto.getPage() : 1;
        int pageSize = dto.getPageSize() != null ? dto.getPageSize() : 20;
        Page<File> pageParam = new Page<>(page, pageSize);

        IPage<File> pageResult = fileDataService.page(pageParam, buildSearchQuery(dto));
        List<File> files = pageResult.getRecords();

        List<FileDetailVo> result = files.stream()
                .map(this::convertToFileDetailVo)
                .collect(Collectors.toList());

        long total = pageResult.getTotal();
        int totalPages = (int) Math.ceil((double) total / pageSize);

        log.info("搜索完成（带总数）[keyword={}, resultCount={}, total={}]", dto.getKeyword(), result.size(), total);
        return SearchResultWithCount.builder()
                .files(result)
                .total(total)
                .page(page)
                .pageSize(pageSize)
                .totalPages(totalPages)
                .build();
    }

    private com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<File> buildSearchQuery(SearchRequestDto dto) {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<File> query = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();

        if (StringUtils.hasText(dto.getKeyword())) {
            query.like("file_name", dto.getKeyword());
        }
        if (dto.getFileTypes() != null && !dto.getFileTypes().isEmpty()) {
            query.in("type", dto.getFileTypes());
        }
        if (StringUtils.hasText(dto.getStartDate())) {
            query.ge("update_time", dto.getStartDate());
        }
        if (StringUtils.hasText(dto.getEndDate())) {
            query.le("update_time", dto.getEndDate());
        }
        if (dto.getMinSize() != null) {
            query.ge("size", dto.getMinSize());
        }
        if (dto.getMaxSize() != null) {
            query.le("size", dto.getMaxSize());
        }
        if (StringUtils.hasText(dto.getDirectoryPath())) {
            Long parentId = resolveDirectoryPathToParentId(dto.getDirectoryPath());
            if (parentId != null) {
                if (Boolean.TRUE.equals(dto.getIncludeSubdirectories())) {
                    List<Long> allChildIds = getAllChildDirectoryIds(parentId);
                    query.in("parent_id", allChildIds);
                } else {
                    query.eq("parent_id", parentId);
                }
            }
        }
        if (dto.getTags() != null && !dto.getTags().isEmpty()) {
            List<Long> fileIdsWithTags = getFileIdsByTagIds(dto.getTags());
            if (fileIdsWithTags.isEmpty()) {
                query.eq("id", -1L);
            } else {
                query.in("id", fileIdsWithTags);
            }
        }
        if (Boolean.TRUE.equals(dto.getFavoritesOnly())) {
            List<Long> favoriteResourceIds = getFavoriteResourceIds();
            if (favoriteResourceIds.isEmpty()) {
                query.eq("id", -1L);
            } else {
                query.in("resource_id", favoriteResourceIds);
            }
        }

        // 排序
        String sortBy = dto.getSortBy() != null ? dto.getSortBy() : "updateTime";
        String sortOrder = dto.getSortOrder() != null ? dto.getSortOrder() : "DESC";
        String column = switch (sortBy.toLowerCase()) {
            case "name" -> "file_name";
            case "size" -> "size";
            default -> "update_time";
        };
        if ("ASC".equalsIgnoreCase(sortOrder)) {
            query.orderByAsc(column);
        } else {
            query.orderByDesc(column);
        }

        return query;
    }

    private Long resolveDirectoryPathToParentId(String directoryPath) {
        if (directoryPath == null || directoryPath.trim().isEmpty()) {
            return null;
        }
        File folder = fileDataService.lambdaQuery()
                .eq(File::getFileName, directoryPath.trim())
                .eq(File::getType, FileType.FOLDER.toString())
                .last("LIMIT 1")
                .one();
        return folder != null ? folder.getId() : null;
    }

    private List<Long> getAllChildDirectoryIds(Long parentId) {
        List<Long> ids = new ArrayList<>();
        ids.add(parentId);
        collectChildDirectoryIds(parentId, ids);
        return ids;
    }

    private void collectChildDirectoryIds(Long parentId, List<Long> ids) {
        List<File> children = fileDataService.lambdaQuery()
                .eq(File::getParentId, parentId)
                .eq(File::getType, FileType.FOLDER.toString())
                .list();
        for (File child : children) {
            ids.add(child.getId());
            collectChildDirectoryIds(child.getId(), ids);
        }
    }

    private List<Long> getFileIdsByTagIds(List<Long> tagIds) {
        List<FileTagRelation> relations = fileTagRelationDataService.lambdaQuery()
                .in(FileTagRelation::getTagId, tagIds)
                .list();
        return relations.stream()
                .map(FileTagRelation::getFileId)
                .distinct()
                .collect(Collectors.toList());
    }

    private List<Long> getFavoriteResourceIds() {
        List<Favorite> favorites = favoriteDataService.list();
        return favorites.stream()
                .map(Favorite::getResourceId)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getSearchSuggestions(String keyword) {
        log.info("获取搜索建议 [keyword={}]", keyword);

        if (!StringUtils.hasText(keyword)) {
            return Collections.emptyList();
        }

        Set<String> suggestions = new LinkedHashSet<>();

        // 1. 从历史记录获取建议
        List<SearchHistory> histories = searchHistoryDataService.lambdaQuery()
                .like(SearchHistory::getKeyword, keyword)
                .orderByDesc(SearchHistory::getSearchedAt)
                .last("LIMIT 10")
                .list();
        histories.stream()
                .map(SearchHistory::getKeyword)
                .forEach(suggestions::add);

        // 2. 从文件名获取建议
        List<File> files = fileDataService.lambdaQuery()
                .like(File::getFileName, keyword)
                .isNotNull(File::getResourceId)
                .orderByDesc(File::getUpdateTime)
                .last("LIMIT 10")
                .list();
        files.stream()
                .map(File::getFileName)
                .filter(fileName -> fileName != null && fileName.toLowerCase().contains(keyword.toLowerCase()))
                .forEach(suggestions::add);

        // 3. 从标签名获取建议
        List<FileTag> tags = fileTagDataService.lambdaQuery()
                .like(FileTag::getTagName, keyword)
                .last("LIMIT 5")
                .list();
        tags.stream()
                .map(FileTag::getTagName)
                .forEach(suggestions::add);

        return suggestions.stream()
                .limit(10)
                .collect(Collectors.toList());
    }

    @Override
    public List<HotFilterVo> getHotFilters() {
        log.info("获取热门筛选选项");

        List<HotFilterVo> hotFilters = new ArrayList<>();

        // 按文件类型统计
        for (FileType type : FileType.values()) {
            if (type == FileType.FOLDER) {
                continue;
            }
            long count = fileDataService.lambdaQuery()
                    .eq(File::getType, type.toString())
                    .count();
            if (count > 0) {
                hotFilters.add(HotFilterVo.builder()
                        .filterType("type")
                        .filterValue(type.toString())
                        .displayName(getFileTypeDisplayName(type.toString()))
                        .count(count)
                        .build());
            }
        }

        // 按收藏统计
        long favoriteCount = favoriteDataService.count();
        if (favoriteCount > 0) {
            hotFilters.add(HotFilterVo.builder()
                    .filterType("favorite")
                    .filterValue("true")
                    .displayName("已收藏")
                    .count(favoriteCount)
                    .build());
        }

        // 按标签统计（热门标签）
        List<FileTag> hotTags = fileTagDataService.lambdaQuery()
                .orderByDesc(FileTag::getUpdatedAt)
                .last("LIMIT 5")
                .list();
        for (FileTag tag : hotTags) {
            long tagFileCount = fileTagRelationDataService.lambdaQuery()
                    .eq(FileTagRelation::getTagId, tag.getId())
                    .count();
            if (tagFileCount > 0) {
                hotFilters.add(HotFilterVo.builder()
                        .filterType("tag")
                        .filterValue(String.valueOf(tag.getId()))
                        .displayName(tag.getTagName())
                        .count(tagFileCount)
                        .build());
            }
        }

        // 按大小范围统计
        addSizeRangeHotFilter(hotFilters, "tiny", 0L, 1024L * 1024, "小于 1MB");
        addSizeRangeHotFilter(hotFilters, "small", 1024L * 1024, 10 * 1024L * 1024, "1MB - 10MB");
        addSizeRangeHotFilter(hotFilters, "medium", 10 * 1024L * 1024, 100 * 1024L * 1024, "10MB - 100MB");
        addSizeRangeHotFilter(hotFilters, "large", 100 * 1024L * 1024, 1024L * 1024 * 1024, "100MB - 1GB");

        // 按时间范围统计（最近7天、30天）
        Calendar cal7 = Calendar.getInstance();
        cal7.add(Calendar.DAY_OF_MONTH, -7);
        long recent7Count = fileDataService.lambdaQuery()
                .ge(File::getUpdateTime, cal7.getTime())
                .count();
        if (recent7Count > 0) {
            hotFilters.add(HotFilterVo.builder()
                    .filterType("date")
                    .filterValue("recent7")
                    .displayName("最近7天")
                    .count(recent7Count)
                    .build());
        }

        Calendar cal30 = Calendar.getInstance();
        cal30.add(Calendar.DAY_OF_MONTH, -30);
        long recent30Count = fileDataService.lambdaQuery()
                .ge(File::getUpdateTime, cal30.getTime())
                .count();
        if (recent30Count > 0) {
            hotFilters.add(HotFilterVo.builder()
                    .filterType("date")
                    .filterValue("recent30")
                    .displayName("最近30天")
                    .count(recent30Count)
                    .build());
        }

        // 按数量排序，取前15个
        return hotFilters.stream()
                .sorted((a, b) -> Long.compare(b.getCount(), a.getCount()))
                .limit(15)
                .collect(Collectors.toList());
    }

    private void addSizeRangeHotFilter(List<HotFilterVo> hotFilters, String value, long minSize, long maxSize, String displayName) {
        long count = fileDataService.lambdaQuery()
                .ge(File::getSize, minSize)
                .lt(File::getSize, maxSize)
                .isNotNull(File::getResourceId)
                .count();
        if (count > 0) {
            hotFilters.add(HotFilterVo.builder()
                    .filterType("size")
                    .filterValue(value)
                    .displayName(displayName)
                    .count(count)
                    .build());
        }
    }

    private String getFileTypeDisplayName(String type) {
        return switch (type.toUpperCase()) {
            case "PICTURE" -> "图片";
            case "VIDEO" -> "视频";
            case "AUDIO" -> "音频";
            case "DOCUMENT" -> "文档";
            case "COMPRESS" -> "压缩包";
            case "TEXT" -> "文本";
            case "CODE" -> "代码";
            case "FOLDER" -> "文件夹";
            default -> type;
        };
    }

    private String buildFullFilePath(File file) {
        if (file == null) {
            return null;
        }
        StringBuilder path = new StringBuilder();
        buildPathRecursive(file, path);
        return path.toString();
    }

    private void buildPathRecursive(File file, StringBuilder path) {
        if (file.getParentId() != null) {
            File parent = fileDataService.getById(file.getParentId());
            if (parent != null) {
                buildPathRecursive(parent, path);
                path.append("/");
            }
        }
        path.append(file.getFileName());
    }

    private String buildParentPath(Long parentId) {
        if (parentId == null) {
            return "/";
        }
        File parent = fileDataService.getById(parentId);
        if (parent == null) {
            return "/";
        }
        return buildFullFilePath(parent);
    }

    @Override
    public List<SearchHistoryVo> getSearchHistory(int limit) {
        log.info("获取搜索历史 [limit={}]", limit);

        List<SearchHistory> histories = searchHistoryDataService.lambdaQuery()
                .orderByDesc(SearchHistory::getSearchedAt)
                .last("LIMIT " + limit)
                .list();

        return histories.stream()
                .map(this::convertToSearchHistoryVo)
                .collect(Collectors.toList());
    }

    @Override
    public void saveSearchHistory(String keyword, String searchType, String params) {
        if (!StringUtils.hasText(keyword)) {
            return;
        }

        SearchHistory history = SearchHistory.builder()
                .keyword(keyword.trim())
                .searchType(searchType != null ? searchType : "FILE")
                .searchParams(params)
                .searchedAt(new Date())
                .build();

        searchHistoryDataService.save(history);
        log.debug("保存搜索历史 [keyword={}]", keyword);
    }

    @Override
    public void deleteSearchHistory(Long id) {
        searchHistoryDataService.removeById(id);
        log.info("删除搜索历史 [id={}]", id);
    }

    @Override
    public void clearSearchHistory() {
        searchHistoryDataService.remove(null);
        log.info("清空搜索历史");
    }

    @Override
    public FileDetailVo getFileDetail(Long fileId) {
        log.info("获取文件详情 [fileId={}]", fileId);

        File file = fileDataService.getById(fileId);
        if (file == null) {
            return null;
        }

        return convertToFileDetailVo(file);
    }

    @Override
    public List<FileDetailVo> getSameDirectoryFiles(Long fileId, int limit) {
        log.info("获取同目录文件 [fileId={}, limit={}]", fileId, limit);

        File file = fileDataService.getById(fileId);
        if (file == null || file.getParentId() == null) {
            return Collections.emptyList();
        }

        List<File> files = fileDataService.lambdaQuery()
                .eq(File::getParentId, file.getParentId())
                .ne(fileId != null, File::getId, fileId)
                .orderByDesc(File::getUpdateTime)
                .last("LIMIT " + limit)
                .list();

        return files.stream()
                .map(this::convertToFileDetailVo)
                .collect(Collectors.toList());
    }

    @Override
    public List<FileDetailVo> getSameTypeFiles(Long fileId, int limit) {
        log.info("获取同类型文件 [fileId={}, limit={}]", fileId, limit);

        File file = fileDataService.getById(fileId);
        if (file == null || file.getType() == null) {
            return Collections.emptyList();
        }

        List<File> files = fileDataService.lambdaQuery()
                .eq(File::getType, file.getType())
                .ne(fileId != null, File::getId, fileId)
                .orderByDesc(File::getUpdateTime)
                .last("LIMIT " + limit)
                .list();

        return files.stream()
                .map(this::convertToFileDetailVo)
                .collect(Collectors.toList());
    }

    @Override
    public List<FileDetailVo> getRecentUploads(int limit) {
        log.info("获取最近上传文件 [limit={}]", limit);

        List<File> files = fileDataService.lambdaQuery()
                .isNotNull(File::getResourceId)
                .orderByDesc(File::getCreateTime)
                .last("LIMIT " + limit)
                .list();

        return files.stream()
                .map(this::convertToFileDetailVo)
                .collect(Collectors.toList());
    }

    private FileDetailVo convertToFileDetailVo(File file) {
        FileDetailVo.FileDetailVoBuilder builder = FileDetailVo.builder()
                .fileId(file.getId())
                .fileName(file.getFileName())
                .type(file.getType())
                .size(file.getSize())
                .parentId(file.getParentId())
                .updateTime(file.getUpdateTime() != null ? file.getUpdateTime().toString() : null);

        // 获取资源信息
        if (file.getResourceId() != null) {
            Resource resource = resourceDataService.getById(file.getResourceId());
            if (resource != null) {
                builder.resourceId(resource.getId())
                        .previewUrl("/v1/preview/" + file.getType().toLowerCase() + "/" + resource.getId());
            }

            // 检查收藏状态
            builder.isFavorite(favoriteDataService.lambdaQuery()
                    .eq(Favorite::getResourceId, file.getResourceId())
                    .count() > 0);

            // 获取文件标签
            List<TagVo> tags = getTagsForFile(file.getId());
            builder.tags(tags);

            // 获取预览状态
            builder.previewStatus(getPreviewStatus(file));
        }

        // 获取扩展名
        if (file.getFileName() != null && file.getFileName().contains(".")) {
            builder.extension(file.getFileName().substring(file.getFileName().lastIndexOf(".") + 1));
        }

        // 统计同目录文件数
        builder.sameDirCount(Math.toIntExact(fileDataService.lambdaQuery()
                .eq(File::getParentId, file.getParentId())
                .count()));

        // 统计同类型文件数
        builder.sameTypeCount(Math.toIntExact(fileDataService.lambdaQuery()
                .eq(File::getType, file.getType())
                .count()));

        // 统计最近关联文件数（7天内同类型文件）
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -7);
        builder.recentRelatedCount(Math.toIntExact(fileDataService.lambdaQuery()
                .eq(File::getType, file.getType())
                .ge(File::getCreateTime, cal.getTime())
                .count()));

        // 获取文件备注
        if (file.getResourceId() != null) {
            FileRemark remark = fileRemarkDataService.lambdaQuery()
                    .eq(FileRemark::getResourceId, file.getResourceId())
                    .one();
            if (remark != null && remark.getRemarkContent() != null) {
                builder.remark(remark.getRemarkContent());
            }
        }

        // 构建完整文件路径
        String fullPath = buildFullFilePath(file);
        builder.filePath(fullPath);

        // 构建父目录路径
        if (file.getParentId() != null) {
            String parentPath = buildParentPath(file.getParentId());
            builder.parentPath(parentPath);
        }

        // 获取最近访问时间
        if (file.getResourceId() != null) {
            RecentUse recentUse = recentUseDataService.lambdaQuery()
                    .eq(RecentUse::getResourceId, file.getResourceId())
                    .orderByDesc(RecentUse::getUsedAt)
                    .last("LIMIT 1")
                    .one();
            if (recentUse != null && recentUse.getUsedAt() != null) {
                builder.lastAccessTime(recentUse.getUsedAt().toString());
            }
        }

        return builder.build();
    }

    private List<TagVo> getTagsForFile(Long fileId) {
        List<FileTagRelation> relations = fileTagRelationDataService.lambdaQuery()
                .eq(FileTagRelation::getFileId, fileId)
                .list();

        if (relations.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> tagIds = relations.stream()
                .map(FileTagRelation::getTagId)
                .collect(Collectors.toList());

        List<FileTag> tags = fileTagDataService.listByIds(tagIds);

        return tags.stream()
                .map(tag -> TagVo.builder()
                        .id(tag.getId())
                        .tagName(tag.getTagName())
                        .tagColor(tag.getTagColor())
                        .build())
                .collect(Collectors.toList());
    }

    private PreviewStatusVo getPreviewStatus(File file) {
        PreviewStatusVo.PreviewStatusVoBuilder builder = PreviewStatusVo.builder();

        if (file.getResourceId() == null) {
            return builder.canPreview(false)
                    .previewType(PreviewStatusVo.TYPE_NONE)
                    .previewStatus(PreviewStatusVo.STATUS_UNSUPPORTED)
                    .errorMessage("无资源文件")
                    .build();
        }

        String type = file.getType();
        if (type == null) {
            return builder.canPreview(false)
                    .previewType(PreviewStatusVo.TYPE_NONE)
                    .previewStatus(PreviewStatusVo.STATUS_UNSUPPORTED)
                    .errorMessage("未知文件类型")
                    .build();
        }

        switch (type.toUpperCase()) {
            case "PICTURE":
                builder.canPreview(true)
                        .previewType(PreviewStatusVo.TYPE_IMAGE)
                        .previewStatus(PreviewStatusVo.STATUS_READY);
                break;
            case "TEXT":
            case "DOC":
            case "PDF":
            case "CODE":
            case "WEB":
                builder.canPreview(true)
                        .previewType(PreviewStatusVo.TYPE_TEXT)
                        .previewStatus(PreviewStatusVo.STATUS_READY);
                break;
            case "AUDIO":
                builder.canPreview(true)
                        .previewType(PreviewStatusVo.TYPE_AUDIO)
                        .previewStatus(PreviewStatusVo.STATUS_READY);
                break;
            case "VIDEO":
                builder.canPreview(true)
                        .previewType(PreviewStatusVo.TYPE_VIDEO)
                        .previewStatus(PreviewStatusVo.STATUS_READY);
                break;
            default:
                builder.canPreview(false)
                        .previewType(PreviewStatusVo.TYPE_NONE)
                        .previewStatus(PreviewStatusVo.STATUS_UNSUPPORTED)
                        .errorMessage("不支持预览此类型");
        }

        return builder.build();
    }

    private SearchHistoryVo convertToSearchHistoryVo(SearchHistory history) {
        return SearchHistoryVo.builder()
                .id(history.getId())
                .keyword(history.getKeyword())
                .searchType(history.getSearchType())
                .searchedAt(history.getSearchedAt() != null ? history.getSearchedAt().toString() : null)
                .build();
    }
}
