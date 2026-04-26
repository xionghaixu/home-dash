package com.hd.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 视频详情DTO
 *
 * @author system
 * @since 2026-04-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoDetailDto {

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

    private Integer episodeNumber;

    private Boolean hasSubtitle;

    private Boolean hasAudio;

    private List<SubtitleDto> subtitleList;

    private WatchProgressDto watchProgress;

    private List<MediaItemDto> relatedVideos;

    private LocalDateTime lastWatchedAt;

    private Boolean favorite;
}
