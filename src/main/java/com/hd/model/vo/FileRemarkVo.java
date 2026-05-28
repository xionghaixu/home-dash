package com.hd.model.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.*;

/**
 * 文件备注VO。
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
public class FileRemarkVo {

    /** 备注ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 资源ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long resourceId;

    /** 备注内容。 */
    private String remarkContent;

    /** 创建时间。 */
    private String createdAt;

    /** 更新时间。 */
    private String updatedAt;
}
