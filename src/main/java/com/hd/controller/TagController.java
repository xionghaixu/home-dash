package com.hd.controller;

import com.hd.biz.TagBiz;
import com.hd.common.HomeDashConstants;
import com.hd.model.dto.ResponseDTO;
import com.hd.model.dto.TagRequestDTO;
import com.hd.model.vo.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.ArrayList;

/**
 * 标签控制器。
 * 提供标签CRUD和文件标签关联的REST API接口。
 *
 * @author xhx
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
    public ResponseEntity<ResponseDTO> createTag(@Valid @RequestBody TagRequestDTO dto) {
        log.info("创建标签请求 [tagName={}]", dto.getTagName());
        TagVO tag = tagBiz.createTag(dto);
        return ResponseEntity.ok(ResponseDTO.success(tag));
    }

    /**
     * 更新标签。
     */
    @PostMapping("/tag/{id}/update")
    public ResponseEntity<ResponseDTO> updateTag(@PathVariable Long id, @RequestBody TagRequestDTO dto) {
        log.info("更新标签请求 [id={}]", id);
        TagVO tag = tagBiz.updateTag(id, dto);
        return ResponseEntity.ok(ResponseDTO.success(tag));
    }

    /**
     * 删除标签。
     */
    @PostMapping("/tag/{id}/delete")
    public ResponseEntity<ResponseDTO> deleteTag(@PathVariable Long id) {
        log.info("删除标签请求 [id={}]", id);
        boolean result = tagBiz.deleteTag(id);
        return ResponseEntity.ok(ResponseDTO.success(result));
    }

    /**
     * 获取标签列表。
     */
    @GetMapping("/tag/list")
    public ResponseEntity<ResponseDTO> getTagList() {
        log.info("获取标签列表请求");
        List<TagVO> tags = tagBiz.getTagList();
        return ResponseEntity.ok(ResponseDTO.success(tags));
    }

    /**
     * 获取标签详情。
     */
    @GetMapping("/tag/{id}")
    public ResponseEntity<ResponseDTO> getTagById(@PathVariable Long id) {
        log.info("获取标签详情请求 [id={}]", id);
        TagVO tag = tagBiz.getTagById(id);
        return ResponseEntity.ok(ResponseDTO.success(tag));
    }

    /**
     * 为文件分配标签。
     */
    @PostMapping("/tag/assign")
    public ResponseEntity<ResponseDTO> assignTags(@RequestBody TagAssignRequest request) {
        log.info("分配标签请求 [fileId={}, tagIds={}]", request.getFileId(), request.getTagIds());
        BatchOperationResultVO result = tagBiz.assignTagsToFiles(
                List.of(request.getFileId()), request.getTagIds());
        return ResponseEntity.ok(ResponseDTO.success(result));
    }

    /**
     * 批量为文件添加标签。
     */
    @PostMapping("/tag/batch/add")
    public ResponseEntity<ResponseDTO> batchAddTags(@RequestBody BatchTagRequest request) {
        log.info("批量添加标签请求 [fileIds={}, tagIds={}, tagNames={}]", 
                request.getFileIds(), request.getTagIds(), request.getTagNames());
        
        List<Long> tagIds = request.getTagIds() != null ? new ArrayList<>(request.getTagIds()) : new ArrayList<>();
        if (request.getTagNames() != null && !request.getTagNames().isEmpty()) {
            List<TagVO> allTags = tagBiz.getTagList();
            for (String name : request.getTagNames()) {
                if (name == null || name.trim().isEmpty()) {
                    continue;
                }
                String trimmedName = name.trim();
                TagVO tag = allTags.stream()
                        .filter(t -> trimmedName.equalsIgnoreCase(t.getTagName()))
                        .findFirst()
                        .orElse(null);
                if (tag == null) {
                    tag = tagBiz.createTag(TagRequestDTO.builder().tagName(trimmedName).tagColor("#409EFF").build());
                    allTags.add(tag);
                }
                tagIds.add(tag.getId());
            }
        }
        
        BatchOperationResultVO result = tagBiz.addTagsToFiles(request.getFileIds(), tagIds);
        return ResponseEntity.ok(ResponseDTO.success(result));
    }

    /**
     * 批量移除文件标签。
     */
    @PostMapping("/tag/batch/remove")
    public ResponseEntity<ResponseDTO> batchRemoveTags(@RequestBody BatchTagRequest request) {
        log.info("批量移除标签请求 [fileIds={}, tagIds={}, tagNames={}]", 
                request.getFileIds(), request.getTagIds(), request.getTagNames());
        
        List<Long> tagIds = request.getTagIds() != null ? new ArrayList<>(request.getTagIds()) : new ArrayList<>();
        if (request.getTagNames() != null && !request.getTagNames().isEmpty()) {
            List<TagVO> allTags = tagBiz.getTagList();
            for (String name : request.getTagNames()) {
                if (name == null || name.trim().isEmpty()) {
                    continue;
                }
                String trimmedName = name.trim();
                TagVO tag = allTags.stream()
                        .filter(t -> trimmedName.equalsIgnoreCase(t.getTagName()))
                        .findFirst()
                        .orElse(null);
                if (tag != null) {
                    tagIds.add(tag.getId());
                }
            }
        }
        
        BatchOperationResultVO result = tagBiz.removeTagsFromFiles(request.getFileIds(), tagIds);
        return ResponseEntity.ok(ResponseDTO.success(result));
    }

    /**
     * 获取文件的所有标签。
     */
    @GetMapping("/tag/file/{fileId}")
    public ResponseEntity<ResponseDTO> getTagsForFile(@PathVariable Long fileId) {
        log.info("获取文件标签请求 [fileId={}]", fileId);
        List<TagVO> tags = tagBiz.getTagsForFile(fileId);
        return ResponseEntity.ok(ResponseDTO.success(tags));
    }

    /**
     * 批量操作文件标签。
     */
    @PostMapping("/tag/batch/operate")
    public ResponseEntity<ResponseDTO> batchOperateTags(@RequestBody BatchOperateRequest request) {
        log.info("批量操作标签请求 [fileId={}, tagIds={}, action={}]",
                request.getFileId(), request.getTagIds(), request.getAction());
        BatchOperationResultVO result = tagBiz.batchOperateTags(
                request.getFileId(), request.getTagIds(), request.getAction());
        return ResponseEntity.ok(ResponseDTO.success(result));
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
        private List<String> tagNames;
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