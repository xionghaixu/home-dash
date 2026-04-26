package com.hd.biz;

import com.hd.model.dto.*;

import java.util.List;

/**
 * 视频媒体业务接口
 *
 * @author system
 * @since 2026-04-26
 */
public interface VideoMediaBiz {

    PageResponseDto<VideoListDto> getVideoList(Integer page, Integer pageSize, String sortBy, String sortOrder, Long seriesId, Boolean hasSubtitle, String resolution);

    VideoDetailDto getVideoDetail(Long fileId);

    WatchProgressDto getWatchProgress(Long fileId);

    void updateWatchProgress(Long fileId, Long currentPosition, Long duration);

    List<SubtitleDto> getSubtitleList(Long fileId);

    List<VideoSeriesDto> getSeriesList();

    VideoSeriesDto createSeries(String seriesName, String description);

    void updateSeries(Long seriesId, String seriesName, String description);

    void deleteSeries(Long seriesId);

    void addEpisodeToSeries(Long seriesId, Long fileId, Integer episodeNumber, Integer seasonNumber, String episodeTitle);

    List<VideoEpisodeDto> getSeriesEpisodes(Long seriesId);

    List<VideoListDto> getContinueWatchList(Integer limit);
}
