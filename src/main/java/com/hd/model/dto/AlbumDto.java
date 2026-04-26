package com.hd.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 相册DTO
 *
 * @author system
 * @since 2026-04-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlbumDto {

    private Long albumId;

    private String albumName;

    private String albumType;

    private Long coverFileId;

    private String coverUrl;

    private String description;

    private Integer photoCount;

    private LocalDateTime createTime;
}
