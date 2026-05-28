package com.hd.model.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 播放列表DTO
 *
 * @author system
 * @since 2026-04-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AudioPlaylistDto {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long playlistId;

    private String playlistName;

    private String description;

    private String coverUrl;

    private String playMode;

    private Integer totalTracks;

    private Long totalDuration;

    private Boolean isDefault;

    private LocalDateTime createTime;
}
