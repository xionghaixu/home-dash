package com.hd.biz.impl;

import com.hd.biz.TagBiz;
import com.hd.common.enums.ErrorCodeEnum;
import com.hd.common.exception.BusinessException;
import com.hd.dao.entity.*;
import com.hd.dao.service.FileTagDataService;
import com.hd.dao.service.FileTagRelationDataService;
import com.hd.model.dto.TagRequestDTO;
import com.hd.model.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 标签业务实现类。
 *
 * @author xhx
 * @version 1.0
 * @createTime 2026/04/25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TagBizImpl implements TagBiz {

    private final FileTagDataService fileTagDataService;
    private final FileTagRelationDataService fileTagRelationDataService;

    @Override
    @Transactional
    public TagVO createTag(TagRequestDTO dto) {
        log.info("创建标签 [tagName={}]", dto.getTagName());

        // 检查名称唯一性
        long count = fileTagDataService.lambdaQuery()
                .eq(FileTag::getTagName, dto.getTagName())
                .count();
        if (count > 0) {
            throw new BusinessException(ErrorCodeEnum.DATA_DUPLICATE, "标签名称已存在: " + dto.getTagName());
        }

        FileTag tag = FileTag.builder()
                .tagName(dto.getTagName())
                .tagColor(dto.getTagColor() != null ? dto.getTagColor() : "#409EFF")
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();

        fileTagDataService.save(tag);
        log.info("标签创建成功 [id={}, tagName={}]", tag.getId(), tag.getTagName());

        return convertToTagVO(tag);
    }

    @Override
    @Transactional
    public TagVO updateTag(Long id, TagRequestDTO dto) {
        log.info("更新标签 [id={}]", id);

        FileTag tag = fileTagDataService.getById(id);
        if (tag == null) {
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND, "标签不存在: " + id);
        }

        // 检查名称唯一性（排除自己）
        if (dto.getTagName() != null && !dto.getTagName().equals(tag.getTagName())) {
            long count = fileTagDataService.lambdaQuery()
                    .eq(FileTag::getTagName, dto.getTagName())
                    .count();
            if (count > 0) {
                throw new BusinessException(ErrorCodeEnum.DATA_DUPLICATE, "标签名称已存在: " + dto.getTagName());
            }
            tag.setTagName(dto.getTagName());
        }

        if (dto.getTagColor() != null) {
            tag.setTagColor(dto.getTagColor());
        }
        tag.setUpdatedAt(new Date());

        fileTagDataService.updateById(tag);
        log.info("标签更新成功 [id={}]", id);

        return convertToTagVO(tag);
    }

    @Override
    @Transactional
    public boolean deleteTag(Long id) {
        log.info("删除标签 [id={}]", id);

        // 删除标签关联
        fileTagRelationDataService.lambdaUpdate()
                .eq(FileTagRelation::getTagId, id)
                .remove();

        // 删除标签
        boolean result = fileTagDataService.removeById(id);
        log.info("删除标签完成 [id={}, result={}]", id, result);

        return result;
    }

    @Override
    public List<TagVO> getTagList() {
        log.info("获取标签列表");

        List<FileTag> tags = fileTagDataService.list();
        return tags.stream()
                .map(this::convertToTagVO)
                .collect(Collectors.toList());
    }

    @Override
    public TagVO getTagById(Long id) {
        log.info("获取标签详情 [id={}]", id);

        FileTag tag = fileTagDataService.getById(id);
        return tag != null ? convertToTagVO(tag) : null;
    }

    @Override
    @Transactional
    public BatchOperationResultVO assignTagsToFiles(List<Long> fileIds, List<Long> tagIds) {
        return batchOperateTagsInternal(fileIds, tagIds, "ADD");
    }

    @Override
    @Transactional
    public BatchOperationResultVO addTagsToFiles(List<Long> fileIds, List<Long> tagIds) {
        return batchOperateTagsInternal(fileIds, tagIds, "ADD");
    }

    @Override
    @Transactional
    public BatchOperationResultVO removeTagsFromFiles(List<Long> fileIds, List<Long> tagIds) {
        return batchOperateTagsInternal(fileIds, tagIds, "REMOVE");
    }

    @Override
    public List<TagVO> getTagsForFile(Long fileId) {
        log.info("获取文件标签 [fileId={}]", fileId);

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
                .map(this::convertToTagVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BatchOperationResultVO batchOperateTags(Long fileId, List<Long> tagIds, String action) {
        return batchOperateTagsInternal(Collections.singletonList(fileId), tagIds, action);
    }

    private BatchOperationResultVO batchOperateTagsInternal(List<Long> fileIds, List<Long> tagIds, String action) {
        log.info("批量操作标签 [fileIds={}, tagIds={}, action={}]", fileIds, tagIds, action);

        List<String> errors = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        for (Long fileId : fileIds) {
            try {
                if ("REPLACE".equalsIgnoreCase(action)) {
                    // 先删除所有标签，再添加新标签
                    removeAllTagRelations(fileId);
                    for (Long tagId : tagIds) {
                        addTagRelation(fileId, tagId);
                    }
                    successCount++;
                } else {
                    for (Long tagId : tagIds) {
                        try {
                            switch (action.toUpperCase()) {
                                case "ADD":
                                    successCount += addTagRelation(fileId, tagId) ? 1 : 0;
                                    break;
                                case "REMOVE":
                                    successCount += removeTagRelation(fileId, tagId) ? 1 : 0;
                                    break;
                                default:
                                    failCount++;
                                    errors.add("未知操作: " + action);
                            }
                        } catch (Exception e) {
                            failCount++;
                            errors.add("操作失败: fileId=" + fileId + ", tagId=" + tagId + ", error=" + e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                failCount++;
                errors.add("操作失败: fileId=" + fileId + ", error=" + e.getMessage());
            }
        }

        return BatchOperationResultVO.builder()
                .successCount(successCount)
                .failCount(failCount)
                .errors(errors)
                .build();
    }

    private boolean addTagRelation(Long fileId, Long tagId) {
        // 检查是否已存在
        long count = fileTagRelationDataService.lambdaQuery()
                .eq(FileTagRelation::getFileId, fileId)
                .eq(FileTagRelation::getTagId, tagId)
                .count();

        if (count > 0) {
            return true; // 已存在，认为成功
        }

        FileTagRelation relation = FileTagRelation.builder()
                .fileId(fileId)
                .tagId(tagId)
                .createdAt(new Date())
                .build();

        return fileTagRelationDataService.save(relation);
    }

    private boolean removeTagRelation(Long fileId, Long tagId) {
        return fileTagRelationDataService.lambdaUpdate()
                .eq(FileTagRelation::getFileId, fileId)
                .eq(FileTagRelation::getTagId, tagId)
                .remove();
    }

    private void removeAllTagRelations(Long fileId) {
        fileTagRelationDataService.lambdaUpdate()
                .eq(FileTagRelation::getFileId, fileId)
                .remove();
    }

    private TagVO convertToTagVO(FileTag tag) {
        return TagVO.builder()
                .id(tag.getId())
                .tagName(tag.getTagName())
                .tagColor(tag.getTagColor())
                .build();
    }
}
