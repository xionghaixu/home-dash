package com.hd.model.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 音频列表项DTO
 *
 * @author xhx
 * @since 2026-04-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AudioListDTO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long fileId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long resourceId;

    private String fileName;

    private String title;

    private String album;

    private String artist;

    private Long duration;

    private Long bitrate;

    private Integer trackNumber;

    private Integer year;

    private String genre;

    private String coverUrl;

    private String audioUrl;
}
