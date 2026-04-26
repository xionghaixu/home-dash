package com.hd.controller;

import com.hd.biz.MediaTaskBiz;
import com.hd.model.dto.BatchRetryDto;
import com.hd.model.dto.ManualScanDto;
import com.hd.model.dto.MediaTaskDto;
import com.hd.model.dto.MediaTaskQueryDto;
import com.hd.model.dto.PageResponseDto;
import com.hd.model.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 媒体任务控制器
 *
 * @author system
 * @since 2026-04-26
 */
@RestController
@RequestMapping("/v1/media/tasks")
@RequiredArgsConstructor
public class MediaTaskController {

    private final MediaTaskBiz mediaTaskBiz;

    @GetMapping
    public ResponseDto getTaskList(MediaTaskQueryDto queryDto) {
        PageResponseDto<MediaTaskDto> result = mediaTaskBiz.getTaskList(queryDto);
        return ResponseDto.success(result);
    }

    @GetMapping("/{taskId}")
    public ResponseDto getTaskDetail(@PathVariable Long taskId) {
        MediaTaskDto task = mediaTaskBiz.getTaskDetail(taskId);
        return ResponseDto.success(task);
    }

    @PostMapping("/{taskId}/retry")
    public ResponseDto retryTask(@PathVariable Long taskId) {
        mediaTaskBiz.retryTask(taskId);
        return ResponseDto.success();
    }

    @PostMapping("/batch-retry")
    public ResponseDto batchRetryTasks(@RequestBody BatchRetryDto batchRetryDto) {
        mediaTaskBiz.batchRetryTasks(batchRetryDto.getTaskIds());
        return ResponseDto.success();
    }

    @PostMapping("/scan")
    public ResponseDto manualScan(@RequestBody ManualScanDto manualScanDto) {
        mediaTaskBiz.manualScan(manualScanDto.getFileIds(), manualScanDto.getMediaType());
        return ResponseDto.success();
    }

    @PostMapping("/batch-scan")
    public ResponseDto batchScan(@RequestParam(required = false) String mediaType) {
        mediaTaskBiz.batchScan(mediaType);
        return ResponseDto.success();
    }
}
