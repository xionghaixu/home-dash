package com.hd.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 相册实体类
 * 存储自动或手动创建的相册信息
 *
 * @author system
 * @since 2026-04-26
 */
@Data
@TableName("media_album")
public class MediaAlbum implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String albumName;

    private String albumType;

    private Long coverFileId;

    private String description;

    private String ruleExpression;

    private Integer photoCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
