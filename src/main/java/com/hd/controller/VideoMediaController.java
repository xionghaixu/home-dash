package com.hd.controller;

import com.hd.biz.VideoMediaBiz;
import com.hd.model.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 视频媒体控制器
 *
 * @author system
 * @since 2026-04-26
 */
@RestController
@RequestMapping("/v1/media")
@RequiredArgsConstructor
public class VideoMediaController {

    private final VideoMediaBiz videoMediaBiz;

    @GetMapping("/videos")
    public ResponseDto getVideoList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(defaultValue = "createTime") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @RequestParam(required = false) Long seriesId,
            @RequestParam(required = false) Boolean hasSubtitle,
            @RequestParam(required = false) String resolution) {
        PageResponseDto<VideoListDto> result = videoMediaBiz.getVideoList(page, pageSize, sortBy, sortOrder, seriesId, hasSubtitle, resolution);
        return ResponseDto.success(result);
    }

    @GetMapping("/videos/{fileId}")
    public ResponseDto getVideoDetail(@PathVariable Long fileId) {
        VideoDetailDto detail = videoMediaBiz.getVideoDetail(fileId);
        return ResponseDto.success(detail);
    }

    @GetMapping("/videos/{fileId}/progress")
    public ResponseDto getWatchProgress(@PathVariable Long fileId) {
        WatchProgressDto progress = videoMediaBiz.getWatchProgress(fileId);
        return ResponseDto.success(progress);
    }

    @PostMapping("/videos/{fileId}/progress")
    public ResponseDto updateWatchProgress(@PathVariable Long fileId, @RequestBody UpdateProgressDto progressDto) {
        videoMediaBiz.updateWatchProgress(fileId, progressDto.getCurrentPosition(), progressDto.getDuration());
        return ResponseDto.success();
    }

    @GetMapping("/videos/{fileId}/subtitles")
    public ResponseDto getSubtitleList(@PathVariable Long fileId) {
        List<SubtitleDto> subtitles = videoMediaBiz.getSubtitleList(fileId);
        return ResponseDto.success(subtitles);
    }

    @GetMapping("/series")
    public ResponseDto getSeriesList() {
        List<VideoSeriesDto> series = videoMediaBiz.getSeriesList();
        return ResponseDto.success(series);
    }

    @PostMapping("/series")
    public ResponseDto createSeries(@RequestBody VideoSeriesDto seriesDto) {
        VideoSeriesDto series = videoMediaBiz.createSeries(seriesDto.getSeriesName(), seriesDto.getDescription());
        return ResponseDto.success(series);
    }

    @PutMapping("/series/{seriesId}")
    public ResponseDto updateSeries(@PathVariable Long seriesId, @RequestBody VideoSeriesDto seriesDto) {
        videoMediaBiz.updateSeries(seriesId, seriesDto.getSeriesName(), seriesDto.getDescription());
        return ResponseDto.success();
    }

    @DeleteMapping("/series/{seriesId}")
    public ResponseDto deleteSeries(@PathVariable Long seriesId) {
        videoMediaBiz.deleteSeries(seriesId);
        return ResponseDto.success();
    }

    @PostMapping("/series/{seriesId}/episodes")
    public ResponseDto addEpisodeToSeries(@PathVariable Long seriesId, @RequestBody VideoEpisodeDto episodeDto) {
        videoMediaBiz.addEpisodeToSeries(seriesId, episodeDto.getFileId(), episodeDto.getEpisodeNumber(), episodeDto.getSeasonNumber(), episodeDto.getEpisodeTitle());
        return ResponseDto.success();
    }

    @GetMapping("/series/{seriesId}/videos")
    public ResponseDto getSeriesEpisodes(@PathVariable Long seriesId) {
        List<VideoEpisodeDto> episodes = videoMediaBiz.getSeriesEpisodes(seriesId);
        return ResponseDto.success(episodes);
    }

    @GetMapping("/videos/continue-watch")
    public ResponseDto getContinueWatchList(@RequestParam(defaultValue = "10") Integer limit) {
        List<VideoListDto> videos = videoMediaBiz.getContinueWatchList(limit);
        return ResponseDto.success(videos);
    }
}
