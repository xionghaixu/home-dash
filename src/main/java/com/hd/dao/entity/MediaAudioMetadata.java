package com.hd.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 音频元数据实体类
 * 存储音频标题、专辑、歌手、时长等信息
 *
 * @author system
 * @since 2026-04-26
 */
@Data
@TableName("media_audio_metadata")
public class MediaAudioMetadata implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long fileId;

    private String title;

    private String album;

    private String artist;

    private String albumArtist;

    private String genre;

    private Integer trackNumber;

    private Integer discNumber;

    private Integer year;

    private Long duration;

    private Long bitrate;

    private Integer sampleRate;

    private String coverPath;

    private String lyrics;

    private String scanStatus;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
