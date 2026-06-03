package com.hd.biz.impl;

import com.hd.biz.HomeAggregationBiz;
import com.hd.common.enums.FileTypeEnum;
import com.hd.common.util.FileSizeFormatter;
import com.hd.dao.entity.*;
import com.hd.dao.service.*;
import com.hd.model.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 首页媒体聚合业务实现类
 * 负责聚合首页媒体工作台所需数据，支持缓存
 *
 * @author xhx
 * @since 2026-04-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HomeAggregationBizImpl implements HomeAggregationBiz {

    private final FileDataService fileDataService;
    private final MediaPictureMetadataDataService pictureMetadataDataService;
    private final MediaVideoMetadataDataService videoMetadataDataService;
    private final MediaAudioMetadataDataService audioMetadataDataService;
    private final PlayHistoryDataService playHistoryDataService;
    private final WatchProgressDataService watchProgressDataService;
    private final MediaScanTaskDataService scanTaskDataService;
    private final FavoriteDataService favoriteDataService;

    @Override
    public HomeMediaSummaryDTO getHomeSummary() {
        log.info("[HomeAggregation] 开始聚合首页数据");

        List<File> recentFiles = fileDataService.lambdaQuery()
                .orderByDesc(File::getCreateTime)
                .last("LIMIT 20")
                .list();

        List<File> recentImages = recentFiles.stream()
                .filter(f -> FileTypeEnum.PICTURE.toString().equals(f.getType()))
                .limit(5)
                .collect(Collectors.toList());
        List<File> recentVideos = recentFiles.stream()
                .filter(f -> FileTypeEnum.VIDEO.toString().equals(f.getType()))
                .limit(5)
                .collect(Collectors.toList());
        List<File> recentAudio = recentFiles.stream()
                .filter(f -> FileTypeEnum.AUDIO.toString().equals(f.getType()))
                .limit(5)
                .collect(Collectors.toList());

        List<File> allPictures = fileDataService.lambdaQuery()
                .eq(File::getType, FileTypeEnum.PICTURE.toString())
                .orderByDesc(File::getCreateTime)
                .last("LIMIT 4")
                .list();

        // Batch-load video and audio metadata to avoid N+1 queries in convertToMediaItem
        List<File> allMediaFiles = new ArrayList<>();
        allMediaFiles.addAll(recentVideos);
        allMediaFiles.addAll(recentAudio);

        List<Long> videoFileIds = allMediaFiles.stream()
                .filter(f -> FileTypeEnum.VIDEO.toString().equals(f.getType()))
                .map(File::getId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, MediaVideoMetadata> videoMetadataMap = videoFileIds.isEmpty() ? Map.of()
                : safeMapQuery("video metadata batch", () -> videoMetadataDataService.lambdaQuery()
                        .in(MediaVideoMetadata::getFileId, videoFileIds)
                        .list().stream()
                        .collect(Collectors.toMap(MediaVideoMetadata::getFileId, m -> m, (a, b) -> a)));

        List<Long> audioFileIds = allMediaFiles.stream()
                .filter(f -> FileTypeEnum.AUDIO.toString().equals(f.getType()))
                .map(File::getId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, MediaAudioMetadata> audioMetadataMap = audioFileIds.isEmpty() ? Map.of()
                : safeMapQuery("audio metadata batch", () -> audioMetadataDataService.lambdaQuery()
                        .in(MediaAudioMetadata::getFileId, audioFileIds)
                        .list().stream()
                        .collect(Collectors.toMap(MediaAudioMetadata::getFileId, m -> m, (a, b) -> a)));

        long imageCount = fileDataService.lambdaQuery().eq(File::getType, FileTypeEnum.PICTURE.toString()).count();
        long videoCount = fileDataService.lambdaQuery().eq(File::getType, FileTypeEnum.VIDEO.toString()).count();
        long audioCount = fileDataService.lambdaQuery().eq(File::getType, FileTypeEnum.AUDIO.toString()).count();

        long imageSize = sumFileSize(FileTypeEnum.PICTURE.toString());
        long videoSize = sumFileSize(FileTypeEnum.VIDEO.toString());
        long audioSize = sumFileSize(FileTypeEnum.AUDIO.toString());

        List<MediaScanTask> pendingTasks = safeListQuery("pending media tasks", () -> scanTaskDataService.lambdaQuery()
                .in(MediaScanTask::getStatus, List.of("PENDING", "RUNNING", "FAILED"))
                .orderByDesc(MediaScanTask::getCreateTime)
                .last("LIMIT 5")
                .list());

        HomeMediaSummaryDTO summary = HomeMediaSummaryDTO.builder()
                .recentUploads(HomeMediaSummaryDTO.RecentUploads.builder()
                        .images(recentImages.stream().map(f -> convertToMediaItem(f, videoMetadataMap, audioMetadataMap)).collect(Collectors.toList()))
                        .videos(recentVideos.stream().map(f -> convertToMediaItem(f, videoMetadataMap, audioMetadataMap)).collect(Collectors.toList()))
                        .audio(recentAudio.stream().map(f -> convertToMediaItem(f, videoMetadataMap, audioMetadataMap)).collect(Collectors.toList()))
                        .build())
                .recentPlays(HomeMediaSummaryDTO.RecentPlays.builder()
                        .videos(getRecentVideoPlays())
                        .audio(getRecentAudioPlays())
                        .build())
                .imageReview(allPictures.stream().map(f -> convertToMediaItem(f, videoMetadataMap, audioMetadataMap)).collect(Collectors.toList()))
                .mediaStats(HomeMediaSummaryDTO.MediaStats.builder()
                        .imageCount(imageCount)
                        .imageCapacity(FileSizeFormatter.format(imageSize))
                        .videoCount(videoCount)
                        .videoCapacity(FileSizeFormatter.format(videoSize))
                        .audioCount(audioCount)
                        .audioCapacity(FileSizeFormatter.format(audioSize))
                        .build())
                .favoriteSummary(HomeMediaSummaryDTO.RecentUploads.builder()
                        .images(new ArrayList<>())
                        .videos(new ArrayList<>())
                        .audio(new ArrayList<>())
                        .build())
                .pendingTasks(pendingTasks.stream().map(t -> MediaTaskDTO.builder()
                        .taskId(t.getId())
                        .mediaType(t.getMediaType())
                        .taskType(t.getTaskType())
                        .status(t.getStatus())
                        .errorMessage(t.getErrorMessage())
                        .createTime(t.getCreateTime())
                        .build()).collect(Collectors.toList()))
                .build();

        log.info("[HomeAggregation] 首页数据聚合完成");
        return summary;
    }

    private List<HomeMediaSummaryDTO.VideoPlayItem> getRecentVideoPlays() {
        List<PlayHistory> histories = safeListQuery("recent video plays", () -> playHistoryDataService.lambdaQuery()
                .eq(PlayHistory::getMediaType, "VIDEO")
                .orderByDesc(PlayHistory::getPlayTime)
                .last("LIMIT 5")
                .list());

        List<Long> fileIds = histories.stream().map(PlayHistory::getFileId).distinct().collect(Collectors.toList());
        if (fileIds.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, File> fileMap = fileDataService.listByIds(fileIds).stream()
                .collect(Collectors.toMap(File::getId, f -> f));
        Map<Long, WatchProgress> progressMap = safeMapQuery("video watch progress", () -> watchProgressDataService.lambdaQuery()
                .in(WatchProgress::getFileId, fileIds)
                .list().stream()
                .collect(Collectors.toMap(WatchProgress::getFileId, p -> p)));
        Map<Long, MediaVideoMetadata> metadataMap = safeMapQuery("video metadata", () -> videoMetadataDataService.lambdaQuery()
                .in(MediaVideoMetadata::getFileId, fileIds)
                .list().stream()
                .collect(Collectors.toMap(MediaVideoMetadata::getFileId, m -> m)));

        return histories.stream()
                .filter(h -> fileMap.containsKey(h.getFileId()))
                .map(h -> {
                    File file = fileMap.get(h.getFileId());
                    WatchProgress progress = progressMap.get(h.getFileId());
                    MediaVideoMetadata metadata = metadataMap.get(h.getFileId());
                    return HomeMediaSummaryDTO.VideoPlayItem.builder()
                            .fileId(h.getFileId())
                            .fileName(file.getFileName())
                            .coverUrl(normalizeAccessibleUrl(metadata != null ? metadata.getCoverPath() : null))
                            .progress(progress != null && progress.getProgressPercent() != null ? progress.getProgressPercent().intValue() : 0)
                            .lastPlayedAt(h.getPlayTime() != null ? h.getPlayTime().toString() : null)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<HomeMediaSummaryDTO.AudioPlayItem> getRecentAudioPlays() {
        List<PlayHistory> histories = safeListQuery("recent audio plays", () -> playHistoryDataService.lambdaQuery()
                .eq(PlayHistory::getMediaType, "AUDIO")
                .orderByDesc(PlayHistory::getPlayTime)
                .last("LIMIT 5")
                .list());

        List<Long> fileIds = histories.stream().map(PlayHistory::getFileId).distinct().collect(Collectors.toList());
        if (fileIds.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, File> fileMap = fileDataService.listByIds(fileIds).stream()
                .collect(Collectors.toMap(File::getId, f -> f));
        Map<Long, MediaAudioMetadata> metadataMap = audioMetadataDataService.lambdaQuery()
                .in(MediaAudioMetadata::getFileId, fileIds)
                .list().stream()
                .collect(Collectors.toMap(MediaAudioMetadata::getFileId, m -> m));

        return histories.stream()
                .filter(h -> fileMap.containsKey(h.getFileId()))
                .map(h -> {
                    File file = fileMap.get(h.getFileId());
                    MediaAudioMetadata metadata = metadataMap.get(h.getFileId());
                    return HomeMediaSummaryDTO.AudioPlayItem.builder()
                            .fileId(h.getFileId())
                            .fileName(file.getFileName())
                            .coverUrl(normalizeAccessibleUrl(metadata != null ? metadata.getCoverPath() : null))
                            .title(metadata != null ? metadata.getTitle() : file.getFileName())
                            .artist(metadata != null ? metadata.getArtist() : null)
                            .lastPlayedAt(h.getPlayTime() != null ? h.getPlayTime().toString() : null)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private MediaItemDTO convertToMediaItem(File file) {
        return convertToMediaItem(file, Map.of(), Map.of());
    }

    private MediaItemDTO convertToMediaItem(File file, Map<Long, MediaVideoMetadata> videoMetadataMap,
                                             Map<Long, MediaAudioMetadata> audioMetadataMap) {
        String thumbnailUrl = null;
        String coverUrl = null;
        if (FileTypeEnum.PICTURE.toString().equals(file.getType())) {
            thumbnailUrl = buildPictureThumbnailUrl(file.getResourceId());
        } else if (FileTypeEnum.VIDEO.toString().equals(file.getType())) {
            MediaVideoMetadata metadata = videoMetadataMap.get(file.getId());
            coverUrl = normalizeAccessibleUrl(metadata != null ? metadata.getCoverPath() : null);
        } else if (FileTypeEnum.AUDIO.toString().equals(file.getType())) {
            MediaAudioMetadata metadata = audioMetadataMap.get(file.getId());
            coverUrl = normalizeAccessibleUrl(metadata != null ? metadata.getCoverPath() : null);
        }

        return MediaItemDTO.builder()
                .fileId(file.getId())
                .parentId(file.getParentId())
                .resourceId(file.getResourceId())
                .fileName(file.getFileName())
                .size(file.getSize())
                .type(file.getType())
                .createTime(file.getCreateTime() != null ? file.getCreateTime().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null)
                .thumbnailUrl(thumbnailUrl)
                .coverUrl(coverUrl)
                .build();
    }

    private long sumFileSize(String type) {
        try {
            Map<String, Object> map = fileDataService.getMap(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<File>()
                            .select("COALESCE(SUM(size), 0) as total_size")
                            .lambda()
                            .eq(File::getType, type)
            );
            if (map != null && map.get("total_size") != null) {
                return ((Number) map.get("total_size")).longValue();
            }
        } catch (Exception e) {
            log.warn("[HomeAggregation] Failed to sum file size for type {}: {}", type, e.getMessage());
        }
        return 0L;
    }

    private String buildPictureThumbnailUrl(Long resourceId) {
        return resourceId != null ? "/v1/preview/image/" + resourceId + "/thumbnail" : null;
    }

    private String normalizeAccessibleUrl(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        if (path.startsWith("http://") || path.startsWith("https://") || path.startsWith("/")) {
            return path;
        }
        return null;
    }

    private <T> List<T> safeListQuery(String queryName, Supplier<List<T>> supplier) {
        try {
            return supplier.get();
        } catch (Exception exception) {
            log.warn("[HomeAggregation] Fallback to empty list for {}: {}", queryName, exception.getMessage());
            return new ArrayList<>();
        }
    }

    private <K, V> Map<K, V> safeMapQuery(String queryName, Supplier<Map<K, V>> supplier) {
        try {
            return supplier.get();
        } catch (Exception exception) {
            log.warn("[HomeAggregation] Fallback to empty map for {}: {}", queryName, exception.getMessage());
            return Map.of();
        }
    }
}
