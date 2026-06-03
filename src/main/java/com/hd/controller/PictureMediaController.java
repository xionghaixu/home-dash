package com.hd.controller;

import com.hd.biz.PictureMediaBiz;
import com.hd.model.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 图片媒体控制器
 *
 * @author xhx
 * @since 2026-04-26
 */
@RestController
@RequestMapping("/v1/media")
@RequiredArgsConstructor
public class PictureMediaController {

    private final PictureMediaBiz pictureMediaBiz;

    @GetMapping("/images")
    public ResponseEntity<ResponseDTO> getPictureList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "40") Integer pageSize,
            @RequestParam(defaultValue = "createTime") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Long albumId,
            @RequestParam(required = false) String directory) {
        PageResponseDTO<PictureListDTO> result = pictureMediaBiz.getPictureList(page, pageSize, sortBy, sortOrder, year, month, albumId, directory);
        return ResponseEntity.ok(ResponseDTO.success(result));
    }

    @GetMapping("/images/{fileId}")
    public ResponseEntity<ResponseDTO> getPictureDetail(@PathVariable Long fileId) {
        PictureDetailDTO detail = pictureMediaBiz.getPictureDetail(fileId);
        return ResponseEntity.ok(ResponseDTO.success(detail));
    }

    @GetMapping("/images/timeline")
    public ResponseEntity<ResponseDTO> getPictureTimeline(@RequestParam String groupBy) {
        List<PictureTimelineGroupDTO> timeline = pictureMediaBiz.getPictureTimeline(groupBy);
        return ResponseEntity.ok(ResponseDTO.success(timeline));
    }

    @GetMapping("/albums")
    public ResponseEntity<ResponseDTO> getAlbumList(@RequestParam(required = false) String albumType) {
        List<AlbumDTO> albums = pictureMediaBiz.getAlbumList(albumType);
        return ResponseEntity.ok(ResponseDTO.success(albums));
    }

    @PostMapping("/albums")
    public ResponseEntity<ResponseDTO> createAlbum(@RequestBody AlbumDTO albumDto) {
        AlbumDTO album = pictureMediaBiz.createAlbum(albumDto.getAlbumName(), albumDto.getDescription(), albumDto.getCoverFileId());
        return ResponseEntity.ok(ResponseDTO.success(album));
    }

    @PostMapping("/albums/{albumId}/update")
    public ResponseEntity<ResponseDTO> updateAlbum(@PathVariable Long albumId, @RequestBody AlbumDTO albumDto) {
        pictureMediaBiz.updateAlbum(albumId, albumDto.getAlbumName(), albumDto.getDescription(), albumDto.getCoverFileId());
        return ResponseEntity.ok(ResponseDTO.success());
    }

    @PostMapping("/albums/{albumId}/delete")
    public ResponseEntity<ResponseDTO> deleteAlbum(@PathVariable Long albumId) {
        pictureMediaBiz.deleteAlbum(albumId);
        return ResponseEntity.ok(ResponseDTO.success());
    }

    @PostMapping("/albums/{albumId}/items")
    public ResponseEntity<ResponseDTO> addPictureToAlbum(@PathVariable Long albumId, @RequestParam Long fileId) {
        pictureMediaBiz.addPictureToAlbum(albumId, fileId);
        return ResponseEntity.ok(ResponseDTO.success());
    }

    @PostMapping("/albums/{albumId}/items/{fileId}")
    public ResponseEntity<ResponseDTO> removePictureFromAlbum(@PathVariable Long albumId, @PathVariable Long fileId) {
        pictureMediaBiz.removePictureFromAlbum(albumId, fileId);
        return ResponseEntity.ok(ResponseDTO.success());
    }

    @GetMapping("/albums/{albumId}/items")
    public ResponseEntity<ResponseDTO> getAlbumPictures(
            @PathVariable Long albumId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "40") Integer pageSize) {
        PageResponseDTO<PictureListDTO> result = pictureMediaBiz.getAlbumPictures(albumId, page, pageSize);
        return ResponseEntity.ok(ResponseDTO.success(result));
    }
}
