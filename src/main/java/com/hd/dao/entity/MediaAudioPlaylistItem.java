package com.hd.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 播放列表曲目关联实体类
 * 关联播放列表和音频文件
 *
 * @author system
 * @since 2026-04-26
 */
@Data
@TableName("media_audio_playlist_item")
public class MediaAudioPlaylistItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long playlistId;

    private Long fileId;

    private Integer position;

    private LocalDateTime addedAt;
}
