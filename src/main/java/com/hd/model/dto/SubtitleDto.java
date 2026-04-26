package com.hd.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 字幕信息DTO
 *
 * @author system
 * @since 2026-04-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubtitleDto {

    private Long subtitleId;

    private String language;

    private String subtitleType;

    private String format;

    private Boolean isDefault;

    private String url;
}
