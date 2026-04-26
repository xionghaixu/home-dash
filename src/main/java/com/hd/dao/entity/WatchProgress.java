package com.hd.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 视频观看进度实体类
 * 支持继续播放功能
 *
 * @author system
 * @since 2026-04-26
 */
@Data
@TableName("watch_progress")
public class WatchProgress implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long fileId;

    private Long currentPosition;

    private Long duration;

    private BigDecimal progressPercent;

    private Boolean finished;

    private LocalDateTime lastWatched;
}
