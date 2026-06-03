package com.hd.model.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 视频详情DTO
 *
 * @author xhx
 * @since 2026-04-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoDetailDTO {

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

    private Integer episodeNumber;

    private Boolean hasSubtitle;

    private Boolean hasAudio;

    private List<SubtitleDTO> subtitleList;

    private WatchProgressDTO watchProgress;

    private List<MediaItemDTO> relatedVideos;

    private LocalDateTime lastWatchedAt;

    private Boolean favorite;
}
