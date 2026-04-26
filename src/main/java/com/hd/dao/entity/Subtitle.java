package com.hd.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 字幕关联实体类
 * 存储视频的外挂字幕信息
 *
 * @author system
 * @since 2026-04-26
 */
@Data
@TableName("subtitle")
public class Subtitle implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long fileId;

    private String subtitlePath;

    private String language;

    private String subtitleType;

    private String format;

    private Boolean isDefault;

    private LocalDateTime createTime;
}
