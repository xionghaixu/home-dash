package com.hd.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 视频系列实体类
 * 存储电视剧、综艺等视频系列信息
 *
 * @author system
 * @since 2026-04-26
 */
@Data
@TableName("media_video_series")
public class MediaVideoSeries implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String seriesName;

    private String description;

    private String posterPath;

    private Integer totalEpisodes;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
