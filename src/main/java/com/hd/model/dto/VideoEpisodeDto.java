package com.hd.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 视频集数DTO
 *
 * @author system
 * @since 2026-04-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoEpisodeDto {

    private Long episodeId;

    private Long fileId;

    private Integer episodeNumber;

    private Integer seasonNumber;

    private String episodeTitle;

    private String coverUrl;

    private Long duration;

    private WatchProgressDto watchProgress;
}
