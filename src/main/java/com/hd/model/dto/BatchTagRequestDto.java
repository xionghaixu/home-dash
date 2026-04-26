package com.hd.model.dto;

import lombok.*;

/**
 * 批量标签请求DTO。
 * 用于批量添加、移除或替换文件标签。
 *
 * @author team-lead
 * @version 1.0
 * @createTime 2026/04/25
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchTagRequestDto {

    /** 资源ID列表。 */
    private java.util.List<Long> resourceIds;

    /** 标签ID列表。 */
    private java.util.List<Long> tagIds;

    /** 操作类型：ADD-添加，REMOVE-移除，REPLACE-替换。 */
    private String action;

    /** 操作类型常量。 */
    public static final String ACTION_ADD = "ADD";
    public static final String ACTION_REMOVE = "REMOVE";
    public static final String ACTION_REPLACE = "REPLACE";
}