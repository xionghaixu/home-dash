package com.hd.model.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import lombok.Data;

import java.util.List;

/**
 * 手动扫描DTO
 *
 * @author xhx
 * @since 2026-04-26
 */
@Data
public class ManualScanDTO {

    @JsonSerialize(contentUsing = ToStringSerializer.class)
    private List<Long> fileIds;

    private String mediaType;
}
