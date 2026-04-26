package com.hd.model.vo;

import lombok.*;

/**
 * 搜索历史VO。
 * 用于返回用户的搜索历史记录。
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
public class SearchHistoryVo {

    /** 记录ID。 */
    private Long id;

    /** 搜索关键词。 */
    private String keyword;

    /** 搜索类型。 */
    private String searchType;

    /** 搜索时间。 */
    private String searchedAt;
}