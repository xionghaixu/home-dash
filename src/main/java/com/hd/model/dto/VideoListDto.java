package com.hd.model.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 视频列表项DTO
 *
 * @author system
 * @since 2026-04-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoListDto {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long fileId;

    private String fileName;

    private Long size;

    private Long duration;

    private String resolution;

    private Integer width;

    private Integer height;

    private Long bitrate;

    private String coverUrl;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long seriesId;

    private String seriesName;

    private Boolean hasSubtitle;

    private Boolean hasAudio;

    private WatchProgressDto watchProgress;

    private LocalDateTime lastWatchedAt;
}
