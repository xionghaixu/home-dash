package com.hd.model.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 统一任务查询DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TaskQueryDto extends PageRequestDto {

    /** 状态筛选: PENDING, RUNNING, SUCCESS, FAILED */
    private String status;

    /** 任务类型筛选 */
    private String taskType;

    /** 来源筛选: sys 或 media */
    private String source;
}
