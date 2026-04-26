package com.hd.biz.impl;

import com.hd.biz.VideoMediaBiz;
import com.hd.common.enums.ErrorCode;
import com.hd.common.enums.FileType;
import com.hd.common.exception.BusinessException;
import com.hd.common.exception.DataNotFoundException;
import com.hd.dao.entity.*;
import com.hd.dao.service.*;
import com.hd.model.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 视频媒体业务实现类
 *
 * @author system
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
    public PageResponseDto<VideoListDto> getVideoList(Integer page, Integer pageSize, String sortBy, String sortOrder,
                                                       Long seriesId, Boolean hasSubtitle, String resolution) {
        int current = page != null && page > 0 ? page : 1;
        int size = pageSize != null && pageSize > 0 ? pageSize : 20;

        List<File> files = fileDataService.lambdaQuery()
                .eq(File::getType, FileType.VIDEO.toString())
                .orderByDesc(File::getCreateTime)
                .list();

        if (seriesId != null) {
            List<Long> seriesFileIds = episodeDataService.lambdaQuery()
                    .eq(MediaVideoEpisode::getSeriesId, seriesId)
                    .list().stream()
                    .map(MediaVideoEpisode::getFileId)
                    .collect(Collectors.toList());
            files = files.stream().filter(f -> seriesFileIds.contains(f.getId())).collect(Collectors.toList());
        }

        long total = files.size();
        int fromIndex = (current - 1) * size;
        int toIndex = Math.min(fromIndex + size, files.size());
        List<File> pageFiles = fromIndex < files.size() ? files.subList(fromIndex, toIndex) : new ArrayList<>();

        List<VideoListDto> list = pageFiles.stream().map(this::convertToVideoListDto).collect(Collectors.toList());
        return PageResponseDto.of(list, total, current, size);
    }

    @Override
    public VideoDetailDto getVideoDetail(Long fileId) {
        File file = fileDataService.getById(fileId);
        if (file == null || !FileType.VIDEO.toString().equals(file.getType())) {
            throw new DataNotFoundException(String.format("视频不存在 [fileId=%d]", fileId));
        }

        MediaVideoMetadata metadata = videoMetadataDataService.lambdaQuery()
                .eq(MediaVideoMetadata::getFileId, fileId)
                .one();

        MediaVideoEpisode episode = episodeDataService.lambdaQuery()
                .eq(MediaVideoEpisode::getFileId, fileId)
                .one();

        VideoDetailDto.VideoDetailDtoBuilder builder = VideoDetailDto.builder()
                .fileId(fileId)
                .fileName(file.getFileName())
                .size(file.getSize())
                .coverUrl("/thumb/" + fileId + "_cover.jpg")
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
    public WatchProgressDto getWatchProgress(Long fileId) {
        WatchProgress progress = watchProgressDataService.lambdaQuery()
                .eq(WatchProgress::getFileId, fileId)
                .one();
        if (progress == null) {
            return null;
        }
        return WatchProgressDto.builder()
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
    public List<SubtitleDto> getSubtitleList(Long fileId) {
        return subtitleDataService.lambdaQuery()
                .eq(Subtitle::getFileId, fileId)
                .list().stream()
                .map(s -> SubtitleDto.builder()
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
    public List<VideoSeriesDto> getSeriesList() {
        return seriesDataService.list().stream()
                .map(s -> VideoSeriesDto.builder()
                        .seriesId(s.getId())
                        .seriesName(s.getSeriesName())
                        .description(s.getDescription())
                        .posterUrl(s.getPosterPath())
                        .totalEpisodes(s.getTotalEpisodes())
                        .createTime(s.getCreateTime())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VideoSeriesDto createSeries(String seriesName, String description) {
        MediaVideoSeries series = new MediaVideoSeries();
        series.setSeriesName(seriesName);
        series.setDescription(description);
        series.setTotalEpisodes(0);
        seriesDataService.save(series);
        return VideoSeriesDto.builder()
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
            throw new BusinessException(ErrorCode.VIDEO_SERIES_NOT_FOUND);
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
            throw new BusinessException(ErrorCode.VIDEO_SERIES_NOT_FOUND);
        }
        episodeDataService.lambdaUpdate().eq(MediaVideoEpisode::getSeriesId, seriesId).remove();
        seriesDataService.removeById(seriesId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addEpisodeToSeries(Long seriesId, Long fileId, Integer episodeNumber, Integer seasonNumber, String episodeTitle) {
        MediaVideoSeries series = seriesDataService.getById(seriesId);
        if (series == null) {
            throw new BusinessException(ErrorCode.VIDEO_SERIES_NOT_FOUND);
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
    public List<VideoEpisodeDto> getSeriesEpisodes(Long seriesId) {
        return episodeDataService.lambdaQuery()
                .eq(MediaVideoEpisode::getSeriesId, seriesId)
                .orderByAsc(MediaVideoEpisode::getEpisodeNumber)
                .list().stream()
                .map(e -> VideoEpisodeDto.builder()
                        .episodeId(e.getId())
                        .fileId(e.getFileId())
                        .episodeNumber(e.getEpisodeNumber())
                        .seasonNumber(e.getSeasonNumber())
                        .episodeTitle(e.getEpisodeTitle())
                        .coverUrl("/thumb/" + e.getFileId() + "_cover.jpg")
                        .watchProgress(getWatchProgress(e.getFileId()))
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<VideoListDto> getContinueWatchList(Integer limit) {
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

        List<File> files = fileDataService.listByIds(fileIds);
        return files.stream()
                .filter(f -> FileType.VIDEO.toString().equals(f.getType()))
                .map(this::convertToVideoListDto)
                .collect(Collectors.toList());
    }

    private VideoListDto convertToVideoListDto(File file) {
        MediaVideoMetadata metadata = videoMetadataDataService.lambdaQuery()
                .eq(MediaVideoMetadata::getFileId, file.getId())
                .one();

        MediaVideoEpisode episode = episodeDataService.lambdaQuery()
                .eq(MediaVideoEpisode::getFileId, file.getId())
                .one();

        VideoListDto.VideoListDtoBuilder builder = VideoListDto.builder()
                .fileId(file.getId())
                .fileName(file.getFileName())
                .size(file.getSize())
                .coverUrl("/thumb/" + file.getId() + "_cover.jpg")
                .watchProgress(getWatchProgress(file.getId()));

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
            builder.seriesId(episode.getSeriesId());
            MediaVideoSeries series = seriesDataService.getById(episode.getSeriesId());
            if (series != null) {
                builder.seriesName(series.getSeriesName());
            }
        }

        return builder.build();
    }
}
