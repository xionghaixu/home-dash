package com.hd.model.dto;

import lombok.*;
import java.util.List;

/**
 * 搜索请求DTO。
 * 用于文件搜索和筛选的请求参数。
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
public class SearchRequestDto {

    /** 搜索关键词。 */
    private String keyword;

    /** 文件类型筛选列表。 */
    private List<String> fileTypes;

    /** 开始时间。 */
    private String startDate;

    /** 结束时间。 */
    private String endDate;

    /** 最小文件大小（字节）。 */
    private Long minSize;

    /** 最大文件大小（字节）。 */
    private Long maxSize;

    /** 目录路径。 */
    private String directoryPath;

    /** 是否包含子目录。 */
    private Boolean includeSubdirectories;

    /** 标签ID列表。 */
    private List<Long> tags;

    /** 是否仅显示收藏。 */
    private Boolean favoritesOnly;

    /** 页码（从1开始）。 */
    @Builder.Default
    private Integer page = 1;

    /** 每页大小。 */
    @Builder.Default
    private Integer pageSize = 20;

    /** 排序字段。 */
    @Builder.Default
    private String sortBy = "updateTime";

    /** 排序方向（ASC/DESC）。 */
    @Builder.Default
    private String sortOrder = "DESC";
}