package com.hd.model.dto;

import lombok.Data;

/**
 * 统一分页请求DTO
 *
 * @author system
 * @since 2026-04-26
 */
@Data
public class PageRequestDto {

    private Integer page = 1;

    private Integer pageSize = 20;

    private String sortBy;

    private String sortOrder = "desc";

    public int getOffset() {
        return (Math.max(page, 1) - 1) * Math.max(pageSize, 1);
    }

    public int getLimit() {
        return Math.max(pageSize, 1);
    }
}
