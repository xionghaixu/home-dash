package com.hd.biz;

import com.hd.model.dto.TagRequestDTO;
import com.hd.model.vo.*;
import java.util.List;

/**
 * 标签业务接口。
 * 定义标签的CRUD和文件标签关联操作。
 *
 * @author xhx
 * @version 1.0
 * @createTime 2026/04/25
 */
public interface TagBiz {

    /**
     * 创建标签。
     */
    TagVO createTag(TagRequestDTO dto);

    /**
     * 更新标签。
     */
    TagVO updateTag(Long id, TagRequestDTO dto);

    /**
     * 删除标签。
     */
    boolean deleteTag(Long id);

    /**
     * 获取标签列表。
     */
    List<TagVO> getTagList();

    /**
     * 获取标签详情。
     */
    TagVO getTagById(Long id);

    /**
     * 为文件分配标签。
     */
    BatchOperationResultVO assignTagsToFiles(List<Long> fileIds, List<Long> tagIds);

    /**
     * 为文件添加标签。
     */
    BatchOperationResultVO addTagsToFiles(List<Long> fileIds, List<Long> tagIds);

    /**
     * 移除文件的标签。
     */
    BatchOperationResultVO removeTagsFromFiles(List<Long> fileIds, List<Long> tagIds);

    /**
     * 获取文件的所有标签。
     */
    List<TagVO> getTagsForFile(Long fileId);

    /**
     * 批量操作标签（添加、移除、替换）。
     */
    BatchOperationResultVO batchOperateTags(Long fileId, List<Long> tagIds, String action);
}