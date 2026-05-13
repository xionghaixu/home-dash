package com.hd.biz.impl;

import com.hd.biz.HomeAggregationBiz;
import com.hd.common.enums.FileType;
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
 * @author system
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
    public HomeMediaSummaryDto getHomeSummary() {
        log.info("[HomeAggregation] 开始聚合首页数据");

        List<File> recentFiles = fileDataService.lambdaQuery()
                .orderByDesc(File::getCreateTime)
                .last("LIMIT 20")
                .list();

        List<File> recentImages = recentFiles.stream()
                .filter(f -> FileType.PICTURE.toString().equals(f.getType()))
                .limit(5)
                .collect(Collectors.toList());
        List<File> recentVideos = recentFiles.stream()
                .filter(f -> FileType.VIDEO.toString().equals(f.getType()))
                .limit(5)
                .collect(Collectors.toList());
        List<File> recentAudio = recentFiles.stream()
                .filter(f -> FileType.AUDIO.toString().equals(f.getType()))
                .limit(5)
                .collect(Collectors.toList());

        List<File> allPictures = fileDataService.lambdaQuery()
                .eq(File::getType, FileType.PICTURE.toString())
                .orderByDesc(File::getCreateTime)
                .last("LIMIT 4")
                .list();

        long imageCount = fileDataService.lambdaQuery().eq(File::getType, FileType.PICTURE.toString()).count();
        long videoCount = fileDataService.lambdaQuery().eq(File::getType, FileType.VIDEO.toString()).count();
        long audioCount = fileDataService.lambdaQuery().eq(File::getType, FileType.AUDIO.toString()).count();

        long imageSize = sumFileSize(FileType.PICTURE.toString());
        long videoSize = sumFileSize(FileType.VIDEO.toString());
        long audioSize = sumFileSize(FileType.AUDIO.toString());

        List<MediaScanTask> pendingTasks = safeListQuery("pending media tasks", () -> scanTaskDataService.lambdaQuery()
                .in(MediaScanTask::getStatus, List.of("PENDING", "RUNNING", "FAILED"))
                .orderByDesc(MediaScanTask::getCreateTime)
                .last("LIMIT 5")
                .list());

        HomeMediaSummaryDto summary = HomeMediaSummaryDto.builder()
                .recentUploads(HomeMediaSummaryDto.RecentUploads.builder()
                        .images(recentImages.stream().map(this::convertToMediaItem).collect(Collectors.toList()))
                        .videos(recentVideos.stream().map(this::convertToMediaItem).collect(Collectors.toList()))
                        .audio(recentAudio.stream().map(this::convertToMediaItem).collect(Collectors.toList()))
                        .build())
                .recentPlays(HomeMediaSummaryDto.RecentPlays.builder()
                        .videos(getRecentVideoPlays())
                        .audio(getRecentAudioPlays())
                        .build())
                .imageReview(allPictures.stream().map(this::convertToMediaItem).collect(Collectors.toList()))
                .mediaStats(HomeMediaSummaryDto.MediaStats.builder()
                        .imageCount(imageCount)
                        .imageCapacity(FileSizeFormatter.format(imageSize))
                        .videoCount(videoCount)
                        .videoCapacity(FileSizeFormatter.format(videoSize))
                        .audioCount(audioCount)
                        .audioCapacity(FileSizeFormatter.format(audioSize))
                        .build())
                .favoriteSummary(HomeMediaSummaryDto.RecentUploads.builder()
                        .images(new ArrayList<>())
                        .videos(new ArrayList<>())
                        .audio(new ArrayList<>())
                        .build())
                .pendingTasks(pendingTasks.stream().map(t -> MediaTaskDto.builder()
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

    private List<HomeMediaSummaryDto.VideoPlayItem> getRecentVideoPlays() {
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

        return histories.stream()
                .filter(h -> fileMap.containsKey(h.getFileId()))
                .map(h -> {
                    File file = fileMap.get(h.getFileId());
                    WatchProgress progress = progressMap.get(h.getFileId());
                    MediaVideoMetadata metadata = videoMetadataDataService.lambdaQuery()
                            .eq(MediaVideoMetadata::getFileId, h.getFileId())
                            .one();
                    return HomeMediaSummaryDto.VideoPlayItem.builder()
                            .fileId(h.getFileId())
                            .fileName(file.getFileName())
                            .coverUrl(normalizeAccessibleUrl(metadata != null ? metadata.getCoverPath() : null))
                            .progress(progress != null && progress.getProgressPercent() != null ? progress.getProgressPercent().intValue() : 0)
                            .lastPlayedAt(h.getPlayTime() != null ? h.getPlayTime().toString() : null)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<HomeMediaSummaryDto.AudioPlayItem> getRecentAudioPlays() {
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
                    return HomeMediaSummaryDto.AudioPlayItem.builder()
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

    private MediaItemDto convertToMediaItem(File file) {
        String thumbnailUrl = null;
        String coverUrl = null;
        if (FileType.PICTURE.toString().equals(file.getType())) {
            thumbnailUrl = buildPictureThumbnailUrl(file.getResourceId());
        } else if (FileType.VIDEO.toString().equals(file.getType())) {
            MediaVideoMetadata metadata = videoMetadataDataService.lambdaQuery()
                    .eq(MediaVideoMetadata::getFileId, file.getId())
                    .one();
            coverUrl = normalizeAccessibleUrl(metadata != null ? metadata.getCoverPath() : null);
        } else if (FileType.AUDIO.toString().equals(file.getType())) {
            MediaAudioMetadata metadata = audioMetadataDataService.lambdaQuery()
                    .eq(MediaAudioMetadata::getFileId, file.getId())
                    .one();
            coverUrl = normalizeAccessibleUrl(metadata != null ? metadata.getCoverPath() : null);
        }

        return MediaItemDto.builder()
                .fileId(file.getId())
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
        return fileDataService.lambdaQuery()
                .eq(File::getType, type)
                .list().stream()
                .mapToLong(f -> f.getSize() != null ? f.getSize() : 0)
                .sum();
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
