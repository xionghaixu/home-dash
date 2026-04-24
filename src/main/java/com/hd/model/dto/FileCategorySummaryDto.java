package com.hd.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author xhx
 * @version 1.0
 * @package com.hd.model.dto
 * @createTime 2026/04/23 23:34
 * @description 文件分类摘要。用于阶段一基础分类浏览页面展示分类数量。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileCategorySummaryDto {

    private String category;
    private String label;
    private Integer count;
}
