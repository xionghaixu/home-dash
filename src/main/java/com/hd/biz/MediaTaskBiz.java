package com.hd.biz;

import com.hd.model.dto.MediaTaskDTO;
import com.hd.model.dto.MediaTaskQueryDTO;
import com.hd.model.dto.PageResponseDTO;

import java.util.List;

/**
 * 媒体任务业务接口
 *
 * @author xhx
 * @since 2026-04-26
 */
public interface MediaTaskBiz {

    PageResponseDTO<MediaTaskDTO> getTaskList(MediaTaskQueryDTO queryDto);

    MediaTaskDTO getTaskDetail(Long taskId);

    void retryTask(Long taskId);

    void batchRetryTasks(List<Long> taskIds);

    void manualScan(List<Long> fileIds, String mediaType);

    void batchScan(String mediaType);
}
