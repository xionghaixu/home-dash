package com.hd.biz.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hd.biz.SearchBiz;
import com.hd.common.enums.FileTypeEnum;
import com.hd.dao.entity.*;
import com.hd.dao.service.*;
import com.hd.model.dto.SearchRequestDTO;
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
 * @author xhx
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
    public List<FileDetailVO> searchFiles(SearchRequestDTO dto) {
        log.info("开始搜索文件 [keyword={}, fileTypes={}]", dto.getKeyword(), dto.getFileTypes());

        int page = dto.getPage() != null ? dto.getPage() : 1;
        int pageSize = dto.getPageSize() != null ? dto.getPageSize() : 20;
        Page<File> pageParam = new Page<>(page, pageSize);

        IPage<File> pageResult = fileDataService.page(pageParam, buildSearchQuery(dto));
        List<File> files = pageResult.getRecords();

        Map<Long, Resource> resourceMap = preloadResources(files);
        Map<Long, Boolean> favoriteMap = preloadFavorites(files);
        Map<Long, List<TagVO>> tagsMap = preloadTags(files);
        Map<Long, String> remarkMap = preloadRemarks(files);
        Map<Long, Long> sameDirCountMap = preloadSameDirCounts(files);
        Map<String, Long> sameTypeCountMap = preloadSameTypeCounts(files);
        Map<String, Long> recentRelatedCountMap = preloadRecentRelatedCounts(files);
        Map<Long, String> lastAccessTimeMap = preloadLastAccessTimes(files);
        Map<Long, String> pathCache = new HashMap<>();

        List<FileDetailVO> result = files.stream()
                .map(f -> convertToFileDetailVO(f, resourceMap, favoriteMap, tagsMap, remarkMap,
                        sameDirCountMap, sameTypeCountMap, recentRelatedCountMap, lastAccessTimeMap, pathCache))
                .collect(Collectors.toList());

        log.info("搜索完成 [keyword={}, resultCount={}]", dto.getKeyword(), result.size());
        return result;
    }

    @Override
    public SearchResultWithCount searchFilesWithCount(SearchRequestDTO dto) {
        log.info("开始搜索文件（带总数）[keyword={}, fileTypes={}]", dto.getKeyword(), dto.getFileTypes());

        int page = dto.getPage() != null ? dto.getPage() : 1;
        int pageSize = dto.getPageSize() != null ? dto.getPageSize() : 20;
        Page<File> pageParam = new Page<>(page, pageSize);

        IPage<File> pageResult = fileDataService.page(pageParam, buildSearchQuery(dto));
        List<File> files = pageResult.getRecords();

        Map<Long, Resource> resourceMap = preloadResources(files);
        Map<Long, Boolean> favoriteMap = preloadFavorites(files);
        Map<Long, List<TagVO>> tagsMap = preloadTags(files);
        Map<Long, String> remarkMap = preloadRemarks(files);
        Map<Long, Long> sameDirCountMap = preloadSameDirCounts(files);
        Map<String, Long> sameTypeCountMap = preloadSameTypeCounts(files);
        Map<String, Long> recentRelatedCountMap = preloadRecentRelatedCounts(files);
        Map<Long, String> lastAccessTimeMap = preloadLastAccessTimes(files);
        Map<Long, String> pathCache = new HashMap<>();

        List<FileDetailVO> result = files.stream()
                .map(f -> convertToFileDetailVO(f, resourceMap, favoriteMap, tagsMap, remarkMap,
                        sameDirCountMap, sameTypeCountMap, recentRelatedCountMap, lastAccessTimeMap, pathCache))
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

    private LambdaQueryWrapper<File> buildSearchQuery(SearchRequestDTO dto) {
        LambdaQueryWrapper<File> query = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(dto.getKeyword())) {
            String keyword = dto.getKeyword().trim();
            List<Long> keywordMatchedFileIds = getFileIdsByTagKeyword(keyword);
            query.and(qw -> {
                qw.like(File::getFileName, keyword);
                if (!keywordMatchedFileIds.isEmpty()) {
                    qw.or().in(File::getId, keywordMatchedFileIds);
                }
            });
        }
        if (dto.getFileTypes() != null && !dto.getFileTypes().isEmpty()) {
            query.in(File::getType, dto.getFileTypes());
        }
        if (StringUtils.hasText(dto.getStartDate())) {
            query.ge(File::getUpdateTime, dto.getStartDate());
        }
        if (StringUtils.hasText(dto.getEndDate())) {
            query.le(File::getUpdateTime, dto.getEndDate());
        }
        if (dto.getMinSize() != null) {
            query.ge(File::getSize, dto.getMinSize());
        }
        if (dto.getMaxSize() != null) {
            query.le(File::getSize, dto.getMaxSize());
        }
        if (StringUtils.hasText(dto.getDirectoryPath())) {
            Long parentId = resolveDirectoryPathToParentId(dto.getDirectoryPath());
            if (parentId == null) {
                query.eq(File::getId, -1L);
            } else if (Boolean.TRUE.equals(dto.getIncludeSubdirectories())) {
                List<Long> allChildIds = getAllChildDirectoryIds(parentId);
                query.in(File::getParentId, allChildIds);
            } else {
                query.eq(File::getParentId, parentId);
            }
        }
        if (dto.getTags() != null && !dto.getTags().isEmpty()) {
            List<Long> fileIdsWithTags = getFileIdsByTagIds(dto.getTags());
            if (fileIdsWithTags.isEmpty()) {
                query.eq(File::getId, -1L);
            } else {
                query.in(File::getId, fileIdsWithTags);
            }
        }
        if (Boolean.TRUE.equals(dto.getFavoritesOnly())) {
            List<Long> favoriteResourceIds = getFavoriteResourceIds();
            if (favoriteResourceIds.isEmpty()) {
                query.eq(File::getId, -1L);
            } else {
                query.in(File::getResourceId, favoriteResourceIds);
            }
        }

        // 排序
        String sortBy = dto.getSortBy() != null ? dto.getSortBy() : "updateTime";
        String sortOrder = dto.getSortOrder() != null ? dto.getSortOrder() : "DESC";
        boolean isAsc = "ASC".equalsIgnoreCase(sortOrder);
        
        switch (sortBy.toLowerCase()) {
            case "name" -> {
                if (isAsc) query.orderByAsc(File::getFileName);
                else query.orderByDesc(File::getFileName);
            }
            case "size" -> {
                if (isAsc) query.orderByAsc(File::getSize);
                else query.orderByDesc(File::getSize);
            }
            case "createtime" -> {
                if (isAsc) query.orderByAsc(File::getCreateTime);
                else query.orderByDesc(File::getCreateTime);
            }
            default -> {
                if (isAsc) query.orderByAsc(File::getUpdateTime);
                else query.orderByDesc(File::getUpdateTime);
            }
        }

        return query;
    }

    private Long resolveDirectoryPathToParentId(String directoryPath) {
        if (directoryPath == null || directoryPath.trim().isEmpty()) {
            return null;
        }
        File folder = fileDataService.lambdaQuery()
                .eq(File::getFileName, directoryPath.trim())
                .eq(File::getType, FileTypeEnum.FOLDER.toString())
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
                .eq(File::getType, FileTypeEnum.FOLDER.toString())
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

    private List<Long> getFileIdsByTagKeyword(String keyword) {
        List<FileTag> tags = fileTagDataService.lambdaQuery()
                .like(FileTag::getTagName, keyword)
                .list();
        if (tags.isEmpty()) {
            return Collections.emptyList();
        }
        return getFileIdsByTagIds(tags.stream().map(FileTag::getId).collect(Collectors.toList()));
    }

    private List<Long> getFileIdsByTagNames(List<String> tagNames) {
        List<FileTag> tags = fileTagDataService.lambdaQuery()
                .in(FileTag::getTagName, tagNames)
                .list();
        if (tags.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> tagIds = tags.stream().map(FileTag::getId).collect(Collectors.toList());
        return getFileIdsByTagIds(tagIds);
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
    public List<HotFilterVO> getHotFilters() {
        log.info("获取热门筛选选项");

        List<HotFilterVO> hotFilters = new ArrayList<>();

        // 按文件类型统计
        for (FileTypeEnum type : FileTypeEnum.values()) {
            if (type == FileTypeEnum.FOLDER) {
                continue;
            }
            long count = fileDataService.lambdaQuery()
                    .eq(File::getType, type.toString())
                    .count();
            if (count > 0) {
                hotFilters.add(HotFilterVO.builder()
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
            hotFilters.add(HotFilterVO.builder()
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
                hotFilters.add(HotFilterVO.builder()
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
            hotFilters.add(HotFilterVO.builder()
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
            hotFilters.add(HotFilterVO.builder()
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

    private void addSizeRangeHotFilter(List<HotFilterVO> hotFilters, String value, long minSize, long maxSize, String displayName) {
        long count = fileDataService.lambdaQuery()
                .ge(File::getSize, minSize)
                .lt(File::getSize, maxSize)
                .isNotNull(File::getResourceId)
                .count();
        if (count > 0) {
            hotFilters.add(HotFilterVO.builder()
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

    private String buildFullFilePath(File file, Map<Long, String> pathCache) {
        if (file == null) {
            return null;
        }
        if (file.getId() != null && pathCache.containsKey(file.getId())) {
            return pathCache.get(file.getId());
        }

        String result;
        if (file.getParentId() != null) {
            String parentPath = getOrComputePath(file.getParentId(), pathCache);
            if (parentPath != null) {
                result = parentPath + "/" + file.getFileName();
            } else {
                result = file.getFileName();
            }
        } else {
            result = file.getFileName();
        }

        if (file.getId() != null) {
            pathCache.put(file.getId(), result);
        }
        return result;
    }

    private String getOrComputePath(Long fileId, Map<Long, String> pathCache) {
        if (pathCache.containsKey(fileId)) {
            return pathCache.get(fileId);
        }
        File file = fileDataService.getById(fileId);
        if (file == null) {
            return null;
        }
        String path = buildFullFilePath(file, pathCache);
        pathCache.put(fileId, path);
        return path;
    }

    private String buildParentPath(Long parentId, Map<Long, String> pathCache) {
        if (parentId == null) {
            return "/";
        }
        String cachedPath = getOrComputePath(parentId, pathCache);
        return cachedPath != null ? cachedPath : "/";
    }

    @Override
    public List<SearchHistoryVO> getSearchHistory(int limit) {
        log.info("获取搜索历史 [limit={}]", limit);

        List<SearchHistory> histories = searchHistoryDataService.lambdaQuery()
                .orderByDesc(SearchHistory::getSearchedAt)
                .last("LIMIT " + limit)
                .list();

        return histories.stream()
                .map(this::convertToSearchHistoryVO)
                .collect(Collectors.toList());
    }

    @Override
    public void saveSearchHistory(String keyword, String searchType, String params) {
        if (!StringUtils.hasText(keyword)) {
            return;
        }

        String trimmedKeyword = keyword.trim();

        // 去重：如果已存在相同关键词的记录，更新其时间戳而非插入新记录
        SearchHistory existing = searchHistoryDataService.lambdaQuery()
                .eq(SearchHistory::getKeyword, trimmedKeyword)
                .last("LIMIT 1")
                .one();

        if (existing != null) {
            existing.setSearchedAt(new Date());
            if (searchType != null) {
                existing.setSearchType(searchType);
            }
            if (params != null) {
                existing.setSearchParams(params);
            }
            searchHistoryDataService.updateById(existing);
            log.debug("更新搜索历史 [keyword={}]", trimmedKeyword);
        } else {
            SearchHistory history = SearchHistory.builder()
                    .keyword(trimmedKeyword)
                    .searchType(searchType != null ? searchType : "FILE")
                    .searchParams(params)
                    .searchedAt(new Date())
                    .build();
            searchHistoryDataService.save(history);
            log.debug("保存搜索历史 [keyword={}]", trimmedKeyword);
        }
    }

    @Override
    public void deleteSearchHistory(Long id) {
        searchHistoryDataService.removeById(id);
        log.info("删除搜索历史 [id={}]", id);
    }

    @Override
    public void clearSearchHistory() {
        searchHistoryDataService.remove(new LambdaQueryWrapper<>());
        log.info("清空搜索历史");
    }

    @Override
    public FileDetailVO getFileDetail(Long fileId) {
        log.info("获取文件详情 [fileId={}]", fileId);

        File file = fileDataService.getById(fileId);
        if (file == null) {
            return null;
        }

        return convertToFileDetailVO(file);
    }

    @Override
    public List<FileDetailVO> getSameDirectoryFiles(Long fileId, int limit) {
        log.info("获取同目录文件 [fileId={}, limit={}]", fileId, limit);

        File file = fileDataService.getById(fileId);
        if (file == null || file.getParentId() == null) {
            return Collections.emptyList();
        }

        List<File> files = fileDataService.lambdaQuery()
                .eq(File::getParentId, file.getParentId())
                .ne(File::getId, fileId)
                .orderByDesc(File::getUpdateTime)
                .last("LIMIT " + limit)
                .list();

        Map<Long, Resource> resourceMap = preloadResources(files);
        Map<Long, Boolean> favoriteMap = preloadFavorites(files);
        Map<Long, List<TagVO>> tagsMap = preloadTags(files);
        Map<Long, String> remarkMap = preloadRemarks(files);
        Map<Long, Long> sameDirCountMap = preloadSameDirCounts(files);
        Map<String, Long> sameTypeCountMap = preloadSameTypeCounts(files);
        Map<String, Long> recentRelatedCountMap = preloadRecentRelatedCounts(files);
        Map<Long, String> lastAccessTimeMap = preloadLastAccessTimes(files);
        Map<Long, String> pathCache = new HashMap<>();

        return files.stream()
                .map(f -> convertToFileDetailVO(f, resourceMap, favoriteMap, tagsMap, remarkMap,
                        sameDirCountMap, sameTypeCountMap, recentRelatedCountMap, lastAccessTimeMap, pathCache))
                .collect(Collectors.toList());
    }

    @Override
    public List<FileDetailVO> getSameTypeFiles(Long fileId, int limit) {
        log.info("获取同类型文件 [fileId={}, limit={}]", fileId, limit);

        File file = fileDataService.getById(fileId);
        if (file == null || file.getType() == null) {
            return Collections.emptyList();
        }

        List<File> files = fileDataService.lambdaQuery()
                .eq(File::getType, file.getType())
                .ne(File::getId, fileId)
                .orderByDesc(File::getUpdateTime)
                .last("LIMIT " + limit)
                .list();

        Map<Long, Resource> resourceMap = preloadResources(files);
        Map<Long, Boolean> favoriteMap = preloadFavorites(files);
        Map<Long, List<TagVO>> tagsMap = preloadTags(files);
        Map<Long, String> remarkMap = preloadRemarks(files);
        Map<Long, Long> sameDirCountMap = preloadSameDirCounts(files);
        Map<String, Long> sameTypeCountMap = preloadSameTypeCounts(files);
        Map<String, Long> recentRelatedCountMap = preloadRecentRelatedCounts(files);
        Map<Long, String> lastAccessTimeMap = preloadLastAccessTimes(files);
        Map<Long, String> pathCache = new HashMap<>();

        return files.stream()
                .map(f -> convertToFileDetailVO(f, resourceMap, favoriteMap, tagsMap, remarkMap,
                        sameDirCountMap, sameTypeCountMap, recentRelatedCountMap, lastAccessTimeMap, pathCache))
                .collect(Collectors.toList());
    }

    @Override
    public List<FileDetailVO> getRecentUploads(int limit) {
        log.info("获取最近上传文件 [limit={}]", limit);

        List<File> files = fileDataService.lambdaQuery()
                .isNotNull(File::getResourceId)
                .orderByDesc(File::getCreateTime)
                .last("LIMIT " + limit)
                .list();

        Map<Long, Resource> resourceMap = preloadResources(files);
        Map<Long, Boolean> favoriteMap = preloadFavorites(files);
        Map<Long, List<TagVO>> tagsMap = preloadTags(files);
        Map<Long, String> remarkMap = preloadRemarks(files);
        Map<Long, Long> sameDirCountMap = preloadSameDirCounts(files);
        Map<String, Long> sameTypeCountMap = preloadSameTypeCounts(files);
        Map<String, Long> recentRelatedCountMap = preloadRecentRelatedCounts(files);
        Map<Long, String> lastAccessTimeMap = preloadLastAccessTimes(files);
        Map<Long, String> pathCache = new HashMap<>();

        return files.stream()
                .map(f -> convertToFileDetailVO(f, resourceMap, favoriteMap, tagsMap, remarkMap,
                        sameDirCountMap, sameTypeCountMap, recentRelatedCountMap, lastAccessTimeMap, pathCache))
                .collect(Collectors.toList());
    }

    /**
     * 单文件转换的便捷方法，用于 getFileDetail 等单文件场景。
     */
    private FileDetailVO convertToFileDetailVO(File file) {
        List<File> singleFile = Collections.singletonList(file);
        return convertToFileDetailVO(file,
                preloadResources(singleFile),
                preloadFavorites(singleFile),
                preloadTags(singleFile),
                preloadRemarks(singleFile),
                preloadSameDirCounts(singleFile),
                preloadSameTypeCounts(singleFile),
                preloadRecentRelatedCounts(singleFile),
                preloadLastAccessTimes(singleFile),
                new HashMap<>());
    }

    /**
     * 批量预加载资源信息。
     * 将 N 次 getById 合并为 1 次 IN 查询。
     */
    private Map<Long, String> preloadLastAccessTimes(List<File> files) {
        if (files == null || files.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> resourceIds = files.stream()
                .map(File::getResourceId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (resourceIds.isEmpty()) {
            return Collections.emptyMap();
        }

        QueryWrapper<RecentUse> query = new QueryWrapper<>();
        query.select("resource_id", "MAX(used_at) as last_used")
                .in("resource_id", resourceIds)
                .groupBy("resource_id");

        List<Map<String, Object>> list = recentUseDataService.listMaps(query);
        Map<Long, String> map = new HashMap<>();
        for (Map<String, Object> row : list) {
            Object rIdObj = row.get("resource_id");
            if (rIdObj == null) {
                rIdObj = row.get("RESOURCE_ID");
            }
            if (rIdObj == null) {
                continue;
            }
            Long resourceId = ((Number) rIdObj).longValue();
            Object lastUsed = row.get("last_used");
            if (lastUsed == null) {
                lastUsed = row.get("LAST_USED");
            }
            if (lastUsed != null) {
                map.put(resourceId, lastUsed.toString());
            }
        }
        return map;
    }

    private Map<Long, Resource> preloadResources(List<File> files) {
        List<Long> resourceIds = files.stream()
                .map(File::getResourceId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (resourceIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return resourceDataService.lambdaQuery()
                .in(Resource::getId, resourceIds)
                .list()
                .stream()
                .collect(Collectors.toMap(Resource::getId, r -> r));
    }

    /**
     * 批量预加载收藏状态。
     * 将 N 次 count 查询合并为 1 次 IN 查询。
     */
    private Map<Long, Boolean> preloadFavorites(List<File> files) {
        List<Long> resourceIds = files.stream()
                .map(File::getResourceId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (resourceIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> favoritedIds = favoriteDataService.lambdaQuery()
                .in(Favorite::getResourceId, resourceIds)
                .list()
                .stream()
                .map(Favorite::getResourceId)
                .collect(Collectors.toSet());
        Map<Long, Boolean> result = new HashMap<>();
        for (Long rid : resourceIds) {
            result.put(rid, favoritedIds.contains(rid));
        }
        return result;
    }

    /**
     * 批量预加载文件标签。
     * 将 N 次（relation查询 + tag查询）合并为 2 次 IN 查询。
     */
    private Map<Long, List<TagVO>> preloadTags(List<File> files) {
        List<Long> fileIds = files.stream()
                .map(File::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (fileIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<FileTagRelation> relations = fileTagRelationDataService.lambdaQuery()
                .in(FileTagRelation::getFileId, fileIds)
                .list();
        if (relations.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> tagIds = relations.stream()
                .map(FileTagRelation::getTagId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, FileTag> tagMap = fileTagDataService.listByIds(tagIds)
                .stream()
                .collect(Collectors.toMap(FileTag::getId, t -> t));

        Map<Long, List<TagVO>> result = new HashMap<>();
        for (FileTagRelation rel : relations) {
            FileTag tag = tagMap.get(rel.getTagId());
            if (tag != null) {
                result.computeIfAbsent(rel.getFileId(), k -> new ArrayList<>())
                        .add(TagVO.builder()
                                .id(tag.getId())
                                .tagName(tag.getTagName())
                                .tagColor(tag.getTagColor())
                                .build());
            }
        }
        return result;
    }

    /**
     * 批量预加载文件备注。
     * 将 N 次查询合并为 1 次 IN 查询。
     */
    private Map<Long, String> preloadRemarks(List<File> files) {
        List<Long> resourceIds = files.stream()
                .map(File::getResourceId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (resourceIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return fileRemarkDataService.lambdaQuery()
                .in(FileRemark::getResourceId, resourceIds)
                .list()
                .stream()
                .filter(r -> r.getRemarkContent() != null)
                .collect(Collectors.toMap(FileRemark::getResourceId, FileRemark::getRemarkContent));
    }

    /**
     * 批量预加载同目录文件数。
     * 将 N 次 count 查询合并为 1 次 IN 查询。
     */
    private Map<Long, Long> preloadSameDirCounts(List<File> files) {
        Set<Long> parentIds = files.stream()
                .map(File::getParentId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toSet());
        if (parentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return fileDataService.lambdaQuery()
                .in(File::getParentId, parentIds)
                .select(File::getParentId)
                .list()
                .stream()
                .collect(Collectors.groupingBy(File::getParentId, Collectors.counting()));
    }

    /**
     * 批量预加载同类型文件数。
     * 将 N 次 count 查询合并为 1 次 IN 查询。
     */
    private Map<String, Long> preloadSameTypeCounts(List<File> files) {
        Set<String> types = files.stream()
                .map(File::getType)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toSet());
        if (types.isEmpty()) {
            return Collections.emptyMap();
        }
        return fileDataService.lambdaQuery()
                .in(File::getType, types)
                .select(File::getType)
                .list()
                .stream()
                .collect(Collectors.groupingBy(File::getType, Collectors.counting()));
    }

    /**
     * 批量预加载最近7天同类型文件数。
     * 将 N 次 count 查询合并为 1 次 IN 查询。
     */
    private Map<String, Long> preloadRecentRelatedCounts(List<File> files) {
        Set<String> types = files.stream()
                .map(File::getType)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toSet());
        if (types.isEmpty()) {
            return Collections.emptyMap();
        }
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -7);
        return fileDataService.lambdaQuery()
                .in(File::getType, types)
                .ge(File::getCreateTime, cal.getTime())
                .select(File::getType)
                .list()
                .stream()
                .collect(Collectors.groupingBy(File::getType, Collectors.counting()));
    }

    private FileDetailVO convertToFileDetailVO(File file,
                                                Map<Long, Resource> resourceMap,
                                                Map<Long, Boolean> favoriteMap,
                                                Map<Long, List<TagVO>> tagsMap,
                                                Map<Long, String> remarkMap,
                                                Map<Long, Long> sameDirCountMap,
                                                Map<String, Long> sameTypeCountMap,
                                                Map<String, Long> recentRelatedCountMap,
                                                Map<Long, String> lastAccessTimeMap,
                                                Map<Long, String> pathCache) {
        FileDetailVO.FileDetailVOBuilder builder = FileDetailVO.builder()
                .fileId(file.getId())
                .fileName(file.getFileName())
                .type(file.getType())
                .size(file.getSize())
                .parentId(file.getParentId())
                .updateTime(file.getUpdateTime() != null ? file.getUpdateTime().toString() : null);

        // 从预加载 map 中获取资源信息
        if (file.getResourceId() != null) {
            Resource resource = resourceMap.get(file.getResourceId());
            if (resource != null) {
                builder.resourceId(resource.getId())
                        .previewUrl(buildPreviewUrl(file, resource.getId()));
            }

            // 从预加载 map 中获取收藏状态
            builder.isFavorite(Boolean.TRUE.equals(favoriteMap.get(file.getResourceId())));

            // 从预加载 map 中获取标签
            List<TagVO> tags = tagsMap.getOrDefault(file.getId(), Collections.emptyList());
            builder.tags(tags);

            // 获取预览状态
            builder.previewStatus(getPreviewStatus(file));
        }

        // 获取扩展名
        if (file.getFileName() != null && file.getFileName().contains(".")) {
            builder.extension(file.getFileName().substring(file.getFileName().lastIndexOf(".") + 1));
        }

        // 统计同目录文件数（从预加载 map 中获取）
        builder.sameDirCount(Math.toIntExact(
                sameDirCountMap.getOrDefault(file.getParentId(), 0L)));

        // 统计同类型文件数（从预加载 map 中获取）
        builder.sameTypeCount(Math.toIntExact(
                sameTypeCountMap.getOrDefault(file.getType(), 0L)));

        // 统计最近关联文件数（从预加载 map 中获取）
        builder.recentRelatedCount(Math.toIntExact(
                recentRelatedCountMap.getOrDefault(file.getType(), 0L)));

        // 从预加载 map 中获取备注
        if (file.getResourceId() != null) {
            String remark = remarkMap.get(file.getResourceId());
            if (remark != null) {
                builder.remark(remark);
            }
        }

        // 构建完整文件路径（使用缓存避免重复查询）
        String fullPath = buildFullFilePath(file, pathCache);
        builder.filePath(fullPath);

        // 构建父目录路径（使用缓存避免重复查询）
        if (file.getParentId() != null) {
            String parentPath = buildParentPath(file.getParentId(), pathCache);
            builder.parentPath(parentPath);
        }

        // 从预加载 map 中获取最近访问时间
        if (file.getResourceId() != null && lastAccessTimeMap != null) {
            String lastAccessTime = lastAccessTimeMap.get(file.getResourceId());
            if (lastAccessTime != null) {
                builder.lastAccessTime(lastAccessTime);
            }
        }

        return builder.build();
    }

    private List<TagVO> getTagsForFile(Long fileId) {
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
                .map(tag -> TagVO.builder()
                        .id(tag.getId())
                        .tagName(tag.getTagName())
                        .tagColor(tag.getTagColor())
                        .build())
                .collect(Collectors.toList());
    }

    private PreviewStatusVO getPreviewStatus(File file) {
        PreviewStatusVO.PreviewStatusVOBuilder builder = PreviewStatusVO.builder();

        if (file.getResourceId() == null) {
            return builder.canPreview(false)
                    .previewType(PreviewStatusVO.TYPE_NONE)
                    .previewStatus(PreviewStatusVO.STATUS_UNSUPPORTED)
                    .errorMessage("无资源文件")
                    .build();
        }

        String type = file.getType();
        if (type == null) {
            return builder.canPreview(false)
                    .previewType(PreviewStatusVO.TYPE_NONE)
                    .previewStatus(PreviewStatusVO.STATUS_UNSUPPORTED)
                    .errorMessage("未知文件类型")
                    .build();
        }

        switch (type.toUpperCase()) {
            case "PICTURE":
                builder.canPreview(true)
                        .previewType(PreviewStatusVO.TYPE_IMAGE)
                        .previewStatus(PreviewStatusVO.STATUS_READY);
                break;
            case "TEXT":
            case "DOC":
            case "PDF":
            case "CODE":
            case "WEB":
                builder.canPreview(true)
                        .previewType(PreviewStatusVO.TYPE_TEXT)
                        .previewStatus(PreviewStatusVO.STATUS_READY);
                break;
            case "AUDIO":
                builder.canPreview(true)
                        .previewType(PreviewStatusVO.TYPE_AUDIO)
                        .previewStatus(PreviewStatusVO.STATUS_READY);
                break;
            case "VIDEO":
                builder.canPreview(false)
                        .previewType(PreviewStatusVO.TYPE_VIDEO)
                        .previewStatus(PreviewStatusVO.STATUS_UNSUPPORTED)
                        .errorMessage("视频预览暂不支持，建议下载后使用本地播放器观看")
                        .suggestion("可尝试下载文件后用 VLC、PotPlayer 等播放器打开，或转换为MP4格式后重试");
                break;
            default:
                builder.canPreview(false)
                        .previewType(PreviewStatusVO.TYPE_NONE)
                        .previewStatus(PreviewStatusVO.STATUS_UNSUPPORTED)
                        .errorMessage("不支持预览此类型");
        }

        return builder.build();
    }

    private String buildPreviewUrl(File file, Long resourceId) {
        if (file == null || resourceId == null || !StringUtils.hasText(file.getType())) {
            return null;
        }

        return switch (file.getType().toUpperCase()) {
            case "PICTURE" -> "/v1/preview/image/" + resourceId + "/thumbnail";
            case "TXT", "TEXT", "DOC", "PDF", "CODE", "WEB" -> "/v1/preview/text/" + resourceId;
            case "AUDIO" -> "/v1/preview/audio/" + resourceId + "/stream";
            default -> null;
        };
    }

    private SearchHistoryVO convertToSearchHistoryVO(SearchHistory history) {
        return SearchHistoryVO.builder()
                .id(history.getId())
                .keyword(history.getKeyword())
                .searchType(history.getSearchType())
                .searchedAt(history.getSearchedAt() != null ? history.getSearchedAt().toString() : null)
                .build();
    }
}
