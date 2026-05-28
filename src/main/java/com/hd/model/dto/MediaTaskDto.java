package com.hd.model.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 媒体任务DTO
 *
 * @author system
 * @since 2026-04-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaTaskDto {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long taskId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long fileId;

    private String mediaType;

    private String taskType;

    private String status;

    private Integer retryCount;

    private Integer maxRetries;

    private String errorMessage;

    private Integer priority;

    private LocalDateTime createTime;

    private LocalDateTime startTime;

    private LocalDateTime finishTime;
}
