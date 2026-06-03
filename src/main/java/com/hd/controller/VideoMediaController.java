package com.hd.controller;

import com.hd.biz.VideoMediaBiz;
import com.hd.model.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 视频媒体控制器
 *
 * @author xhx
 * @since 2026-04-26
 */
@RestController
@RequestMapping("/v1/media")
@RequiredArgsConstructor
public class VideoMediaController {

    private final VideoMediaBiz videoMediaBiz;

    @GetMapping("/videos")
    public ResponseEntity<ResponseDTO> getVideoList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(defaultValue = "createTime") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @RequestParam(required = false) Long seriesId,
            @RequestParam(required = false) Boolean hasSubtitle,
            @RequestParam(required = false) String resolution) {
        PageResponseDTO<VideoListDTO> result = videoMediaBiz.getVideoList(page, pageSize, sortBy, sortOrder, seriesId, hasSubtitle, resolution);
        return ResponseEntity.ok(ResponseDTO.success(result));
    }

    @GetMapping("/videos/{fileId}")
    public ResponseEntity<ResponseDTO> getVideoDetail(@PathVariable Long fileId) {
        VideoDetailDTO detail = videoMediaBiz.getVideoDetail(fileId);
        return ResponseEntity.ok(ResponseDTO.success(detail));
    }

    @GetMapping("/videos/{fileId}/progress")
    public ResponseEntity<ResponseDTO> getWatchProgress(@PathVariable Long fileId) {
        WatchProgressDTO progress = videoMediaBiz.getWatchProgress(fileId);
        return ResponseEntity.ok(ResponseDTO.success(progress));
    }

    @PostMapping("/videos/{fileId}/progress")
    public ResponseEntity<ResponseDTO> updateWatchProgress(@PathVariable Long fileId, @RequestBody UpdateProgressDTO progressDto) {
        videoMediaBiz.updateWatchProgress(fileId, progressDto.getCurrentPosition(), progressDto.getDuration());
        return ResponseEntity.ok(ResponseDTO.success());
    }

    @GetMapping("/videos/{fileId}/subtitles")
    public ResponseEntity<ResponseDTO> getSubtitleList(@PathVariable Long fileId) {
        List<SubtitleDTO> subtitles = videoMediaBiz.getSubtitleList(fileId);
        return ResponseEntity.ok(ResponseDTO.success(subtitles));
    }

    @GetMapping("/series")
    public ResponseEntity<ResponseDTO> getSeriesList() {
        List<VideoSeriesDTO> series = videoMediaBiz.getSeriesList();
        return ResponseEntity.ok(ResponseDTO.success(series));
    }

    @PostMapping("/series")
    public ResponseEntity<ResponseDTO> createSeries(@RequestBody VideoSeriesDTO seriesDto) {
        VideoSeriesDTO series = videoMediaBiz.createSeries(seriesDto.getSeriesName(), seriesDto.getDescription());
        return ResponseEntity.ok(ResponseDTO.success(series));
    }

    @PostMapping("/series/{seriesId}/update")
    public ResponseEntity<ResponseDTO> updateSeries(@PathVariable Long seriesId, @RequestBody VideoSeriesDTO seriesDto) {
        videoMediaBiz.updateSeries(seriesId, seriesDto.getSeriesName(), seriesDto.getDescription());
        return ResponseEntity.ok(ResponseDTO.success());
    }

    @PostMapping("/series/{seriesId}/delete")
    public ResponseEntity<ResponseDTO> deleteSeries(@PathVariable Long seriesId) {
        videoMediaBiz.deleteSeries(seriesId);
        return ResponseEntity.ok(ResponseDTO.success());
    }

    @PostMapping("/series/{seriesId}/episodes")
    public ResponseEntity<ResponseDTO> addEpisodeToSeries(@PathVariable Long seriesId, @RequestBody VideoEpisodeDTO episodeDto) {
        videoMediaBiz.addEpisodeToSeries(seriesId, episodeDto.getFileId(), episodeDto.getEpisodeNumber(), episodeDto.getSeasonNumber(), episodeDto.getEpisodeTitle());
        return ResponseEntity.ok(ResponseDTO.success());
    }

    @GetMapping("/series/{seriesId}/videos")
    public ResponseEntity<ResponseDTO> getSeriesEpisodes(@PathVariable Long seriesId) {
        List<VideoEpisodeDTO> episodes = videoMediaBiz.getSeriesEpisodes(seriesId);
        return ResponseEntity.ok(ResponseDTO.success(episodes));
    }

    @GetMapping("/videos/continue-watch")
    public ResponseEntity<ResponseDTO> getContinueWatchList(@RequestParam(defaultValue = "10") Integer limit) {
        List<VideoListDTO> videos = videoMediaBiz.getContinueWatchList(limit);
        return ResponseEntity.ok(ResponseDTO.success(videos));
    }
}
