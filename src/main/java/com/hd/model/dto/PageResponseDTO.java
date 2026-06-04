package com.hd.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 统一分页响应DTO
 *
 * @author xhx
 * @since 2026-04-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponseDTO<T> {

    private List<T> list;

    private Long total;

    private Integer page;

    private Integer pageSize;

    private Boolean hasMore;

    public static <T> PageResponseDTO<T> of(List<T> list, Long total, Integer page, Integer pageSize) {
        return PageResponseDTO.<T>builder()
                .list(list)
                .total(total)
                .page(page)
                .pageSize(pageSize)
                .hasMore((long) page * pageSize < total)
                .build();
    }
}
