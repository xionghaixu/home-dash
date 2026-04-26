package com.hd.model.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 媒体任务查询DTO
 *
 * @author system
 * @since 2026-04-26
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MediaTaskQueryDto extends PageRequestDto {

    private String status;

    private String mediaType;

    private String taskType;
}
