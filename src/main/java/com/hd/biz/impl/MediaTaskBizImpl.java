package com.hd.biz.impl;

import com.hd.biz.MediaTaskBiz;
import com.hd.common.enums.ErrorCode;
import com.hd.common.exception.BusinessException;
import com.hd.dao.entity.MediaScanTask;
import com.hd.dao.service.MediaScanTaskDataService;
import com.hd.model.dto.MediaTaskDto;
import com.hd.model.dto.MediaTaskQueryDto;
import com.hd.model.dto.PageResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 媒体任务业务实现类
 *
 * @author system
 * @since 2026-04-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MediaTaskBizImpl implements MediaTaskBiz {

    private final MediaScanTaskDataService scanTaskDataService;

    @Override
    public PageResponseDto<MediaTaskDto> getTaskList(MediaTaskQueryDto queryDto) {
        int current = queryDto.getPage() != null && queryDto.getPage() > 0 ? queryDto.getPage() : 1;
        int size = queryDto.getPageSize() != null && queryDto.getPageSize() > 0 ? queryDto.getPageSize() : 20;

        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MediaScanTask>();
        if (queryDto.getStatus() != null && !queryDto.getStatus().isEmpty()) {
            wrapper.eq(MediaScanTask::getStatus, queryDto.getStatus());
        }
        if (queryDto.getMediaType() != null && !queryDto.getMediaType().isEmpty()) {
            wrapper.eq(MediaScanTask::getMediaType, queryDto.getMediaType());
        }
        if (queryDto.getTaskType() != null && !queryDto.getTaskType().isEmpty()) {
            wrapper.eq(MediaScanTask::getTaskType, queryDto.getTaskType());
        }

        long total = scanTaskDataService.count(wrapper);
        List<MediaScanTask> tasks = scanTaskDataService.lambdaQuery()
                .eq(queryDto.getStatus() != null && !queryDto.getStatus().isEmpty(), MediaScanTask::getStatus, queryDto.getStatus())
                .eq(queryDto.getMediaType() != null && !queryDto.getMediaType().isEmpty(), MediaScanTask::getMediaType, queryDto.getMediaType())
                .eq(queryDto.getTaskType() != null && !queryDto.getTaskType().isEmpty(), MediaScanTask::getTaskType, queryDto.getTaskType())
                .orderByDesc(MediaScanTask::getCreateTime)
                .last("LIMIT " + size + " OFFSET " + (current - 1) * size)
                .list();

        List<MediaTaskDto> list = tasks.stream().map(this::convertToDto).collect(Collectors.toList());
        return PageResponseDto.of(list, total, current, size);
    }

    @Override
    public MediaTaskDto getTaskDetail(Long taskId) {
        MediaScanTask task = scanTaskDataService.getById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.MEDIA_SCAN_TASK_NOT_FOUND);
        }
        return convertToDto(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void retryTask(Long taskId) {
        MediaScanTask task = scanTaskDataService.getById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.MEDIA_SCAN_TASK_NOT_FOUND);
        }
        if (task.getRetryCount() >= task.getMaxRetries()) {
            throw new BusinessException(ErrorCode.TASK_MAX_RETRIES_REACHED);
        }
        task.setStatus("PENDING");
        task.setRetryCount(task.getRetryCount() + 1);
        task.setErrorMessage(null);
        task.setFinishTime(null);
        scanTaskDataService.updateById(task);
        log.info("[MediaTask] 任务重试 - taskId: {}, retryCount: {}", taskId, task.getRetryCount());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchRetryTasks(List<Long> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return;
        }
        for (Long taskId : taskIds) {
            try {
                retryTask(taskId);
            } catch (Exception e) {
                log.warn("[MediaTask] 批量重试任务失败 - taskId: {}, error: {}", taskId, e.getMessage());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void manualScan(List<Long> fileIds, String mediaType) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }
        for (Long fileId : fileIds) {
            MediaScanTask task = new MediaScanTask();
            task.setFileId(fileId);
            task.setMediaType(mediaType != null ? mediaType : "UNKNOWN");
            task.setTaskType("METADATA_EXTRACT");
            task.setStatus("PENDING");
            task.setRetryCount(0);
            task.setMaxRetries(3);
            task.setPriority(0);
            task.setCreateTime(LocalDateTime.now());
            scanTaskDataService.save(task);
        }
        log.info("[MediaTask] 手动扫描任务创建完成 - fileCount: {}, mediaType: {}", fileIds.size(), mediaType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchScan(String mediaType) {
        log.info("[MediaTask] 批量扫描触发 - mediaType: {}", mediaType);
    }

    private MediaTaskDto convertToDto(MediaScanTask task) {
        return MediaTaskDto.builder()
                .taskId(task.getId())
                .fileId(task.getFileId())
                .mediaType(task.getMediaType())
                .taskType(task.getTaskType())
                .status(task.getStatus())
                .retryCount(task.getRetryCount())
                .maxRetries(task.getMaxRetries())
                .errorMessage(task.getErrorMessage())
                .priority(task.getPriority())
                .createTime(task.getCreateTime())
                .startTime(task.getStartTime())
                .finishTime(task.getFinishTime())
                .build();
    }
}
