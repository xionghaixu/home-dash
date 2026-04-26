package com.hd.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 观看进度DTO
 *
 * @author system
 * @since 2026-04-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchProgressDto {

    private Long currentPosition;

    private BigDecimal progressPercent;

    private Boolean finished;
}
