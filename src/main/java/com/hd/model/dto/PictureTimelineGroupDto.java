package com.hd.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 时间线分组DTO
 *
 * @author system
 * @since 2026-04-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PictureTimelineGroupDto {

    private Integer year;

    private Integer month;

    private Integer day;

    private Long count;

    private Long coverFileId;

    private String coverThumbnail;
}
