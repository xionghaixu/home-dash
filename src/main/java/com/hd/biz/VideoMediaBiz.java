package com.hd.biz;

import com.hd.model.dto.*;

import java.util.List;

/**
 * 视频媒体业务接口
 *
 * @author xhx
 * @since 2026-04-26
 */
public interface VideoMediaBiz {

    PageResponseDTO<VideoListDTO> getVideoList(Integer page, Integer pageSize, String sortBy, String sortOrder, Long seriesId, Boolean hasSubtitle, String resolution);

    VideoDetailDTO getVideoDetail(Long fileId);

    WatchProgressDTO getWatchProgress(Long fileId);

    void updateWatchProgress(Long fileId, Long currentPosition, Long duration);

    List<SubtitleDTO> getSubtitleList(Long fileId);

    List<VideoSeriesDTO> getSeriesList();

    VideoSeriesDTO createSeries(String seriesName, String description);

    void updateSeries(Long seriesId, String seriesName, String description);

    void deleteSeries(Long seriesId);

    void addEpisodeToSeries(Long seriesId, Long fileId, Integer episodeNumber, Integer seasonNumber, String episodeTitle);

    List<VideoEpisodeDTO> getSeriesEpisodes(Long seriesId);

    List<VideoListDTO> getContinueWatchList(Integer limit);
}
