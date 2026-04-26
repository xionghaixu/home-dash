package com.hd.model.dto;

import lombok.Data;

/**
 * 更新播放进度DTO
 *
 * @author system
 * @since 2026-04-26
 */
@Data
public class UpdateProgressDto {

    private Long currentPosition;

    private Long duration;
}
