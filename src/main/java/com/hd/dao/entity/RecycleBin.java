package com.hd.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.*;
import org.apache.ibatis.type.Alias;

import java.time.LocalDateTime;

/**
 * 软删除回收站记录
 */
@Alias("recycleBin")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecycleBin {

    @TableId(type = IdType.AUTO)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long fileId;
    
    /** 文件删除时的父级目录 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long originalParentId;

    /** 文件删除时的完整路径记录（用于展示） */
    private String originalPath;

    @Builder.Default
    private LocalDateTime deleteTime = LocalDateTime.now();

    /** 预计自动过期被彻底清理的时间 */
    private LocalDateTime expireTime;
}

