package com.hd.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 图片列表项DTO
 *
 * @author system
 * @since 2026-04-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PictureListDto {

    private Long fileId;

    private String fileName;

    private Long size;

    private Integer width;

    private Integer height;

    private LocalDateTime takenAt;

    private String locationName;

    private String cameraModel;

    private Map<String, String> thumbnailUrls;
}
