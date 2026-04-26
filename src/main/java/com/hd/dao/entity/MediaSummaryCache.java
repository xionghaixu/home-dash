package com.hd.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 首页聚合缓存实体类
 * 缓存首页媒体工作台所需聚合数据
 *
 * @author system
 * @since 2026-04-26
 */
@Data
@TableName("media_summary_cache")
public class MediaSummaryCache implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String cacheKey;

    private String cacheValue;

    private LocalDateTime expireTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
