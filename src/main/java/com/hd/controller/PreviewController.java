package com.hd.controller;

import com.hd.biz.PreviewBiz;
import com.hd.common.HomeDashConstants;
import com.hd.common.exception.DataNotFoundException;
import com.hd.model.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 预览控制器。
 * 提供图片、文本、音频等文件的预览接口。
 *
 * @author team-lead
 * @version 1.0
 * @createTime 2026/04/25
 */
@Slf4j
@RestController
@RequestMapping(HomeDashConstants.API_VERSION)
@RequiredArgsConstructor
public class PreviewController {

    private final PreviewBiz previewBiz;

    /**
     * 获取图片缩略图。
     *
     * @param resourceId 资源ID
     * @return 缩略图数据
     */
    @GetMapping("/preview/image/{resourceId}/thumbnail")
    public ResponseEntity<byte[]> getImageThumbnail(@PathVariable Long resourceId) {
        log.info("获取图片缩略图请求 [resourceId={}]", resourceId);
        byte[] thumbnailData = previewBiz.getImageThumbnail(resourceId);
        if (thumbnailData == null) {
            throw new DataNotFoundException("图片缩略图不存在 [resourceId=" + resourceId + "]");
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(thumbnailData);
    }

    /**
     * 获取原始图片。
     *
     * @param resourceId 资源ID
     * @return 原始图片数据
     */
    @GetMapping("/preview/image/{resourceId}/original")
    public ResponseEntity<byte[]> getOriginalImage(@PathVariable Long resourceId) {
        log.info("获取原始图片请求 [resourceId={}]", resourceId);
        byte[] imageData = previewBiz.getOriginalImage(resourceId);
        if (imageData == null) {
            throw new DataNotFoundException("原始图片不存在 [resourceId=" + resourceId + "]");
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(imageData);
    }

    /**
     * 获取图片EXIF信息。
     *
     * @param resourceId 资源ID
     * @return EXIF信息
     */
    @GetMapping("/preview/image/{resourceId}/exif")
    public ResponseEntity<ResponseDto> getImageExif(@PathVariable Long resourceId) {
        log.info("获取图片EXIF信息请求 [resourceId={}]", resourceId);
        Map<String, Object> exifData = previewBiz.getImageExif(resourceId);
        return ResponseEntity.ok(ResponseDto.success(exifData));
    }

    /**
     * 获取图片信息（尺寸、拍摄时间等）。
     *
     * @param resourceId 资源ID
     * @return 图片信息
     */
    @GetMapping("/preview/image/{resourceId}/info")
    public ResponseEntity<ResponseDto> getImageInfo(@PathVariable Long resourceId) {
        log.info("获取图片信息请求 [resourceId={}]", resourceId);
        Map<String, Object> imageInfo = previewBiz.getImageInfo(resourceId);
        return ResponseEntity.ok(ResponseDto.success(imageInfo));
    }

    /**
     * 获取文本内容预览。
     *
     * @param resourceId 资源ID
     * @param offset 起始位置
     * @param limit 读取长度
     * @return 文本内容
     */
    @GetMapping("/preview/text/{resourceId}")
    public ResponseEntity<ResponseDto> getTextPreview(
            @PathVariable Long resourceId,
            @RequestParam(defaultValue = "0") Long offset,
            @RequestParam(defaultValue = "65536") Long limit) {
        log.info("获取文本预览请求 [resourceId={}, offset={}, limit={}]", resourceId, offset, limit);
        Map<String, Object> textPreview = previewBiz.getTextPreview(resourceId, offset, limit);
        return ResponseEntity.ok(ResponseDto.success(textPreview));
    }

    /**
     * 获取文本预览的摘要。
     *
     * @param resourceId 资源ID
     * @return 文本摘要
     */
    @GetMapping("/preview/text/{resourceId}/summary")
    public ResponseEntity<ResponseDto> getTextSummary(@PathVariable Long resourceId) {
        log.info("获取文本摘要请求 [resourceId={}]", resourceId);
        Map<String, Object> summary = previewBiz.getTextSummary(resourceId);
        return ResponseEntity.ok(ResponseDto.success(summary));
    }

    /**
     * 获取音频元数据。
     *
     * @param resourceId 资源ID
     * @return 音频元数据
     */
    @GetMapping("/preview/audio/{resourceId}/metadata")
    public ResponseEntity<ResponseDto> getAudioMetadata(@PathVariable Long resourceId) {
        log.info("获取音频元数据请求 [resourceId={}]", resourceId);
        Map<String, Object> metadata = previewBiz.getAudioMetadata(resourceId);
        return ResponseEntity.ok(ResponseDto.success(metadata));
    }

    /**
     * 获取音频流用于播放。
     *
     * @param resourceId 资源ID
     * @return 音频流数据
     */
    @GetMapping("/preview/audio/{resourceId}/stream")
    public ResponseEntity<byte[]> getAudioStream(@PathVariable Long resourceId) {
        log.info("获取音频流请求 [resourceId={}]", resourceId);
        byte[] audioData = previewBiz.getAudioStream(resourceId);
        if (audioData == null) {
            throw new DataNotFoundException("音频流不存在 [resourceId=" + resourceId + "]");
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .body(audioData);
    }

    /**
     * 获取预览降级信息（当预览失败时）。
     *
     * @param resourceId 资源ID
     * @param previewType 预览类型
     * @return 降级信息
     */
    @GetMapping("/preview/{resourceId}/fallback")
    public ResponseEntity<ResponseDto> getPreviewFallback(
            @PathVariable Long resourceId,
            @RequestParam(defaultValue = "unknown") String previewType) {
        log.info("获取预览降级信息 [resourceId={}, previewType={}]", resourceId, previewType);
        Map<String, Object> fallback = previewBiz.getPreviewFallback(resourceId, previewType);
        return ResponseEntity.ok(ResponseDto.success(fallback));
    }

    /**
     * 记录最近播放。
     *
     * @param resourceId 资源ID
     * @return 操作结果
     */
    @PostMapping("/preview/audio/{resourceId}/play")
    public ResponseEntity<ResponseDto> recordAudioPlay(@PathVariable Long resourceId) {
        log.info("记录音频播放 [resourceId={}]", resourceId);
        previewBiz.recordRecentUse(resourceId, "PLAY");
        return ResponseEntity.ok(ResponseDto.success());
    }

    /**
     * 记录最近预览。
     *
     * @param resourceId 资源ID
     * @return 操作结果
     */
    @PostMapping("/preview/{resourceId}/view")
    public ResponseEntity<ResponseDto> recordPreview(
            @PathVariable Long resourceId,
            @RequestParam(defaultValue = "PREVIEW") String useType) {
        log.info("记录预览 [resourceId={}, useType={}]", resourceId, useType);
        previewBiz.recordRecentUse(resourceId, useType);
        return ResponseEntity.ok(ResponseDto.success());
    }
}
