package com.hd.controller;

import com.hd.biz.SysTaskBiz;
import com.hd.common.HomeDashConstants;
import com.hd.model.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(HomeDashConstants.API_VERSION + "/tasks")
@RequiredArgsConstructor
public class SysTaskController {

    private final SysTaskBiz sysTaskBiz;

    /**
     * 统一任务列表查询（合并系统任务和媒体任务）
     */
    @GetMapping
    public ResponseEntity<ResponseDTO> queryTasks(TaskQueryDTO queryDto) {
        PageResponseDTO<UnifiedTaskDTO> result = sysTaskBiz.queryUnifiedTasks(queryDto);
        return ResponseEntity.ok(ResponseDTO.success(result));
    }

    /**
     * 任务详情
     */
    @GetMapping("/{taskId}")
    public ResponseEntity<ResponseDTO> getTaskDetail(@PathVariable Long taskId,
                                                     @RequestParam(required = false, defaultValue = "sys") String source) {
        UnifiedTaskDTO task = sysTaskBiz.getTaskDetail(taskId, source);
        return ResponseEntity.ok(ResponseDTO.success(task));
    }

    /**
     * 重试任务
     */
    @PostMapping("/{taskId}/retry")
    public ResponseEntity<ResponseDTO> retryTask(@PathVariable Long taskId,
                                                 @RequestParam(required = false, defaultValue = "sys") String source) {
        sysTaskBiz.retryTask(taskId, source);
        return ResponseEntity.ok(ResponseDTO.success());
    }

    /**
     * 批量重试任务
     */
    @PostMapping("/batch-retry")
    public ResponseEntity<ResponseDTO> batchRetryTasks(@RequestBody BatchRetryRequest request) {
        sysTaskBiz.batchRetryTasks(request.getTaskIds(), request.getSource());
        return ResponseEntity.ok(ResponseDTO.success());
    }

    /**
     * 批量重试请求体
     */
    @lombok.Data
    public static class BatchRetryRequest {
        private List<Long> taskIds;
        private String source;
    }
}
