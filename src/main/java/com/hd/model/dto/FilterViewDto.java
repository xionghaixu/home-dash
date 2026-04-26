package com.hd.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * 筛选视图请求DTO。
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
public class FilterViewDto {

    /** 视图ID（更新时需要）。 */
    private Long id;

    /** 视图名称。 */
    @NotBlank(message = "视图名称不能为空")
    private String viewName;

    /** 视图参数（JSON格式）。 */
    @NotBlank(message = "视图参数不能为空")
    private String viewParams;

    /** 是否设为默认。 */
    private Boolean isDefault;
}
