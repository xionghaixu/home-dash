package com.hd.model.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 播放列表曲目DTO
 *
 * @author xhx
 * @since 2026-04-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaylistItemDTO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long fileId;

    private String fileName;

    private String title;

    private String artist;

    private String album;

    private Long duration;

    private Integer position;

    private String coverUrl;
}
