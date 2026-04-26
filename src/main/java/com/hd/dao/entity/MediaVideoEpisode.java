package com.hd.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 视频集数关联实体类
 * 关联视频系列和具体的视频文件
 *
 * @author system
 * @since 2026-04-26
 */
@Data
@TableName("media_video_episode")
public class MediaVideoEpisode implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long seriesId;

    private Long fileId;

    private Integer episodeNumber;

    private Integer seasonNumber;

    private String episodeTitle;
}
