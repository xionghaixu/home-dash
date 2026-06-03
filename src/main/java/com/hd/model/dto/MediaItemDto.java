package com.hd.model.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 媒体项基础DTO
 * 用于图片、视频、音频列表展示
 *
 * @author xhx
 * @since 2026-04-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaItemDTO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long fileId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long resourceId;

    private String fileName;

    private Long size;

    private String type;

    private LocalDateTime createTime;

    private String thumbnailUrl;

    private String coverUrl;

    private Map<String, String> thumbnailUrls;
}
