package com.hd.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 首页媒体聚合响应DTO
 *
 * @author system
 * @since 2026-04-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeMediaSummaryDto {

    private RecentUploads recentUploads;

    private RecentPlays recentPlays;

    private List<MediaItemDto> imageReview;

    private MediaStats mediaStats;

    private RecentUploads favoriteSummary;

    private List<MediaTaskDto> pendingTasks;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentUploads {
        private List<MediaItemDto> images;
        private List<MediaItemDto> videos;
        private List<MediaItemDto> audio;
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
