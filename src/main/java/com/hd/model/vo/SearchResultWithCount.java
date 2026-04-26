package com.hd.model.vo;

import lombok.*;
import java.util.List;

/**
 * 搜索结果（带总数）VO。
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
public class SearchResultWithCount {

    /** 搜索结果列表。 */
    private List<FileDetailVo> files;

    /** 总数。 */
    private Long total;

    /** 当前页码。 */
    private Integer page;

    /** 每页大小。 */
    private Integer pageSize;

    /** 总页数。 */
    private Integer totalPages;
}
