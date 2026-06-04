package com.hd.model.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecycleBinVO {
    /** 映射为fileId，匹配前端 row.id 以正常执行 restore */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    
    @JsonSerialize(using = ToStringSerializer.class)
    private Long fileId;
    
    private String name;
    
    private String type;
    
    private Long size;
    
    private String originalPath;
    
    private LocalDateTime deleteTime;
    
    private LocalDateTime deletedAt; // 匹配前端 prop="deletedAt"
    
    private LocalDateTime expireTime;
}

