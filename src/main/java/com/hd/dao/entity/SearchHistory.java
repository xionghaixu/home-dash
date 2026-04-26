package com.hd.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.io.Serializable;
import java.util.Date;

/**
 * 搜索历史实体类。
 * 记录用户的搜索关键词和搜索参数，用于搜索历史展示和热门搜索统计。
 *
 * @author team-lead
 * @version 1.0
 * @createTime 2026/04/25
 */
@TableName("search_history")
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 搜索记录唯一标识符（主键）。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 搜索关键词。 */
    private String keyword;

    /** 搜索类型（FILE/TAG等）。 */
    private String searchType;

    /** 搜索参数（JSON格式）。 */
    private String searchParams;

    /** 搜索时间。 */
    private Date searchedAt;

    /** 用户ID（多用户场景隔离）。 */
    private Long userId;
}