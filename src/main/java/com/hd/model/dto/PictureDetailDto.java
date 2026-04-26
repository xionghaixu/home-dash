package com.hd.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 图片详情DTO（含EXIF）
 *
 * @author system
 * @since 2026-04-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PictureDetailDto {

    private Long fileId;

    private String fileName;

    private Long size;

    private Integer width;

    private Integer height;

    private String format;

    private LocalDateTime takenAt;

    private String locationName;

    private String cameraMake;

    private String cameraModel;

    private String lensModel;

    private String focalLength;

    private String aperture;

    private String exposureTime;

    private Integer iso;

    private BigDecimal gpsLatitude;

    private BigDecimal gpsLongitude;

    private Integer orientation;

    private String colorMode;

    private Map<String, String> thumbnailUrls;

    private String originalUrl;

    private Boolean favorite;
}
