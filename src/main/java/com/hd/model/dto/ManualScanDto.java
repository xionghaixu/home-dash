package com.hd.model.dto;

import lombok.Data;

import java.util.List;

/**
 * 手动扫描DTO
 *
 * @author system
 * @since 2026-04-26
 */
@Data
public class ManualScanDto {

    private List<Long> fileIds;

    private String mediaType;
}
