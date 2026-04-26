package com.hd.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 播放列表曲目DTO
 *
 * @author system
 * @since 2026-04-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaylistItemDto {

    private Long fileId;

    private String fileName;

    private String title;

    private String artist;

    private String album;

    private Long duration;

    private Integer position;

    private String coverUrl;
}
