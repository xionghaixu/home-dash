package com.hd.biz.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hd.biz.MediaTaskBiz;
import com.hd.biz.SysTaskBiz;
import com.hd.common.enums.ErrorCode;
import com.hd.common.exception.BusinessException;
import com.hd.dao.entity.MediaScanTask;
import com.hd.dao.entity.SysTask;
import com.hd.dao.mapper.SysTaskMapper;
import com.hd.dao.service.MediaScanTaskDataService;
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

    private final SysTaskMapper sysTaskMapper;
    private final MediaScanTaskDataService scanTaskDataService;
    private final MediaTaskBiz mediaTaskBiz;

    /** 任务类型显示名称映射 */
    private static final Map<String, String> TASK_TYPE_NAMES = Map.of(
            "SCAN_DUPLICATE", "重复文件扫描",
            "STORAGE_ANALYSIS", "存储分析",
            "INTEGRITY_CHECK", "完整性检查",
            "METADATA_EXTRACT", "元数据提取",
            "THUMBNAIL_GENERATE", "缩略图生成"
    );

    @Override
    public ResponseDto queryTasks() {
        List<SysTask> tasks = sysTaskMapper.selectList(
                new LambdaQueryWrapper<SysTask>().orderByDesc(SysTask::getCreateTime)
        );
        return ResponseDto.success(tasks);
    }

    @Override
    public PageResponseDto<UnifiedTaskDto> queryUnifiedTasks(TaskQueryDto queryDto) {
        int page = queryDto.getPage() != null && queryDto.getPage() > 0 ? queryDto.getPage() : 1;
        int pageSize = queryDto.getPageSize() != null && queryDto.getPageSize() > 0 ? queryDto.getPageSize() : 20;
        String status = queryDto.getStatus();
        String taskType = queryDto.getTaskType();
        String source = queryDto.getSource();

        List<UnifiedTaskDto> allTasks = new ArrayList<>();

        // 查询系统任务
        if (source == null || source.isEmpty() || "sys".equals(source)) {
            var sysWrapper = new LambdaQueryWrapper<SysTask>();
            if (status != null && !status.isEmpty()) {
                sysWrapper.eq(SysTask::getStatus, status);
            }
            if (taskType != null && !taskType.isEmpty()) {
                sysWrapper.eq(SysTask::getTaskType, taskType);
            }
            sysWrapper.orderByDesc(SysTask::getCreateTime);
            List<SysTask> sysTasks = sysTaskMapper.selectList(sysWrapper);
            allTasks.addAll(sysTasks.stream().map(this::convertSysTask).collect(Collectors.toList()));
        }

        // 查询媒体任务
        if (source == null || source.isEmpty() || "media".equals(source)) {
            var mediaWrapper = new LambdaQueryWrapper<MediaScanTask>();
            if (status != null && !status.isEmpty()) {
                mediaWrapper.eq(MediaScanTask::getStatus, status);
            }
            if (taskType != null && !taskType.isEmpty()) {
                mediaWrapper.eq(MediaScanTask::getTaskType, taskType);
            }
            mediaWrapper.orderByDesc(MediaScanTask::getCreateTime);
            List<MediaScanTask> mediaTasks = scanTaskDataService.list(mediaWrapper);
            allTasks.addAll(mediaTasks.stream().map(this::convertMediaTask).collect(Collectors.toList()));
        }

        // 按创建时间倒序排序
        allTasks.sort((a, b) -> {
            LocalDateTime timeA = a.getCreateTime();
            LocalDateTime timeB = b.getCreateTime();
            if (timeA == null && timeB == null) return 0;
            if (timeA == null) return 1;
            if (timeB == null) return -1;
            return timeB.compareTo(timeA);
        });

        // 分页
        long total = allTasks.size();
        int offset = (page - 1) * pageSize;
        List<UnifiedTaskDto> pageList = allTasks.stream()
                .skip(offset)
                .limit(pageSize)
                .collect(Collectors.toList());

        return PageResponseDto.of(pageList, total, page, pageSize);
    }

    @Override
    public UnifiedTaskDto getTaskDetail(Long taskId, String source) {
        if ("media".equals(source)) {
            MediaScanTask task = scanTaskDataService.getById(taskId);
            if (task == null) {
                throw new BusinessException(ErrorCode.MEDIA_SCAN_TASK_NOT_FOUND);
            }
            return convertMediaTask(task);
        } else {
            SysTask task = sysTaskMapper.selectById(taskId);
            if (task == null) {
                throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
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
            SysTask task = sysTaskMapper.selectById(taskId);
            if (task == null) {
                throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
            }
            task.setStatus("PENDING");
            task.setErrorMsg(null);
            task.setEndTime(null);
            task.setProgressPercent(0);
            sysTaskMapper.updateById(task);
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

    private UnifiedTaskDto convertSysTask(SysTask task) {
        LocalDateTime createTime = task.getCreateTime() != null
                ? task.getCreateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                : null;
        LocalDateTime endTime = task.getEndTime() != null
                ? task.getEndTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                : null;

        return UnifiedTaskDto.builder()
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

    private UnifiedTaskDto convertMediaTask(MediaScanTask task) {
        return UnifiedTaskDto.builder()
                .id(task.getId())
                .source("media")
                .taskName(getTaskTypeName(task.getTaskType()))
                .taskType(task.getTaskType())
                .status(task.getStatus())
                .progressPercent("SUCCESS".equals(task.getStatus()) ? 100 : ("RUNNING".equals(task.getStatus()) ? 50 : 0))
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
        return TASK_TYPE_NAMES.getOrDefault(taskType, taskType);
    }
}
