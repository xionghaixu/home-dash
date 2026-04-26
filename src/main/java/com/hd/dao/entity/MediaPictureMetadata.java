package com.hd.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 图片元数据实体类
 * 存储图片EXIF信息，包括拍摄时间、设备、地理位置等
 *
 * @author system
 * @since 2026-04-26
 */
@Data
@TableName("media_picture_metadata")
public class MediaPictureMetadata implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long fileId;

    private Integer width;

    private Integer height;

    private LocalDateTime takenAt;

    private String cameraMake;

    private String cameraModel;

    private String lensModel;

    private String focalLength;

    private String aperture;

    private String exposureTime;

    private Integer iso;

    private BigDecimal gpsLatitude;

    private BigDecimal gpsLongitude;

    private String locationName;

    private Integer orientation;

    private String colorMode;

    private String scanStatus;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
