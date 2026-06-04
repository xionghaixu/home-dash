package com.hd.biz.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hd.biz.MediaTaskBiz;
import com.hd.biz.SysTaskBiz;
import com.hd.common.enums.ErrorCodeEnum;
import com.hd.common.enums.TaskStatusEnum;
import com.hd.common.enums.TaskTypeEnum;
import com.hd.common.exception.BusinessException;
import com.hd.dao.entity.MediaScanTask;
import com.hd.dao.entity.SysTask;
import com.hd.dao.service.MediaScanTaskDataService;
import com.hd.dao.service.SysTaskDataService;
import com.hd.model.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysTaskBizImpl implements SysTaskBiz {

    private final SysTaskDataService sysTaskDataService;
    private final MediaScanTaskDataService scanTaskDataService;
    private final MediaTaskBiz mediaTaskBiz;

    @Override
    public ResponseDTO queryTasks() {
        List<SysTask> tasks = sysTaskDataService.list(
                new LambdaQueryWrapper<SysTask>().orderByDesc(SysTask::getCreateTime));
        return ResponseDTO.success(tasks);
    }

    @Override
    public PageResponseDTO<UnifiedTaskDTO> queryUnifiedTasks(TaskQueryDTO queryDto) {
        int page = queryDto.getPage() != null && queryDto.getPage() > 0 ? queryDto.getPage() : 1;
        int pageSize = queryDto.getPageSize() != null && queryDto.getPageSize() > 0 ? queryDto.getPageSize() : 20;
        String status = queryDto.getStatus();
        String taskType = queryDto.getTaskType();
        String source = queryDto.getSource();

        long sysCount = 0;
        long mediaCount = 0;

        // Build count wrappers (separate from list wrappers to avoid "last()" leakage)
        LambdaQueryWrapper<SysTask> sysCountWrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            sysCountWrapper.eq(SysTask::getStatus, status);
        }
        if (taskType != null && !taskType.isEmpty()) {
            sysCountWrapper.eq(SysTask::getTaskType, taskType);
        }

        LambdaQueryWrapper<MediaScanTask> mediaCountWrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            mediaCountWrapper.eq(MediaScanTask::getStatus, status);
        }
        if (taskType != null && !taskType.isEmpty()) {
            mediaCountWrapper.eq(MediaScanTask::getTaskType, taskType);
        }

        // Build list wrappers (do not reuse wrappers - create fresh ones for list
        // queries)
        LambdaQueryWrapper<SysTask> sysListWrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            sysListWrapper.eq(SysTask::getStatus, status);
        }
        if (taskType != null && !taskType.isEmpty()) {
            sysListWrapper.eq(SysTask::getTaskType, taskType);
        }
        sysListWrapper.orderByDesc(SysTask::getCreateTime);
        sysListWrapper.last("LIMIT " + (page * pageSize));

        LambdaQueryWrapper<MediaScanTask> mediaListWrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            mediaListWrapper.eq(MediaScanTask::getStatus, status);
        }
        if (taskType != null && !taskType.isEmpty()) {
            mediaListWrapper.eq(MediaScanTask::getTaskType, taskType);
        }
        mediaListWrapper.orderByDesc(MediaScanTask::getCreateTime);
        mediaListWrapper.last("LIMIT " + (page * pageSize));

        List<UnifiedTaskDTO> allTasks = new ArrayList<>();

        // Query sys tasks with database-level limit
        if (source == null || source.isEmpty() || "sys".equals(source)) {
            sysCount = sysTaskDataService.count(sysCountWrapper);
            List<SysTask> sysTasks = sysTaskDataService.list(sysListWrapper);
            allTasks.addAll(sysTasks.stream().map(this::convertSysTask).toList());
        }

        // Query media tasks with database-level limit
        if (source == null || source.isEmpty() || "media".equals(source)) {
            mediaCount = scanTaskDataService.count(mediaCountWrapper);
            List<MediaScanTask> mediaTasks = scanTaskDataService.list(mediaListWrapper);
            allTasks.addAll(mediaTasks.stream().map(this::convertMediaTask).toList());
        }

        // Sort combined list in memory
        allTasks.sort((a, b) -> {
            LocalDateTime timeA = a.getCreateTime();
            LocalDateTime timeB = b.getCreateTime();
            if (timeA == null && timeB == null)
                return 0;
            if (timeA == null)
                return 1;
            if (timeB == null)
                return -1;
            return timeB.compareTo(timeA);
        });

        // Determine total count based on source filters
        long total = 0;
        if (source == null || source.isEmpty()) {
            total = sysCount + mediaCount;
        } else if ("sys".equals(source)) {
            total = sysCount;
        } else if ("media".equals(source)) {
            total = mediaCount;
        }

        int offset = (page - 1) * pageSize;
        List<UnifiedTaskDTO> pageList;
        if (offset >= allTasks.size()) {
            pageList = Collections.emptyList();
        } else {
            pageList = allTasks.stream()
                    .skip(offset)
                    .limit(pageSize)
                    .collect(Collectors.toList());
        }

        return PageResponseDTO.of(pageList, total, page, pageSize);
    }

    @Override
    public UnifiedTaskDTO getTaskDetail(Long taskId, String source) {
        if ("media".equals(source)) {
            MediaScanTask task = scanTaskDataService.getById(taskId);
            if (task == null) {
                throw new BusinessException(ErrorCodeEnum.MEDIA_SCAN_TASK_NOT_FOUND);
            }
            return convertMediaTask(task);
        } else {
            SysTask task = sysTaskDataService.getById(taskId);
            if (task == null) {
                throw new BusinessException(ErrorCodeEnum.TASK_NOT_FOUND);
            }
            return convertSysTask(task);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void retryTask(Long taskId, String source) {
        if ("media".equals(source)) {
            mediaTaskBiz.retryTask(taskId);
        } else {
            SysTask task = sysTaskDataService.getById(taskId);
            if (task == null) {
                throw new BusinessException(ErrorCodeEnum.TASK_NOT_FOUND);
            }
            task.setStatus(TaskStatusEnum.PENDING.getCode());
            task.setErrorMsg(null);
            task.setEndTime(null);
            task.setProgressPercent(0);
            sysTaskDataService.updateById(task);
            log.info("[SysTask] 任务重试 - taskId: {}", taskId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchRetryTasks(List<Long> taskIds, String source) {
        if (taskIds == null || taskIds.isEmpty()) {
            return;
        }
        for (Long taskId : taskIds) {
            try {
                retryTask(taskId, source);
            } catch (Exception e) {
                log.warn("[SysTask] 批量重试任务失败 - taskId: {}, source: {}, error: {}", taskId, source, e.getMessage());
            }
        }
    }

    private UnifiedTaskDTO convertSysTask(SysTask task) {
        LocalDateTime createTime = task.getCreateTime();
        LocalDateTime endTime = task.getEndTime();

        return UnifiedTaskDTO.builder()
                .id(task.getId())
                .source("sys")
                .taskName(getTaskTypeName(task.getTaskType()))
                .taskType(task.getTaskType())
                .status(task.getStatus())
                .progressPercent(task.getProgressPercent())
                .errorMsg(task.getErrorMsg())
                .retryCount(0)
                .maxRetries(0)
                .createTime(createTime)
                .finishTime(endTime)
                .build();
    }

    private UnifiedTaskDTO convertMediaTask(MediaScanTask task) {
        return UnifiedTaskDTO.builder()
                .id(task.getId())
                .source("media")
                .taskName(getTaskTypeName(task.getTaskType()))
                .taskType(task.getTaskType())
                .status(task.getStatus())
                .progressPercent(TaskStatusEnum.SUCCESS.getCode().equals(task.getStatus()) ? 100
                        : (TaskStatusEnum.RUNNING.getCode().equals(task.getStatus()) ? 50 : 0))
                .errorMsg(task.getErrorMessage())
                .retryCount(task.getRetryCount())
                .maxRetries(task.getMaxRetries())
                .fileId(task.getFileId())
                .mediaType(task.getMediaType())
                .createTime(task.getCreateTime())
                .startTime(task.getStartTime())
                .finishTime(task.getFinishTime())
                .build();
    }

    private String getTaskTypeName(String taskType) {
        return TaskTypeEnum.getNameByCode(taskType);
    }
}


