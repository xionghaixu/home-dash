package com.hd.model.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 视频集数DTO
 *
 * @author xhx
 * @since 2026-04-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoEpisodeDTO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long episodeId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long fileId;

    private Integer episodeNumber;

    private Integer seasonNumber;

    private String episodeTitle;

    private String coverUrl;

    private String seriesName;

    private Long duration;

    private WatchProgressDTO watchProgress;
}
