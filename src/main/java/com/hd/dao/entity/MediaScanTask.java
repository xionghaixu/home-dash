package com.hd.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 媒体扫描任务实体类
 * 记录媒体文件的扫描、元数据提取、缩略图生成等任务
 *
 * @author system
 * @since 2026-04-26
 */
@Data
@TableName("media_scan_task")
public class MediaScanTask implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long fileId;

    private String mediaType;

    private String taskType;

    private String status;

    private Integer retryCount;

    private Integer maxRetries;

    private String errorMessage;

    private Integer priority;

    private LocalDateTime createTime;

    private LocalDateTime startTime;

    private LocalDateTime finishTime;
}
