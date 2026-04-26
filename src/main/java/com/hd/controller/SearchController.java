package com.hd.controller;

import com.hd.biz.SearchBiz;
import com.hd.common.HomeDashConstants;
import com.hd.model.dto.SearchRequestDto;
import com.hd.model.dto.ResponseDto;
import com.hd.model.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 搜索控制器。
 * 提供文件搜索、筛选、历史记录等REST API接口。
 *
 * @author team-lead
 * @version 1.0
 * @createTime 2026/04/25
 */
@Slf4j
@RestController
@RequestMapping(HomeDashConstants.API_VERSION)
@RequiredArgsConstructor
@Validated
public class SearchController {

    private final SearchBiz searchBiz;

    /**
     * 搜索文件。
     * 支持关键词、类型、时间、大小等多种筛选条件。
     *
     * @param dto 搜索请求参数
     * @return 搜索结果列表
     */
    @GetMapping("/search")
    public ResponseEntity<ResponseDto> searchFiles(@ModelAttribute SearchRequestDto dto) {
        log.info("文件搜索请求 [keyword={}, fileTypes={}, page={}, pageSize={}]",
                dto.getKeyword(), dto.getFileTypes(), dto.getPage(), dto.getPageSize());

        if (dto.getKeyword() != null && !dto.getKeyword().trim().isEmpty()) {
            searchBiz.saveSearchHistory(dto.getKeyword(), "FILE", null);
        }

        List<FileDetailVo> results = searchBiz.searchFiles(dto);
        log.info("文件搜索完成 [keyword={}, resultCount={}]", dto.getKeyword(), results.size());
        return ResponseEntity.ok(ResponseDto.success(results));
    }

    /**
     * 搜索文件（带总数）。
     *
     * @param dto 搜索请求参数
     * @return 搜索结果列表及总数
     */
    @GetMapping("/search/with-count")
    public ResponseEntity<ResponseDto> searchFilesWithCount(@ModelAttribute SearchRequestDto dto) {
        log.info("文件搜索请求（带总数）[keyword={}, page={}, pageSize={}]",
                dto.getKeyword(), dto.getPage(), dto.getPageSize());

        if (dto.getKeyword() != null && !dto.getKeyword().trim().isEmpty()) {
            searchBiz.saveSearchHistory(dto.getKeyword(), "FILE", null);
        }

        SearchResultWithCount result = searchBiz.searchFilesWithCount(dto);
        log.info("文件搜索完成（带总数）[keyword={}, resultCount={}, total={}]",
                dto.getKeyword(), result.getFiles().size(), result.getTotal());
        return ResponseEntity.ok(ResponseDto.success(result));
    }

    /**
     * 获取搜索建议。
     *
     * @param keyword 关键词前缀
     * @return 建议列表
     */
    @GetMapping("/search/suggestions")
    public ResponseEntity<ResponseDto> getSearchSuggestions(@RequestParam String keyword) {
        log.info("获取搜索建议 [keyword={}]", keyword);
        List<String> suggestions = searchBiz.getSearchSuggestions(keyword);
        return ResponseEntity.ok(ResponseDto.success(suggestions));
    }

    /**
     * 获取热门筛选选项。
     *
     * @return 热门筛选列表
     */
    @GetMapping("/search/hot-filters")
    public ResponseEntity<ResponseDto> getHotFilters() {
        log.info("获取热门筛选选项请求");
        List<HotFilterVo> hotFilters = searchBiz.getHotFilters();
        return ResponseEntity.ok(ResponseDto.success(hotFilters));
    }

    /**
     * 获取搜索历史。
     *
     * @param limit 返回数量限制，默认10条
     * @return 搜索历史列表
     */
    @GetMapping("/search/history")
    public ResponseEntity<ResponseDto> getSearchHistory(
            @RequestParam(defaultValue = "10") Integer limit) {
        log.info("获取搜索历史请求 [limit={}]", limit);
        List<SearchHistoryVo> history = searchBiz.getSearchHistory(limit);
        return ResponseEntity.ok(ResponseDto.success(history));
    }

    /**
     * 删除单条搜索历史。
     *
     * @param id 历史记录ID
     * @return 操作结果
     */
    @DeleteMapping("/search/history/{id}")
    public ResponseEntity<ResponseDto> deleteSearchHistory(@PathVariable Long id) {
        log.info("删除搜索历史 [id={}]", id);
        searchBiz.deleteSearchHistory(id);
        return ResponseEntity.ok(ResponseDto.success());
    }

    /**
     * 清空全部搜索历史。
     *
     * @return 操作结果
     */
    @DeleteMapping("/search/history")
    public ResponseEntity<ResponseDto> clearSearchHistory() {
        log.info("清空搜索历史请求");
        searchBiz.clearSearchHistory();
        return ResponseEntity.ok(ResponseDto.success());
    }

    /**
     * 获取文件详情。
     *
     * @param fileId 文件ID
     * @return 文件详情
     */
    @GetMapping("/search/file/{fileId}")
    public ResponseEntity<ResponseDto> getFileDetail(@PathVariable Long fileId) {
        log.info("获取文件详情请求 [fileId={}]", fileId);
        FileDetailVo detail = searchBiz.getFileDetail(fileId);
        return ResponseEntity.ok(ResponseDto.success(detail));
    }

    /**
     * 获取同目录文件。
     *
     * @param fileId 文件ID
     * @param limit 返回数量限制
     * @return 同目录文件列表
     */
    @GetMapping("/search/file/{fileId}/same-directory")
    public ResponseEntity<ResponseDto> getSameDirectoryFiles(
            @PathVariable Long fileId,
            @RequestParam(defaultValue = "10") Integer limit) {
        log.info("获取同目录文件请求 [fileId={}, limit={}]", fileId, limit);
        List<FileDetailVo> files = searchBiz.getSameDirectoryFiles(fileId, limit);
        return ResponseEntity.ok(ResponseDto.success(files));
    }

    /**
     * 获取同类型文件。
     *
     * @param fileId 文件ID
     * @param limit 返回数量限制
     * @return 同类型文件列表
     */
    @GetMapping("/search/file/{fileId}/same-type")
    public ResponseEntity<ResponseDto> getSameTypeFiles(
            @PathVariable Long fileId,
            @RequestParam(defaultValue = "10") Integer limit) {
        log.info("获取同类型文件请求 [fileId={}, limit={}]", fileId, limit);
        List<FileDetailVo> files = searchBiz.getSameTypeFiles(fileId, limit);
        return ResponseEntity.ok(ResponseDto.success(files));
    }

    /**
     * 获取最近上传文件。
     *
     * @param limit 返回数量限制，默认10
     * @return 最近上传文件列表
     */
    @GetMapping("/search/recent-uploads")
    public ResponseEntity<ResponseDto> getRecentUploads(
            @RequestParam(defaultValue = "10") Integer limit) {
        log.info("获取最近上传文件请求 [limit={}]", limit);
        List<FileDetailVo> files = searchBiz.getRecentUploads(limit);
        return ResponseEntity.ok(ResponseDto.success(files));
    }
}
