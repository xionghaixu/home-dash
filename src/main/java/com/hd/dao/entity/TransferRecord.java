package com.hd.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 传输记录持久化实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("transfer_record")
public class TransferRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String identifier;

    private String fileName;

    private String fileType;

    private String operationType;

    private String status;

    private String errorMessage;

    private Long totalSize;

    private Long parentId;

    private Long fileId;

    private Integer totalChunks;

    private Integer uploadedChunks;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
