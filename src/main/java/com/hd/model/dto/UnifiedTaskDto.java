package com.hd.model.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 统一任务响应DTO
 * 合并 SysTask 和 MediaScanTask 的展示模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnifiedTaskDto {

    /** 任务ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 来源: sys 或 media */
    private String source;

    /** 任务显示名称 */
    private String taskName;

    /** 任务类型原始值 */
    private String taskType;

    /** 状态: PENDING, RUNNING, SUCCESS, FAILED */
    private String status;

    /** 进度百分比 0-100 */
    private Integer progressPercent;

    /** 错误信息 */
    private String errorMsg;

    /** 已重试次数 */
    private Integer retryCount;

    /** 最大重试次数 */
    private Integer maxRetries;

    /** 关联文件ID（媒体任务） */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long fileId;

    /** 媒体类型（媒体任务） */
    private String mediaType;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime finishTime;
}
