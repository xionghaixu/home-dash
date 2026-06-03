package com.hd.model.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 媒体任务查询DTO
 *
 * @author xhx
 * @since 2026-04-26
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MediaTaskQueryDTO extends PageRequestDTO {

    private String status;

    private String mediaType;

    private String taskType;
}
