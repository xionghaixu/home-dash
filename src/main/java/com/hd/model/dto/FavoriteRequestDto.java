package com.hd.model.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import lombok.*;

/**
 * 收藏请求DTO。
 * 用于添加或移除收藏。
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
public class FavoriteRequestDTO {

    /** 资源ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long resourceId;
}