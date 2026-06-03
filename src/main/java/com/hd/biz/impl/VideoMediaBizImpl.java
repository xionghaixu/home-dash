package com.hd.biz.impl;

import com.hd.biz.VideoMediaBiz;
import com.hd.common.enums.ErrorCodeEnum;
import com.hd.common.enums.FileTypeEnum;
import com.hd.common.exception.BusinessException;
import com.hd.common.exception.DataNotFoundException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hd.dao.entity.*;
import com.hd.dao.service.*;
import com.hd.model.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 视频媒体业务实现类
 *
 * @author xhx
 * @since 2026-04-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoMediaBizImpl implements VideoMediaBiz {

    private final FileDataService fileDataService;
    private final MediaVideoMetadataDataService videoMetadataDataService;
    private final MediaVideoSeriesDataService seriesDataService;
    private final MediaVideoEpisodeDataService episodeDataService;
    private final WatchProgressDataService watchProgressDataService;
    private final SubtitleDataService subtitleDataService;
    private final PlayHistoryDataService playHistoryDataService;

    @Override
    public PageResponseDTO<VideoListDTO> getVideoList(Integer page, Integer pageSize, String sortBy, String sortOrder,
                                                       Long seriesId, Boolean hasSubtitle, String resolution) {
        int current = page != null && page > 0 ? page : 1;
        int size = pageSize != null && pageSize > 0 ? pageSize : 20;

        LambdaQueryWrapper<File> query = new LambdaQueryWrapper<>();
        query.eq(File::getType, FileTypeEnum.VIDEO.toString())
                .eq(File::getIsDeleted, 0);

        if (seriesId != null) {
            List<Long> seriesFileIds = episodeDataService.lambdaQuery()
                    .eq(MediaVideoEpisode::getSeriesId, seriesId)
                    .list().stream()
                    .map(MediaVideoEpisode::getFileId)
                    .collect(Collectors.toList());
            if (seriesFileIds.isEmpty()) {
                return PageResponseDTO.of(new ArrayList<>(), 0L, current, size);
            }
            query.in(File::getId, seriesFileIds);
        }

        if (hasSubtitle != null || StringUtils.hasText(resolution)) {
            LambdaQueryWrapper<MediaVideoMetadata> metaQuery = new LambdaQueryWrapper<>();
            if (hasSubtitle != null) {
                metaQuery.eq(MediaVideoMetadata::getHasSubtitle, hasSubtitle);
            }
            if (StringUtils.hasText(resolution)) {
                metaQuery.eq(MediaVideoMetadata::getResolution, resolution.trim());
            }
            List<Long> matchedFileIds = videoMetadataDataService.list(metaQuery).stream()
                    .map(MediaVideoMetadata::getFileId)
                    .collect(Collectors.toList());
            if (matchedFileIds.isEmpty()) {
                return PageResponseDTO.of(new ArrayList<>(), 0L, current, size);
            }
            query.in(File::getId, matchedFileIds);
        }

        long total = fileDataService.count(query);
        if (total == 0) {
            return PageResponseDTO.of(new ArrayList<>(), 0L, current, size);
        }

        String normalizedSortBy = StringUtils.hasText(sortBy) ? sortBy.trim().toLowerCase() : "createtime";
        boolean isAsc = "asc".equalsIgnoreCase(sortOrder);
        
        switch (normalizedSortBy) {
            case "name", "filename" -> {
                if (isAsc) query.orderByAsc(File::getFileName);
                else query.orderByDesc(File::getFileName);
            }
            case "size" -> {
                if (isAsc) query.orderByAsc(File::getSize);
                else query.orderByDesc(File::getSize);
            }
            case "duration" -> {
                String direction = isAsc ? "ASC" : "DESC";
                query.last("ORDER BY (SELECT duration FROM media_video_metadata WHERE file_id = id) " + direction);
            }
            case "resolution" -> {
                String direction = isAsc ? "ASC" : "DESC";
                query.last("ORDER BY (SELECT resolution FROM media_video_metadata WHERE file_id = id) " + direction);
            }
            default -> {
                if (isAsc) query.orderByAsc(File::getCreateTime);
                else query.orderByDesc(File::getCreateTime);
            }
        }

        Page<File> filePage = fileDataService.page(new Page<>(current, size), query);
        List<File> pageFiles = filePage.getRecords();

        List<Long> fileIds = pageFiles.stream().map(File::getId).collect(Collectors.toList());
        Map<Long, MediaVideoMetadata> metadataMap = videoMetadataDataService.lambdaQuery()
                .in(MediaVideoMetadata::getFileId, fileIds)
                .list().stream()
                .collect(Collectors.toMap(MediaVideoMetadata::getFileId, m -> m, (l, r) -> l));
        Map<Long, MediaVideoEpisode> episodeMap = episodeDataService.lambdaQuery()
                .in(MediaVideoEpisode::getFileId, fileIds)
                .list().stream()
                .collect(Collectors.toMap(MediaVideoEpisode::getFileId, e -> e, (l, r) -> l));

        Map<Long, WatchProgressDTO> progressMap = batchLoadWatchProgress(fileIds);
        List<Long> seriesIds = episodeMap.values().stream()
                .map(MediaVideoEpisode::getSeriesId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, MediaVideoSeries> seriesMap = batchLoadSeries(seriesIds);

        List<VideoListDTO> list = pageFiles.stream()
                .map(file -> convertToVideoListDTO(metadataMap.get(file.getId()), file, metadataMap, episodeMap, progressMap, seriesMap))
                .collect(Collectors.toList());
        return PageResponseDTO.of(list, total, current, size);
    }

    @Override
    public VideoDetailDTO getVideoDetail(Long fileId) {
        File file = fileDataService.getById(fileId);
        if (file == null || !FileTypeEnum.VIDEO.toString().equals(file.getType())) {
            throw new DataNotFoundException(String.format("视频不存在 [fileId=%d]", fileId));
        }

        MediaVideoMetadata metadata = videoMetadataDataService.lambdaQuery()
                .eq(MediaVideoMetadata::getFileId, fileId)
                .one();

        MediaVideoEpisode episode = episodeDataService.lambdaQuery()
                .eq(MediaVideoEpisode::getFileId, fileId)
                .one();

        VideoDetailDTO.VideoDetailDTOBuilder builder = VideoDetailDTO.builder()
                .fileId(fileId)
                .fileName(file.getFileName())
                .size(file.getSize())
                .coverUrl(normalizeAccessibleUrl(metadata != null ? metadata.getCoverPath() : null))
                .watchProgress(getWatchProgress(fileId))
                .subtitleList(getSubtitleList(fileId))
                .favorite(false);

        if (metadata != null) {
            builder.duration(metadata.getDuration())
                    .resolution(metadata.getResolution())
                    .width(metadata.getWidth())
                    .height(metadata.getHeight())
                    .bitrate(metadata.getBitrate())
                    .hasSubtitle(metadata.getHasSubtitle())
                    .hasAudio(metadata.getHasAudio());
        }

        if (episode != null) {
            builder.seriesId(episode.getSeriesId())
                    .episodeNumber(episode.getEpisodeNumber());
            MediaVideoSeries series = seriesDataService.getById(episode.getSeriesId());
            if (series != null) {
                builder.seriesName(series.getSeriesName());
            }
        }

        return builder.build();
    }

    @Override
    public WatchProgressDTO getWatchProgress(Long fileId) {
        WatchProgress progress = watchProgressDataService.lambdaQuery()
                .eq(WatchProgress::getFileId, fileId)
                .one();
        if (progress == null) {
            return null;
        }
        return WatchProgressDTO.builder()
                .currentPosition(progress.getCurrentPosition())
                .progressPercent(progress.getProgressPercent())
                .finished(progress.getFinished())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWatchProgress(Long fileId, Long currentPosition, Long duration) {
        WatchProgress progress = watchProgressDataService.lambdaQuery()
                .eq(WatchProgress::getFileId, fileId)
                .one();

        boolean finished = duration != null && duration > 0 && currentPosition * 100 / duration >= 90;

        if (progress == null) {
            progress = new WatchProgress();
            progress.setFileId(fileId);
            progress.setCurrentPosition(currentPosition);
            progress.setDuration(duration);
            progress.setFinished(finished);
            progress.setLastWatched(LocalDateTime.now());
            watchProgressDataService.save(progress);
        } else {
            progress.setCurrentPosition(currentPosition);
            progress.setDuration(duration);
            progress.setFinished(finished);
            progress.setLastWatched(LocalDateTime.now());
            watchProgressDataService.updateById(progress);
        }

        PlayHistory history = new PlayHistory();
        history.setFileId(fileId);
        history.setMediaType("VIDEO");
        history.setPlayTime(LocalDateTime.now());
        history.setPlayDuration(0L);
        playHistoryDataService.save(history);
    }

    @Override
    public List<SubtitleDTO> getSubtitleList(Long fileId) {
        return subtitleDataService.lambdaQuery()
                .eq(Subtitle::getFileId, fileId)
                .list().stream()
                .map(s -> SubtitleDTO.builder()
                        .subtitleId(s.getId())
                        .language(s.getLanguage())
                        .subtitleType(s.getSubtitleType())
                        .format(s.getFormat())
                        .isDefault(s.getIsDefault())
                        .url("/v1/media/subtitles/" + s.getId() + "/content")
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<VideoSeriesDTO> getSeriesList() {
        return seriesDataService.list().stream()
                .map(s -> VideoSeriesDTO.builder()
                        .seriesId(s.getId())
                        .seriesName(s.getSeriesName())
                        .description(s.getDescription())
                        .posterUrl(normalizeAccessibleUrl(s.getPosterPath()))
                        .totalEpisodes(s.getTotalEpisodes())
                        .createTime(s.getCreateTime())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VideoSeriesDTO createSeries(String seriesName, String description) {
        MediaVideoSeries series = new MediaVideoSeries();
        series.setSeriesName(seriesName);
        series.setDescription(description);
        series.setTotalEpisodes(0);
        seriesDataService.save(series);
        return VideoSeriesDTO.builder()
                .seriesId(series.getId())
                .seriesName(series.getSeriesName())
                .description(series.getDescription())
                .totalEpisodes(0)
                .createTime(series.getCreateTime())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSeries(Long seriesId, String seriesName, String description) {
        MediaVideoSeries series = seriesDataService.getById(seriesId);
        if (series == null) {
            throw new BusinessException(ErrorCodeEnum.VIDEO_SERIES_NOT_FOUND);
        }
        series.setSeriesName(seriesName);
        series.setDescription(description);
        seriesDataService.updateById(series);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSeries(Long seriesId) {
        MediaVideoSeries series = seriesDataService.getById(seriesId);
        if (series == null) {
            throw new BusinessException(ErrorCodeEnum.VIDEO_SERIES_NOT_FOUND);
        }
        episodeDataService.lambdaUpdate().eq(MediaVideoEpisode::getSeriesId, seriesId).remove();
        seriesDataService.removeById(seriesId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addEpisodeToSeries(Long seriesId, Long fileId, Integer episodeNumber, Integer seasonNumber, String episodeTitle) {
        MediaVideoSeries series = seriesDataService.getById(seriesId);
        if (series == null) {
            throw new BusinessException(ErrorCodeEnum.VIDEO_SERIES_NOT_FOUND);
        }

        Long existing = episodeDataService.lambdaQuery()
                .eq(MediaVideoEpisode::getSeriesId, seriesId)
                .eq(MediaVideoEpisode::getFileId, fileId)
                .count();
        if (existing > 0) {
            throw new BusinessException("该文件已经是该剧集的一部分");
        }

        MediaVideoEpisode episode = new MediaVideoEpisode();
        episode.setSeriesId(seriesId);
        episode.setFileId(fileId);
        episode.setEpisodeNumber(episodeNumber);
        episode.setSeasonNumber(seasonNumber != null ? seasonNumber : 1);
        episode.setEpisodeTitle(episodeTitle);
        episodeDataService.save(episode);

        series.setTotalEpisodes(series.getTotalEpisodes() + 1);
        seriesDataService.updateById(series);
    }

    @Override
    public List<VideoEpisodeDTO> getSeriesEpisodes(Long seriesId) {
        List<MediaVideoEpisode> episodes = episodeDataService.lambdaQuery()
                .eq(MediaVideoEpisode::getSeriesId, seriesId)
                .orderByAsc(MediaVideoEpisode::getEpisodeNumber)
                .list();

        if (episodes.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> fileIds = episodes.stream().map(MediaVideoEpisode::getFileId).collect(Collectors.toList());
        Map<Long, MediaVideoMetadata> metadataMap = getVideoMetadataMap(fileIds);
        Map<Long, WatchProgressDTO> progressMap = batchLoadWatchProgress(fileIds);
        MediaVideoSeries series = seriesDataService.getById(seriesId);

        return episodes.stream()
                .map(e -> VideoEpisodeDTO.builder()
                        .episodeId(e.getId())
                        .fileId(e.getFileId())
                        .episodeNumber(e.getEpisodeNumber())
                        .seasonNumber(e.getSeasonNumber())
                        .episodeTitle(e.getEpisodeTitle())
                        .coverUrl(normalizeAccessibleUrl(
                                metadataMap.containsKey(e.getFileId()) ? metadataMap.get(e.getFileId()).getCoverPath() : null))
                        .watchProgress(progressMap.get(e.getFileId()))
                        .seriesName(series != null ? series.getSeriesName() : null)
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<VideoListDTO> getContinueWatchList(Integer limit) {
        List<WatchProgress> progresses = watchProgressDataService.lambdaQuery()
                .eq(WatchProgress::getFinished, false)
                .gt(WatchProgress::getCurrentPosition, 0)
                .orderByDesc(WatchProgress::getLastWatched)
                .last("LIMIT " + (limit != null ? limit : 10))
                .list();

        List<Long> fileIds = progresses.stream().map(WatchProgress::getFileId).collect(Collectors.toList());
        if (fileIds.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, File> fileMap = fileDataService.listByIds(fileIds).stream()
                .filter(f -> FileTypeEnum.VIDEO.toString().equals(f.getType()))
                .collect(Collectors.toMap(File::getId, file -> file));

        List<Long> validFileIds = fileMap.keySet().stream().collect(Collectors.toList());
        Map<Long, MediaVideoMetadata> metadataMap = getVideoMetadataMap(validFileIds);
        Map<Long, MediaVideoEpisode> episodeMap = episodeDataService.lambdaQuery()
                .in(MediaVideoEpisode::getFileId, validFileIds)
                .list().stream()
                .collect(Collectors.toMap(MediaVideoEpisode::getFileId, e -> e, (l, r) -> l));

        Map<Long, WatchProgressDTO> progressMap = progresses.stream()
                .collect(Collectors.toMap(WatchProgress::getFileId, this::toWatchProgressDTO, (a, b) -> a));
        List<Long> seriesIds = episodeMap.values().stream()
                .map(MediaVideoEpisode::getSeriesId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, MediaVideoSeries> seriesMap = batchLoadSeries(seriesIds);

        return progresses.stream()
                .map(WatchProgress::getFileId)
                .map(fileMap::get)
                .filter(Objects::nonNull)
                .map(file -> convertToVideoListDTO(metadataMap.get(file.getId()), file, metadataMap, episodeMap, progressMap, seriesMap))
                .collect(Collectors.toList());
    }

    private VideoListDTO convertToVideoListDTO(MediaVideoMetadata metadata, File file,
                                                Map<Long, MediaVideoMetadata> metadataMap,
                                                Map<Long, MediaVideoEpisode> episodeMap,
                                                Map<Long, WatchProgressDTO> progressMap,
                                                Map<Long, MediaVideoSeries> seriesMap) {
        if (file == null) {
            return null;
        }

        VideoListDTO.VideoListDTOBuilder builder = VideoListDTO.builder()
                .fileId(file.getId())
                .fileName(file.getFileName())
                .size(file.getSize())
                .coverUrl(normalizeAccessibleUrl(metadata != null ? metadata.getCoverPath() : null))
                .watchProgress(progressMap.get(file.getId()));

        if (metadata != null) {
            builder.duration(metadata.getDuration())
                    .resolution(metadata.getResolution())
                    .width(metadata.getWidth())
                    .height(metadata.getHeight())
                    .bitrate(metadata.getBitrate())
                    .hasSubtitle(metadata.getHasSubtitle())
                    .hasAudio(metadata.getHasAudio());
        }

        MediaVideoEpisode episode = episodeMap.get(file.getId());
        if (episode != null) {
            builder.seriesId(episode.getSeriesId());
            MediaVideoSeries series = seriesMap.get(episode.getSeriesId());
            if (series != null) {
                builder.seriesName(series.getSeriesName());
            }
        }

        return builder.build();
    }

    private WatchProgressDTO toWatchProgressDTO(WatchProgress progress) {
        if (progress == null) {
            return null;
        }
        return WatchProgressDTO.builder()
                .currentPosition(progress.getCurrentPosition())
                .progressPercent(progress.getProgressPercent())
                .finished(progress.getFinished())
                .build();
    }

    private Map<Long, WatchProgressDTO> batchLoadWatchProgress(List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return Map.of();
        }
        return watchProgressDataService.lambdaQuery()
                .in(WatchProgress::getFileId, fileIds)
                .list().stream()
                .collect(Collectors.toMap(WatchProgress::getFileId, this::toWatchProgressDTO, (a, b) -> a));
    }

    private Map<Long, MediaVideoSeries> batchLoadSeries(List<Long> seriesIds) {
        if (seriesIds == null || seriesIds.isEmpty()) {
            return Map.of();
        }
        return seriesDataService.listByIds(seriesIds).stream()
                .collect(Collectors.toMap(MediaVideoSeries::getId, s -> s, (a, b) -> a));
    }

    private Map<Long, MediaVideoMetadata> getVideoMetadataMap(List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return Map.of();
        }

        return videoMetadataDataService.lambdaQuery()
                .in(MediaVideoMetadata::getFileId, fileIds)
                .list().stream()
                .collect(Collectors.toMap(MediaVideoMetadata::getFileId, metadata -> metadata, (left, right) -> left));
    }

    private void applyVideoMetadataSorting(LambdaQueryWrapper<MediaVideoMetadata> query, String sortBy, boolean isAsc) {
        switch (sortBy) {
            case "name", "filename" -> {
                // file_name lives on the file table, use correlated subquery
                String direction = isAsc ? "ASC" : "DESC";
                query.last("ORDER BY (SELECT file_name FROM file WHERE file.id = file_id) " + direction);
            }
            case "duration" -> {
                if (isAsc) query.orderByAsc(MediaVideoMetadata::getDuration);
                else query.orderByDesc(MediaVideoMetadata::getDuration);
            }
            case "resolution" -> {
                if (isAsc) query.orderByAsc(MediaVideoMetadata::getResolution);
                else query.orderByDesc(MediaVideoMetadata::getResolution);
            }
            case "size" -> {
                // size lives on the file table, use correlated subquery
                String direction = isAsc ? "ASC" : "DESC";
                query.last("ORDER BY (SELECT size FROM file WHERE file.id = file_id) " + direction);
            }
            default -> {
                if (isAsc) query.orderByAsc(MediaVideoMetadata::getCreateTime);
                else query.orderByDesc(MediaVideoMetadata::getCreateTime);
            }
        }
    }

    private String normalizeAccessibleUrl(String path) {
        if (!StringUtils.hasText(path)) {
            return null;
        }
        if (path.startsWith("http://") || path.startsWith("https://") || path.startsWith("/")) {
            return path;
        }
        return null;
    }

    private String safeLowerCase(String value) {
        return value != null ? value.toLowerCase() : null;
    }
}
