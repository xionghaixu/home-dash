package com.hd.biz.impl;

import com.hd.biz.RemarkBiz;
import com.hd.dao.entity.FileRemark;
import com.hd.dao.service.FileRemarkDataService;
import com.hd.model.vo.FileRemarkVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文件备注业务实现类。
 *
 * @author team-lead
 * @version 1.0
 * @createTime 2026/04/25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RemarkBizImpl implements RemarkBiz {

    private final FileRemarkDataService fileRemarkDataService;

    @Override
    public FileRemarkVo getFileRemark(Long resourceId) {
        log.info("获取文件备注 [resourceId={}]", resourceId);

        FileRemark remark = fileRemarkDataService.lambdaQuery()
                .eq(FileRemark::getResourceId, resourceId)
                .one();

        return remark != null ? convertToFileRemarkVo(remark) : null;
    }

    @Override
    @Transactional
    public FileRemarkVo saveFileRemark(Long resourceId, String remarkContent) {
        log.info("保存文件备注 [resourceId={}, content={}]", resourceId, remarkContent);

        FileRemark remark = fileRemarkDataService.lambdaQuery()
                .eq(FileRemark::getResourceId, resourceId)
                .one();

        if (remark == null) {
            remark = FileRemark.builder()
                    .resourceId(resourceId)
                    .remarkContent(remarkContent)
                    .createdAt(new Date())
                    .updatedAt(new Date())
                    .build();
            fileRemarkDataService.save(remark);
        } else {
            remark.setRemarkContent(remarkContent);
            remark.setUpdatedAt(new Date());
            fileRemarkDataService.updateById(remark);
        }

        log.info("文件备注已保存 [resourceId={}]", resourceId);
        return convertToFileRemarkVo(remark);
    }

    @Override
    @Transactional
    public boolean deleteFileRemark(Long resourceId) {
        log.info("删除文件备注 [resourceId={}]", resourceId);

        boolean result = fileRemarkDataService.lambdaUpdate()
                .eq(FileRemark::getResourceId, resourceId)
                .remove();

        log.info("删除文件备注完成 [resourceId={}, result={}]", resourceId, result);
        return result;
    }

    @Override
    @Transactional
    public int batchDeleteFileRemarks(List<Long> resourceIds) {
        log.info("批量删除文件备注 [count={}]", resourceIds != null ? resourceIds.size() : 0);

        if (resourceIds == null || resourceIds.isEmpty()) {
            return 0;
        }

        int count = fileRemarkDataService.lambdaUpdate()
                .in(FileRemark::getResourceId, resourceIds)
                .remove() ? resourceIds.size() : 0;

        log.info("批量删除文件备注完成 [count={}]", count);
        return count;
    }

    @Override
    public List<FileRemarkVo> getFileRemarks(List<Long> resourceIds) {
        log.info("批量获取文件备注 [resourceIds={}]", resourceIds);

        if (resourceIds == null || resourceIds.isEmpty()) {
            return List.of();
        }

        List<FileRemark> remarks = fileRemarkDataService.lambdaQuery()
                .in(FileRemark::getResourceId, resourceIds)
                .list();

        return remarks.stream()
                .map(this::convertToFileRemarkVo)
                .collect(Collectors.toList());
    }

    private FileRemarkVo convertToFileRemarkVo(FileRemark remark) {
        return FileRemarkVo.builder()
                .id(remark.getId())
                .resourceId(remark.getResourceId())
                .remarkContent(remark.getRemarkContent())
                .createdAt(remark.getCreatedAt() != null ? remark.getCreatedAt().toString() : null)
                .updatedAt(remark.getUpdatedAt() != null ? remark.getUpdatedAt().toString() : null)
                .build();
    }
}
