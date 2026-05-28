package com.hd.model.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.*;

/**
 * 筛选视图VO。
 * 用于返回用户保存的筛选视图配置。
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
public class FilterViewVo {

    /** 视图ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 视图名称。 */
    private String viewName;

    /** 视图参数（JSON格式）。 */
    private String viewParams;

    /** 是否默认视图。 */
    private Boolean isDefault;
}