package com.hd.biz;

import com.hd.model.dto.SearchRequestDto;
import com.hd.model.vo.*;
import java.util.List;

/**
 * 搜索业务接口。
 * 定义文件搜索和筛选的相关操作。
 *
 * @author team-lead
 * @version 1.0
 * @createTime 2026/04/25
 */
public interface SearchBiz {

    /**
     * 搜索文件。
     *
     * @param dto 搜索请求参数
     * @return 搜索结果列表
     */
    List<FileDetailVo> searchFiles(SearchRequestDto dto);

    /**
     * 搜索文件（带总数）。
     *
     * @param dto 搜索请求参数
     * @return 搜索结果，包含总数信息
     */
    SearchResultWithCount searchFilesWithCount(SearchRequestDto dto);

    /**
     * 获取搜索建议。
     *
     * @param keyword 关键词前缀
     * @return 建议列表
     */
    List<String> getSearchSuggestions(String keyword);

    /**
     * 获取热门筛选选项。
     *
     * @return 热门筛选列表
     */
    List<HotFilterVo> getHotFilters();

    /**
     * 获取搜索历史。
     *
     * @param limit 返回数量限制
     * @return 搜索历史列表
     */
    List<SearchHistoryVo> getSearchHistory(int limit);

    /**
     * 保存搜索历史。
     *
     * @param keyword 搜索关键词
     * @param searchType 搜索类型
     * @param params 搜索参数
     */
    void saveSearchHistory(String keyword, String searchType, String params);

    /**
     * 删除搜索历史。
     *
     * @param id 历史记录ID
     */
    void deleteSearchHistory(Long id);

    /**
     * 清空搜索历史。
     */
    void clearSearchHistory();

    /**
     * 获取文件详情。
     *
     * @param fileId 文件ID
     * @return 文件详情VO
     */
    FileDetailVo getFileDetail(Long fileId);

    /**
     * 获取同目录文件列表。
     *
     * @param fileId 文件ID
     * @param limit 返回数量限制
     * @return 同目录文件列表
     */
    List<FileDetailVo> getSameDirectoryFiles(Long fileId, int limit);

    /**
     * 获取同类型文件列表。
     *
     * @param fileId 文件ID
     * @param limit 返回数量限制
     * @return 同类型文件列表
     */
    List<FileDetailVo> getSameTypeFiles(Long fileId, int limit);

    /**
     * 获取最近上传文件列表。
     *
     * @param limit 返回数量限制
     * @return 最近上传文件列表
     */
    List<FileDetailVo> getRecentUploads(int limit);
}