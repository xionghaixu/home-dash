package com.hd.model.vo;

import lombok.*;

/**
 * 标签VO。
 * 用于返回标签信息。
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
public class TagVo {

    /** 标签ID。 */
    private Long id;

    /** 标签名称。 */
    private String tagName;

    /** 标签颜色。 */
    private String tagColor;
}