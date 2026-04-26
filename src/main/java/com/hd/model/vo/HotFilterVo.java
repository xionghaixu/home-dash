package com.hd.model.vo;

import lombok.*;

/**
 * 热门筛选VO。
 * 用于返回热门筛选选项统计。
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
public class HotFilterVo {

    /** 筛选类型（如：type、tag、timeRange）。 */
    private String filterType;

    /** 筛选值。 */
    private String filterValue;

    /** 命中数量。 */
    private Long count;

    /** 显示名称。 */
    private String displayName;
}