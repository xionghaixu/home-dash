package com.hd.model.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 首页媒体聚合响应DTO
 *
 * @author xhx
 * @since 2026-04-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeMediaSummaryDTO {

    private RecentUploads recentUploads;

    private RecentPlays recentPlays;

    private List<MediaItemDTO> imageReview;

    private MediaStats mediaStats;

    private RecentUploads favoriteSummary;

    private List<MediaTaskDTO> pendingTasks;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentUploads {
        private List<MediaItemDTO> images;
        private List<MediaItemDTO> videos;
        private List<MediaItemDTO> audio;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentPlays {
        private List<VideoPlayItem> videos;
        private List<AudioPlayItem> audio;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VideoPlayItem {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long fileId;
        private String fileName;
        private String coverUrl;
        private Integer progress;
        private String lastPlayedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AudioPlayItem {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long fileId;
        private String fileName;
        private String coverUrl;
        private String title;
        private String artist;
        private String lastPlayedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MediaStats {
        private Long imageCount;
        private String imageCapacity;
        private Long videoCount;
        private String videoCapacity;
        private Long audioCount;
        private String audioCapacity;
    }
}
