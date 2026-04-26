package com.hd.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 播放历史实体类
 * 记录视频和音频的播放历史
 *
 * @author system
 * @since 2026-04-26
 */
@Data
@TableName("play_history")
public class PlayHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long fileId;

    private String mediaType;

    private LocalDateTime playTime;

    private Long playDuration;
}
