package com.hd.biz;

import com.hd.model.dto.PageResponseDTO;
import com.hd.model.dto.ResponseDTO;
import com.hd.model.dto.TaskQueryDTO;
import com.hd.model.dto.UnifiedTaskDTO;

import java.util.List;

public interface SysTaskBiz {

    ResponseDTO queryTasks();

    /**
     * 统一任务查询（合并 SysTask + MediaScanTask）
     */
    PageResponseDTO<UnifiedTaskDTO> queryUnifiedTasks(TaskQueryDTO queryDto);

    /**
     * 获取任务详情
     */
    UnifiedTaskDTO getTaskDetail(Long taskId, String source);

    /**
     * 重试任务
     */
    void retryTask(Long taskId, String source);

    /**
     * 批量重试任务
     */
    void batchRetryTasks(List<Long> taskIds, String source);
}
