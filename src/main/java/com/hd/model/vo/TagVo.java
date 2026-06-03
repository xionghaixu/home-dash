package com.hd.model.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.*;

/**
 * 标签VO。
 * 用于返回标签信息。
 *
 * @author xhx
 * @version 1.0
 * @createTime 2026/04/25
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TagVO {

    /** 标签ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 标签名称。 */
    private String tagName;

    /** 标签颜色。 */
    private String tagColor;
}