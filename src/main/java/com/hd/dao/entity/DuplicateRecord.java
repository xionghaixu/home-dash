package com.hd.dao.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.*;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("duplicate_record")
public class DuplicateRecord {
    @TableId(type = IdType.AUTO)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    
    @JsonSerialize(using = ToStringSerializer.class)
    private Long groupId;
    
    @JsonSerialize(using = ToStringSerializer.class)
    private Long fileId;
    
    private String fileName;
    
    private String path;
    
    @Builder.Default
    private Date createTime = new Date();
}
