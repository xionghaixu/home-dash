package com.hd.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 视频元数据实体类
 * 存储视频时长、分辨率、编码信息等
 *
 * @author system
 * @since 2026-04-26
 */
@Data
@TableName("media_video_metadata")
public class MediaVideoMetadata implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long fileId;

    private Long duration;

    private Integer width;

    private Integer height;

    private String resolution;

    private Long bitrate;

    private BigDecimal frameRate;

    private String videoCodec;

    private String audioCodec;

    private Integer audioChannels;

    private Long audioBitrate;

    private Boolean hasAudio;

    private Boolean hasSubtitle;

    private Boolean coverGenerated;

    private String coverPath;

    private String scanStatus;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
