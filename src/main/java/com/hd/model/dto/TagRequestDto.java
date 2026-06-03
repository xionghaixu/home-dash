package com.hd.model.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * 标签请求DTO。
 * 用于创建或更新标签。
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
public class TagRequestDTO {

    /** 标签ID（更新时需要）。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 标签名称。 */
    @NotBlank(message = "标签名称不能为空")
    private String tagName;

    /** 标签颜色（十六进制）。 */
    private String tagColor;
}