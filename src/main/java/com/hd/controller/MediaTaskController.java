package com.hd.controller;

import com.hd.biz.MediaTaskBiz;
import com.hd.model.dto.BatchRetryDTO;
import com.hd.model.dto.ManualScanDTO;
import com.hd.model.dto.MediaTaskDTO;
import com.hd.model.dto.MediaTaskQueryDTO;
import com.hd.model.dto.PageResponseDTO;
import com.hd.model.dto.ResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 媒体任务控制器
 *
 * @author xhx
 * @since 2026-04-26
 */
@RestController
@RequestMapping("/v1/media/tasks")
@RequiredArgsConstructor
public class MediaTaskController {

    private final MediaTaskBiz mediaTaskBiz;

    @GetMapping
    public ResponseEntity<ResponseDTO> getTaskList(MediaTaskQueryDTO queryDto) {
        PageResponseDTO<MediaTaskDTO> result = mediaTaskBiz.getTaskList(queryDto);
        return ResponseEntity.ok(ResponseDTO.success(result));
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<ResponseDTO> getTaskDetail(@PathVariable Long taskId) {
        MediaTaskDTO task = mediaTaskBiz.getTaskDetail(taskId);
        return ResponseEntity.ok(ResponseDTO.success(task));
    }

    @PostMapping("/{taskId}/retry")
    public ResponseEntity<ResponseDTO> retryTask(@PathVariable Long taskId) {
        mediaTaskBiz.retryTask(taskId);
        return ResponseEntity.ok(ResponseDTO.success());
    }

    @PostMapping("/batch-retry")
    public ResponseEntity<ResponseDTO> batchRetryTasks(@RequestBody BatchRetryDTO batchRetryDto) {
        mediaTaskBiz.batchRetryTasks(batchRetryDto.getTaskIds());
        return ResponseEntity.ok(ResponseDTO.success());
    }

    @PostMapping("/scan")
    public ResponseEntity<ResponseDTO> manualScan(@RequestBody ManualScanDTO manualScanDto) {
        mediaTaskBiz.manualScan(manualScanDto.getFileIds(), manualScanDto.getMediaType());
        return ResponseEntity.ok(ResponseDTO.success());
    }

    @PostMapping("/batch-scan")
    public ResponseEntity<ResponseDTO> batchScan(@RequestParam(required = false) String mediaType) {
        mediaTaskBiz.batchScan(mediaType);
        return ResponseEntity.ok(ResponseDTO.success());
    }
}
