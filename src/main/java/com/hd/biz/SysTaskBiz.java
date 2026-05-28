package com.hd.biz;

import com.hd.model.dto.PageResponseDto;
import com.hd.model.dto.ResponseDto;
import com.hd.model.dto.TaskQueryDto;
import com.hd.model.dto.UnifiedTaskDto;

import java.util.List;

public interface SysTaskBiz {

    ResponseDto queryTasks();

    /**
     * 统一任务查询（合并 SysTask + MediaScanTask）
     */
    PageResponseDto<UnifiedTaskDto> queryUnifiedTasks(TaskQueryDto queryDto);

    /**
     * 获取任务详情
     */
    UnifiedTaskDto getTaskDetail(Long taskId, String source);

    /**
     * 重试任务
     */
    void retryTask(Long taskId, String source);

    /**
     * 批量重试任务
     */
    void batchRetryTasks(List<Long> taskIds, String source);
}
