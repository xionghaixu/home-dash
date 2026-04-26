package com.hd.model.dto;

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

    private Long fileId;

    private String fileName;

    private Long size;

    private Long duration;

    private String resolution;

    private Integer width;

    private Integer height;

    private Long bitrate;

    private String coverUrl;

    private Long seriesId;

    private String seriesName;

    private Boolean hasSubtitle;

    private Boolean hasAudio;

    private WatchProgressDto watchProgress;

    private LocalDateTime lastWatchedAt;
}
