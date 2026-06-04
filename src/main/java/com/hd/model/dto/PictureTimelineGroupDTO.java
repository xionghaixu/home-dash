package com.hd.model.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 时间线分组DTO
 *
 * @author xhx
 * @since 2026-04-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PictureTimelineGroupDTO {

    private Integer year;

    private Integer month;

    private Integer day;

    private Long count;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long coverFileId;

    private String coverThumbnail;
}
