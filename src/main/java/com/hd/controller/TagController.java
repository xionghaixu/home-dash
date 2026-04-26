package com.hd.controller;

import com.hd.biz.TagBiz;
import com.hd.common.HomeDashConstants;
import com.hd.model.dto.ResponseDto;
import com.hd.model.dto.TagRequestDto;
import com.hd.model.vo.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 标签控制器。
 * 提供标签CRUD和文件标签关联的REST API接口。
 *
 * @author team-lead
 * @version 1.0
 * @createTime 2026/04/25
 */
@Slf4j
@RestController
@RequestMapping(HomeDashConstants.API_VERSION)
@RequiredArgsConstructor
public class TagController {

    private final TagBiz tagBiz;

    /**
     * 创建标签。
     */
    @PostMapping("/tag")
    public ResponseEntity<ResponseDto> createTag(@Valid @RequestBody TagRequestDto dto) {
        log.info("创建标签请求 [tagName={}]", dto.getTagName());
        TagVo tag = tagBiz.createTag(dto);
        return ResponseEntity.ok(ResponseDto.success(tag));
    }

    /**
     * 更新标签。
     */
    @PutMapping("/tag/{id}")
    public ResponseEntity<ResponseDto> updateTag(@PathVariable Long id, @RequestBody TagRequestDto dto) {
        log.info("更新标签请求 [id={}]", id);
        TagVo tag = tagBiz.updateTag(id, dto);
        return ResponseEntity.ok(ResponseDto.success(tag));
    }

    /**
     * 删除标签。
     */
    @DeleteMapping("/tag/{id}")
    public ResponseEntity<ResponseDto> deleteTag(@PathVariable Long id) {
        log.info("删除标签请求 [id={}]", id);
        boolean result = tagBiz.deleteTag(id);
        return ResponseEntity.ok(ResponseDto.success(result));
    }

    /**
     * 获取标签列表。
     */
    @GetMapping("/tag/list")
    public ResponseEntity<ResponseDto> getTagList() {
        log.info("获取标签列表请求");
        List<TagVo> tags = tagBiz.getTagList();
        return ResponseEntity.ok(ResponseDto.success(tags));
    }

    /**
     * 获取标签详情。
     */
    @GetMapping("/tag/{id}")
    public ResponseEntity<ResponseDto> getTagById(@PathVariable Long id) {
        log.info("获取标签详情请求 [id={}]", id);
        TagVo tag = tagBiz.getTagById(id);
        return ResponseEntity.ok(ResponseDto.success(tag));
    }

    /**
     * 为文件分配标签。
     */
    @PostMapping("/tag/assign")
    public ResponseEntity<ResponseDto> assignTags(@RequestBody TagAssignRequest request) {
        log.info("分配标签请求 [fileId={}, tagIds={}]", request.getFileId(), request.getTagIds());
        BatchOperationResultVo result = tagBiz.assignTagsToFiles(
                List.of(request.getFileId()), request.getTagIds());
        return ResponseEntity.ok(ResponseDto.success(result));
    }

    /**
     * 批量为文件添加标签。
     */
    @PostMapping("/tag/batch/add")
    public ResponseEntity<ResponseDto> batchAddTags(@RequestBody BatchTagRequest request) {
        log.info("批量添加标签请求 [fileIds={}, tagIds={}]", request.getFileIds(), request.getTagIds());
        BatchOperationResultVo result = tagBiz.addTagsToFiles(request.getFileIds(), request.getTagIds());
        return ResponseEntity.ok(ResponseDto.success(result));
    }

    /**
     * 批量移除文件标签。
     */
    @PostMapping("/tag/batch/remove")
    public ResponseEntity<ResponseDto> batchRemoveTags(@RequestBody BatchTagRequest request) {
        log.info("批量移除标签请求 [fileIds={}, tagIds={}]", request.getFileIds(), request.getTagIds());
        BatchOperationResultVo result = tagBiz.removeTagsFromFiles(request.getFileIds(), request.getTagIds());
        return ResponseEntity.ok(ResponseDto.success(result));
    }

    /**
     * 获取文件的所有标签。
     */
    @GetMapping("/tag/file/{fileId}")
    public ResponseEntity<ResponseDto> getTagsForFile(@PathVariable Long fileId) {
        log.info("获取文件标签请求 [fileId={}]", fileId);
        List<TagVo> tags = tagBiz.getTagsForFile(fileId);
        return ResponseEntity.ok(ResponseDto.success(tags));
    }

    /**
     * 批量操作文件标签。
     */
    @PostMapping("/tag/batch/operate")
    public ResponseEntity<ResponseDto> batchOperateTags(@RequestBody BatchOperateRequest request) {
        log.info("批量操作标签请求 [fileId={}, tagIds={}, action={}]",
                request.getFileId(), request.getTagIds(), request.getAction());
        BatchOperationResultVo result = tagBiz.batchOperateTags(
                request.getFileId(), request.getTagIds(), request.getAction());
        return ResponseEntity.ok(ResponseDto.success(result));
    }

    /**
     * 标签分配请求。
     */
    @lombok.Data
    public static class TagAssignRequest {
        private Long fileId;
        private List<Long> tagIds;
    }

    /**
     * 批量标签请求。
     */
    @lombok.Data
    public static class BatchTagRequest {
        private List<Long> fileIds;
        private List<Long> tagIds;
    }

    /**
     * 批量操作请求。
     */
    @lombok.Data
    public static class BatchOperateRequest {
        private Long fileId;
        private List<Long> tagIds;
        private String action;
    }
}