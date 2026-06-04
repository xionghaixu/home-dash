package com.hd.model.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author xhx
 * @version 1.0
 * @package com.hd.model.dto
 * @createTime 2026/04/23 23:34
 * @description 文件详情数据传输对象。为阶段一详情抽屉、视频页和下载入口提供稳定字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileDetailDTO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private Long parentId;
    private Long resourceId;
    private Long size;
    private String fileName;
    private String type;
    private String extension;
    private String folderPath;
    private Boolean downloadable;
    private Boolean playable;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<FolderPathDTO> navigation;
}

