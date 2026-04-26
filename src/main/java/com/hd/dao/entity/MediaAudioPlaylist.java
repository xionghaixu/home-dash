package com.hd.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 音频播放列表实体类
 * 存储用户创建的播放列表信息
 *
 * @author system
 * @since 2026-04-26
 */
@Data
@TableName("media_audio_playlist")
public class MediaAudioPlaylist implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String playlistName;

    private String description;

    private String coverPath;

    private String playMode;

    private Integer totalTracks;

    private Long totalDuration;

    private Boolean isDefault;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
