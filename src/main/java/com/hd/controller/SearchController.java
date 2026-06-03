package com.hd.controller;

import com.hd.biz.SearchBiz;
import com.hd.common.HomeDashConstants;
import com.hd.model.dto.SearchRequestDTO;
import com.hd.model.dto.ResponseDTO;
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
 * @author xhx
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
    public ResponseEntity<ResponseDTO> searchFiles(@ModelAttribute SearchRequestDTO dto) {
        log.info("文件搜索请求 [keyword={}, fileTypes={}, page={}, pageSize={}]",
                dto.getKeyword(), dto.getFileTypes(), dto.getPage(), dto.getPageSize());

        if (dto.getKeyword() != null && !dto.getKeyword().trim().isEmpty()) {
            searchBiz.saveSearchHistory(dto.getKeyword(), "FILE", null);
        }

        List<FileDetailVO> results = searchBiz.searchFiles(dto);
        log.info("文件搜索完成 [keyword={}, resultCount={}]", dto.getKeyword(), results.size());
        return ResponseEntity.ok(ResponseDTO.success(results));
    }

    /**
     * 搜索文件（带总数）。
     *
     * @param dto 搜索请求参数
     * @return 搜索结果列表及总数
     */
    @GetMapping("/search/with-count")
    public ResponseEntity<ResponseDTO> searchFilesWithCount(@ModelAttribute SearchRequestDTO dto) {
        log.info("文件搜索请求（带总数）[keyword={}, page={}, pageSize={}]",
                dto.getKeyword(), dto.getPage(), dto.getPageSize());

        if (dto.getKeyword() != null && !dto.getKeyword().trim().isEmpty()) {
            searchBiz.saveSearchHistory(dto.getKeyword(), "FILE", null);
        }

        SearchResultWithCount result = searchBiz.searchFilesWithCount(dto);
        log.info("文件搜索完成（带总数）[keyword={}, resultCount={}, total={}]",
                dto.getKeyword(), result.getFiles().size(), result.getTotal());
        return ResponseEntity.ok(ResponseDTO.success(result));
    }

    /**
     * 获取搜索建议。
     *
     * @param keyword 关键词前缀
     * @return 建议列表
     */
    @GetMapping("/search/suggestions")
    public ResponseEntity<ResponseDTO> getSearchSuggestions(@RequestParam String keyword) {
        log.info("获取搜索建议 [keyword={}]", keyword);
        List<String> suggestions = searchBiz.getSearchSuggestions(keyword);
        return ResponseEntity.ok(ResponseDTO.success(suggestions));
    }

    /**
     * 获取热门筛选选项。
     *
     * @return 热门筛选列表
     */
    @GetMapping("/search/hot-filters")
    public ResponseEntity<ResponseDTO> getHotFilters() {
        log.info("获取热门筛选选项请求");
        List<HotFilterVO> hotFilters = searchBiz.getHotFilters();
        return ResponseEntity.ok(ResponseDTO.success(hotFilters));
    }

    /**
     * 获取搜索历史。
     *
     * @param limit 返回数量限制，默认10条
     * @return 搜索历史列表
     */
    @GetMapping("/search/history")
    public ResponseEntity<ResponseDTO> getSearchHistory(
            @RequestParam(defaultValue = "10") Integer limit) {
        log.info("获取搜索历史请求 [limit={}]", limit);
        List<SearchHistoryVO> history = searchBiz.getSearchHistory(limit);
        return ResponseEntity.ok(ResponseDTO.success(history));
    }

    /**
     * 删除单条搜索历史。
     *
     * @param id 历史记录ID
     * @return 操作结果
     */
    @PostMapping("/search/history/{id}/delete")
    public ResponseEntity<ResponseDTO> deleteSearchHistory(@PathVariable Long id) {
        log.info("删除搜索历史 [id={}]", id);
        searchBiz.deleteSearchHistory(id);
        return ResponseEntity.ok(ResponseDTO.success());
    }

    /**
     * 清空全部搜索历史。
     *
     * @return 操作结果
     */
    @PostMapping("/search/history/clear")
    public ResponseEntity<ResponseDTO> clearSearchHistory() {
        log.info("清空搜索历史请求");
        searchBiz.clearSearchHistory();
        return ResponseEntity.ok(ResponseDTO.success());
    }

    /**
     * 获取文件详情。
     *
     * @param fileId 文件ID
     * @return 文件详情
     */
    @GetMapping("/search/file/{fileId}")
    public ResponseEntity<ResponseDTO> getFileDetail(@PathVariable Long fileId) {
        log.info("获取文件详情请求 [fileId={}]", fileId);
        FileDetailVO detail = searchBiz.getFileDetail(fileId);
        return ResponseEntity.ok(ResponseDTO.success(detail));
    }

    /**
     * 获取同目录文件。
     *
     * @param fileId 文件ID
     * @param limit 返回数量限制
     * @return 同目录文件列表
     */
    @GetMapping("/search/file/{fileId}/same-directory")
    public ResponseEntity<ResponseDTO> getSameDirectoryFiles(
            @PathVariable Long fileId,
            @RequestParam(defaultValue = "10") Integer limit) {
        log.info("获取同目录文件请求 [fileId={}, limit={}]", fileId, limit);
        List<FileDetailVO> files = searchBiz.getSameDirectoryFiles(fileId, limit);
        return ResponseEntity.ok(ResponseDTO.success(files));
    }

    /**
     * 获取同类型文件。
     *
     * @param fileId 文件ID
     * @param limit 返回数量限制
     * @return 同类型文件列表
     */
    @GetMapping("/search/file/{fileId}/same-type")
    public ResponseEntity<ResponseDTO> getSameTypeFiles(
            @PathVariable Long fileId,
            @RequestParam(defaultValue = "10") Integer limit) {
        log.info("获取同类型文件请求 [fileId={}, limit={}]", fileId, limit);
        List<FileDetailVO> files = searchBiz.getSameTypeFiles(fileId, limit);
        return ResponseEntity.ok(ResponseDTO.success(files));
    }

    /**
     * 获取最近上传文件。
     *
     * @param limit 返回数量限制，默认10
     * @return 最近上传文件列表
     */
    @GetMapping("/search/recent-uploads")
    public ResponseEntity<ResponseDTO> getRecentUploads(
            @RequestParam(defaultValue = "10") Integer limit) {
        log.info("获取最近上传文件请求 [limit={}]", limit);
        List<FileDetailVO> files = searchBiz.getRecentUploads(limit);
        return ResponseEntity.ok(ResponseDTO.success(files));
    }
}
