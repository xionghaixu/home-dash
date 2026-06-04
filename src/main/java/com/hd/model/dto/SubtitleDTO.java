package com.hd.model.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 字幕信息DTO
 *
 * @author xhx
 * @since 2026-04-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubtitleDTO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long subtitleId;

    private String language;

    private String subtitleType;

    private String format;

    private Boolean isDefault;

    private String url;
}
