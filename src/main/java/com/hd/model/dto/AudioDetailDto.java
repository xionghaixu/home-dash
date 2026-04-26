package com.hd.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 音频详情DTO
 *
 * @author system
 * @since 2026-04-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AudioDetailDto {

    private Long fileId;

    private String fileName;

    private String title;

    private String artist;

    private String album;

    private String albumArtist;

    private String genre;

    private Integer trackNumber;

    private Integer discNumber;

    private Integer year;

    private Long duration;

    private Long bitrate;

    private Integer sampleRate;

    private String coverUrl;

    private String audioUrl;

    private String format;

    private String lyrics;

    private List<Long> playlistIds;

    private Boolean favorite;
}
