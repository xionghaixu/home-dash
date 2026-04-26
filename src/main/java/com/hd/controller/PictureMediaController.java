package com.hd.controller;

import com.hd.biz.PictureMediaBiz;
import com.hd.model.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 图片媒体控制器
 *
 * @author system
 * @since 2026-04-26
 */
@RestController
@RequestMapping("/v1/media")
@RequiredArgsConstructor
public class PictureMediaController {

    private final PictureMediaBiz pictureMediaBiz;

    @GetMapping("/images")
    public ResponseDto getPictureList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "40") Integer pageSize,
            @RequestParam(defaultValue = "createTime") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Long albumId,
            @RequestParam(required = false) String directory) {
        PageResponseDto<PictureListDto> result = pictureMediaBiz.getPictureList(page, pageSize, sortBy, sortOrder, year, month, albumId, directory);
        return ResponseDto.success(result);
    }

    @GetMapping("/images/{fileId}")
    public ResponseDto getPictureDetail(@PathVariable Long fileId) {
        PictureDetailDto detail = pictureMediaBiz.getPictureDetail(fileId);
        return ResponseDto.success(detail);
    }

    @GetMapping("/images/timeline")
    public ResponseDto getPictureTimeline(@RequestParam String groupBy) {
        List<PictureTimelineGroupDto> timeline = pictureMediaBiz.getPictureTimeline(groupBy);
        return ResponseDto.success(timeline);
    }

    @GetMapping("/albums")
    public ResponseDto getAlbumList(@RequestParam(required = false) String albumType) {
        List<AlbumDto> albums = pictureMediaBiz.getAlbumList(albumType);
        return ResponseDto.success(albums);
    }

    @PostMapping("/albums")
    public ResponseDto createAlbum(@RequestBody AlbumDto albumDto) {
        AlbumDto album = pictureMediaBiz.createAlbum(albumDto.getAlbumName(), albumDto.getDescription(), albumDto.getCoverFileId());
        return ResponseDto.success(album);
    }

    @PutMapping("/albums/{albumId}")
    public ResponseDto updateAlbum(@PathVariable Long albumId, @RequestBody AlbumDto albumDto) {
        pictureMediaBiz.updateAlbum(albumId, albumDto.getAlbumName(), albumDto.getDescription(), albumDto.getCoverFileId());
        return ResponseDto.success();
    }

    @DeleteMapping("/albums/{albumId}")
    public ResponseDto deleteAlbum(@PathVariable Long albumId) {
        pictureMediaBiz.deleteAlbum(albumId);
        return ResponseDto.success();
    }

    @PostMapping("/albums/{albumId}/items")
    public ResponseDto addPictureToAlbum(@PathVariable Long albumId, @RequestParam Long fileId) {
        pictureMediaBiz.addPictureToAlbum(albumId, fileId);
        return ResponseDto.success();
    }

    @DeleteMapping("/albums/{albumId}/items/{fileId}")
    public ResponseDto removePictureFromAlbum(@PathVariable Long albumId, @PathVariable Long fileId) {
        pictureMediaBiz.removePictureFromAlbum(albumId, fileId);
        return ResponseDto.success();
    }

    @GetMapping("/albums/{albumId}/items")
    public ResponseDto getAlbumPictures(
            @PathVariable Long albumId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "40") Integer pageSize) {
        PageResponseDto<PictureListDto> result = pictureMediaBiz.getAlbumPictures(albumId, page, pageSize);
        return ResponseDto.success(result);
    }
}
