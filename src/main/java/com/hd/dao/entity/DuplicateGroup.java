package com.hd.dao.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("duplicate_group")
public class DuplicateGroup {
    @TableId(type = IdType.AUTO)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    
    private String md5;
    
    private Long size;
    
    private Integer fileCount;
    
    @TableField(exist = false)
    private List<DuplicateRecord> files;
    
    @Builder.Default
    private LocalDateTime createTime = LocalDateTime.now();
    
    @Builder.Default
    private LocalDateTime updateTime = LocalDateTime.now();
}

