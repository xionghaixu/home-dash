package com.hd.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 相册图片关联实体类
 * 关联相册和图片文件
 *
 * @author system
 * @since 2026-04-26
 */
@Data
@TableName("media_album_item")
public class MediaAlbumItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long albumId;

    private Long fileId;

    private LocalDateTime takenAt;

    private Integer sortOrder;

    private LocalDateTime addedAt;
}
