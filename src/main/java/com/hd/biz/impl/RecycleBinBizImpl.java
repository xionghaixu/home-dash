package com.hd.biz.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hd.biz.FileBiz;
import com.hd.biz.RecycleBinBiz;
import com.hd.dao.entity.File;
import com.hd.dao.entity.RecycleBin;
import com.hd.dao.service.FileDataService;
import com.hd.dao.service.RecycleBinDataService;
import com.hd.model.dto.ResponseDTO;
import com.hd.model.vo.RecycleBinVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.scheduling.annotation.Scheduled;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecycleBinBizImpl implements RecycleBinBiz {

    private final RecycleBinDataService recycleBinDataService;
    private final FileDataService fileDataService;

    @Autowired
    @Lazy
    private FileBiz fileBiz;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO softDelete(List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return ResponseDTO.success();
        }
        for (Long fileId : fileIds) {
            File file = fileDataService.getById(fileId);
            if (file != null) {
                // If it is a folder, recursively soft-delete all active descendants
                if ("FOLDER".equals(file.getType())) {
                    softDeleteFolderDescendants(fileId);
                }
                
                // Delete logically via MyBatis-Plus TableLogic
                fileDataService.removeById(fileId);
                
                RecycleBin recycleBin = RecycleBin.builder()
                        .fileId(fileId)
                        .originalParentId(file.getParentId())
                        .originalPath(file.getFileName()) // store name or path
                        .deleteTime(new Date())
                        // set expire time to 30 days from now
                        .expireTime(new Date(System.currentTimeMillis() + 30L * 24 * 3600 * 1000))
                        .build();
                recycleBinDataService.save(recycleBin);
            }
        }
        return ResponseDTO.success();
    }

    private void softDeleteFolderDescendants(Long folderId) {
        // Find all active children (since TableLogic automatically filters out is_deleted = 1)
        List<File> children = fileDataService.list(
                new LambdaQueryWrapper<File>().eq(File::getParentId, folderId)
        );
        for (File child : children) {
            if ("FOLDER".equals(child.getType())) {
                softDeleteFolderDescendants(child.getId());
            }
            fileDataService.removeById(child.getId());
        }
    }

    @Override
    public ResponseDTO list() {
        List<RecycleBin> list = recycleBinDataService.list(
                new LambdaQueryWrapper<RecycleBin>().orderByDesc(RecycleBin::getDeleteTime)
        );
        List<RecycleBinVO> voList = new ArrayList<>();
        for (RecycleBin rb : list) {
            File file = fileDataService.selectByIdWithDeleted(rb.getFileId());
            if (file != null) {
                RecycleBinVO vo = RecycleBinVO.builder()
                        .id(rb.getFileId()) // map id to fileId to match frontend row.id
                        .fileId(rb.getFileId())
                        .name(file.getFileName())
                        .type(file.getType())
                        .size(file.getSize())
                        .originalPath(rb.getOriginalPath())
                        .deleteTime(rb.getDeleteTime())
                        .deletedAt(rb.getDeleteTime())
                        .expireTime(rb.getExpireTime())
                        .build();
                voList.add(vo);
            }
        }
        return ResponseDTO.success(voList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO restore(List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return ResponseDTO.success();
        }
        for (Long fileId : fileIds) {
            RecycleBin rb = recycleBinDataService.getOne(
                    new LambdaQueryWrapper<RecycleBin>().eq(RecycleBin::getFileId, fileId)
            );
            if (rb != null) {
                Long parentId = rb.getOriginalParentId();
                if (parentId != null && parentId != 0) {
                    File parent = fileDataService.selectByIdWithDeleted(parentId);
                    if (parent == null || parent.getIsDeleted() == 1) {
                        parentId = 0L; // Restore to root if parent is gone
                    }
                }

                // Check for name collision before restoring (selectCount respects @TableLogic,
                // so the soft-deleted file being restored is excluded from the count)
                File deletedFile = fileDataService.selectByIdWithDeleted(fileId);
                String renameTo = null;
                if (deletedFile != null) {
                    String fileName = deletedFile.getFileName();
                    Long count = fileDataService.count(
                            new LambdaQueryWrapper<File>()
                                    .eq(File::getParentId, parentId)
                                    .eq(File::getFileName, fileName)
                    );
                    if (count > 0) {
                        renameTo = generateUniqueName(parentId, fileName);
                    }
                }

                // Restore the file
                fileDataService.restoreFile(fileId, parentId);

                // If a collision was detected, rename the restored file
                if (renameTo != null) {
                    File update = File.builder().id(fileId).fileName(renameTo).build();
                    fileDataService.updateById(update);
                }

                File file = fileDataService.getById(fileId);
                if (file != null && "FOLDER".equals(file.getType())) {
                    restoreFolderDescendants(fileId);
                }
                recycleBinDataService.removeById(rb.getId());
            }
        }
        return ResponseDTO.success();
    }

    private String generateUniqueName(Long parentId, String fileName) {
        String baseName;
        String extension;
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = fileName.substring(0, dotIndex);
            extension = fileName.substring(dotIndex);
        } else {
            baseName = fileName;
            extension = "";
        }

        for (int i = 1; i <= 1000; i++) {
            String newName = baseName + " (" + i + ")" + extension;
            Long count = fileDataService.count(
                    new LambdaQueryWrapper<File>()
                            .eq(File::getParentId, parentId)
                            .eq(File::getFileName, newName)
            );
            if (count == 0) {
                return newName;
            }
        }

        return baseName + "_" + java.util.UUID.randomUUID().toString().substring(0, 8) + extension;
    }

    private void restoreFolderDescendants(Long folderId) {
        List<File> deletedChildren = fileDataService.selectDeletedFilesByParentId(folderId);
        for (File child : deletedChildren) {
            // Check if this child has an entry in recycle_bin
            Long count = recycleBinDataService.count(
                    new LambdaQueryWrapper<RecycleBin>().eq(RecycleBin::getFileId, child.getId())
            );
            if (count == 0) {
                // Not in recycle bin, so it was deleted because of the parent. Restore it!
                fileDataService.restoreFile(child.getId(), child.getParentId());
                if ("FOLDER".equals(child.getType())) {
                    restoreFolderDescendants(child.getId());
                }
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO empty() {
        List<RecycleBin> list = recycleBinDataService.list();
        List<Long> fileIds = new ArrayList<>();
        for (RecycleBin rb : list) {
            if (rb.getFileId() != null) {
                fileIds.add(rb.getFileId());
            }
        }
        // Permanently delete files first so that if it fails, recycle bin entries are preserved
        if (!fileIds.isEmpty()) {
            fileBiz.permanentlyDelete(fileIds);
        }
        // Then remove recycle bin entries
        for (RecycleBin rb : list) {
            recycleBinDataService.removeById(rb.getId());
        }
        return ResponseDTO.success();
    }

    @Scheduled(fixedRate = 86400000) // Run daily
    @Transactional(rollbackFor = Exception.class)
    public void cleanupExpiredEntries() {
        List<RecycleBin> expired = recycleBinDataService.lambdaQuery()
                .le(RecycleBin::getExpireTime, new Date())
                .list();
        if (expired.isEmpty()) return;

        List<Long> fileIds = expired.stream()
                .map(RecycleBin::getFileId)
                .collect(Collectors.toList());

        // Permanently delete files first so that if it fails, recycle bin entries are preserved
        fileBiz.permanentlyDelete(fileIds);

        // Then remove recycle bin entries
        recycleBinDataService.lambdaUpdate()
                .in(RecycleBin::getFileId, fileIds)
                .remove();
    }
}
