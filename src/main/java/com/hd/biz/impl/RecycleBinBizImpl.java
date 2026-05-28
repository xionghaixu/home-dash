package com.hd.biz.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hd.biz.FileBiz;
import com.hd.biz.RecycleBinBiz;
import com.hd.dao.entity.File;
import com.hd.dao.entity.RecycleBin;
import com.hd.dao.mapper.FileMapper;
import com.hd.dao.mapper.RecycleBinMapper;
import com.hd.model.dto.ResponseDto;
import com.hd.model.vo.RecycleBinVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecycleBinBizImpl implements RecycleBinBiz {

    private final RecycleBinMapper recycleBinMapper;
    private final FileMapper fileMapper;

    @Autowired
    @Lazy
    private FileBiz fileBiz;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseDto softDelete(List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return ResponseDto.success();
        }
        for (Long fileId : fileIds) {
            File file = fileMapper.selectById(fileId);
            if (file != null) {
                // If it is a folder, recursively soft-delete all active descendants
                if ("FOLDER".equals(file.getType())) {
                    softDeleteFolderDescendants(fileId);
                }
                
                // Delete logically via MyBatis-Plus TableLogic
                fileMapper.deleteById(fileId);
                
                RecycleBin recycleBin = RecycleBin.builder()
                        .fileId(fileId)
                        .originalParentId(file.getParentId())
                        .originalPath(file.getFileName()) // store name or path
                        .deleteTime(new Date())
                        // set expire time to 30 days from now
                        .expireTime(new Date(System.currentTimeMillis() + 30L * 24 * 3600 * 1000))
                        .build();
                recycleBinMapper.insert(recycleBin);
            }
        }
        return ResponseDto.success();
    }

    private void softDeleteFolderDescendants(Long folderId) {
        // Find all active children (since TableLogic automatically filters out is_deleted = 1)
        List<File> children = fileMapper.selectList(
                new LambdaQueryWrapper<File>().eq(File::getParentId, folderId)
        );
        for (File child : children) {
            if ("FOLDER".equals(child.getType())) {
                softDeleteFolderDescendants(child.getId());
            }
            fileMapper.deleteById(child.getId());
        }
    }

    @Override
    public ResponseDto list() {
        List<RecycleBin> list = recycleBinMapper.selectList(
                new LambdaQueryWrapper<RecycleBin>().orderByDesc(RecycleBin::getDeleteTime)
        );
        List<RecycleBinVo> voList = new ArrayList<>();
        for (RecycleBin rb : list) {
            File file = fileMapper.selectByIdWithDeleted(rb.getFileId());
            if (file != null) {
                RecycleBinVo vo = RecycleBinVo.builder()
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
        return ResponseDto.success(voList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseDto restore(List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return ResponseDto.success();
        }
        for (Long fileId : fileIds) {
            RecycleBin rb = recycleBinMapper.selectOne(
                    new LambdaQueryWrapper<RecycleBin>().eq(RecycleBin::getFileId, fileId)
            );
            if (rb != null) {
                fileMapper.restoreFile(fileId, rb.getOriginalParentId());
                File file = fileMapper.selectById(fileId);
                if (file != null && "FOLDER".equals(file.getType())) {
                    restoreFolderDescendants(fileId);
                }
                recycleBinMapper.deleteById(rb.getId());
            }
        }
        return ResponseDto.success();
    }

    private void restoreFolderDescendants(Long folderId) {
        List<File> deletedChildren = fileMapper.selectDeletedFilesByParentId(folderId);
        for (File child : deletedChildren) {
            // Check if this child has an entry in recycle_bin
            Long count = recycleBinMapper.selectCount(
                    new LambdaQueryWrapper<RecycleBin>().eq(RecycleBin::getFileId, child.getId())
            );
            if (count == 0) {
                // Not in recycle bin, so it was deleted because of the parent. Restore it!
                fileMapper.restoreFile(child.getId(), child.getParentId());
                if ("FOLDER".equals(child.getType())) {
                    restoreFolderDescendants(child.getId());
                }
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseDto empty() {
        List<RecycleBin> list = recycleBinMapper.selectList(null);
        List<Long> fileIds = new ArrayList<>();
        for (RecycleBin rb : list) {
            if (rb.getFileId() != null) {
                fileIds.add(rb.getFileId());
            }
            recycleBinMapper.deleteById(rb.getId());
        }
        if (!fileIds.isEmpty()) {
            fileBiz.permanentlyDelete(fileIds);
        }
        return ResponseDto.success();
    }
}
