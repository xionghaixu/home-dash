package com.hd.biz;

import com.hd.model.dto.MediaTaskDto;
import com.hd.model.dto.MediaTaskQueryDto;
import com.hd.model.dto.PageResponseDto;

import java.util.List;

/**
 * 媒体任务业务接口
 *
 * @author system
 * @since 2026-04-26
 */
public interface MediaTaskBiz {

    PageResponseDto<MediaTaskDto> getTaskList(MediaTaskQueryDto queryDto);

    MediaTaskDto getTaskDetail(Long taskId);

    void retryTask(Long taskId);

    void batchRetryTasks(List<Long> taskIds);

    void manualScan(List<Long> fileIds, String mediaType);

    void batchScan(String mediaType);
}
