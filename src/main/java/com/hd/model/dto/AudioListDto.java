package com.hd.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 音频列表项DTO
 *
 * @author system
 * @since 2026-04-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AudioListDto {

    private Long fileId;

    private String fileName;

    private String title;

    private String album;

    private String artist;

    private Long duration;

    private Long bitrate;

    private Integer trackNumber;

    private Integer year;

    private String genre;

    private String coverUrl;
}
