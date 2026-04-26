package com.hd.model.dto;

import lombok.Data;

import java.util.List;

/**
 * 批量重试DTO
 *
 * @author system
 * @since 2026-04-26
 */
@Data
public class BatchRetryDto {

    private List<Long> taskIds;
}
