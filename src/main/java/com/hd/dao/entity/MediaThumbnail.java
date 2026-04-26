package com.hd.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 缩略图记录实体类
 * 存储图片多尺寸缩略图和视频封面
 *
 * @author system
 * @since 2026-04-26
 */
@Data
@TableName("media_thumbnail")
public class MediaThumbnail implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long fileId;

    private String thumbnailType;

    private Integer width;

    private Integer height;

    private String thumbnailPath;

    private Long fileSize;

    private String generateStatus;

    private LocalDateTime generateTime;
}
