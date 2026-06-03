package com.hd.biz;

import com.hd.dao.entity.DuplicateGroup;
import com.hd.dao.entity.File;
import com.hd.model.dto.StorageAnalysisDTO;

import java.util.List;

public interface GovernanceBiz {

    /**
     * Find duplicate files (group by MD5 having count > 1).
     * This may scan and populate duplicate_group and duplicate_record, or just return them.
     */
    List<DuplicateGroup> scanAndGetDuplicates();

    /**
     * Find large files (order by size desc limit 100).
     */
    List<File> getLargeFiles();

    /**
     * Find empty directories.
     */
    List<File> getEmptyDirectories();

    /**
     * Smart cleanup duplicate files: keeps the oldest file in each group, soft-deletes the rest.
     */
    void smartCleanup();

    /**
     * Clean up a specific duplicate group: keeps the oldest file in this group, soft-deletes the rest.
     */
    void cleanupGroup(Long groupId);

    /**
     * 获取存储分析数据
     */
    StorageAnalysisDTO getStorageAnalysis();
}
