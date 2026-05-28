package com.hd.model.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

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

    @JsonSerialize(contentUsing = ToStringSerializer.class)
    private List<Long> taskIds;
}
