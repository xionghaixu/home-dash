package com.hd.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 视频系列DTO
 *
 * @author system
 * @since 2026-04-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoSeriesDto {

    private Long seriesId;

    private String seriesName;

    private String description;

    private String posterUrl;

    private Integer totalEpisodes;

    private LocalDateTime createTime;
}
