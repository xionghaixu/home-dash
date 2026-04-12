package site.bitinit.pnd.web.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 文件分类摘要。
 * 用于阶段一基础分类浏览页面展示分类数量。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileCategorySummaryDto {

    private String category;
    private String label;
    private Integer count;
}
