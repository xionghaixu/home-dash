package com.hd.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 重复文件组清理请求对象。
 * 用于替换无类型的 Map<String, Object> 接收方式。
 */
@Data
public class CleanupDuplicateDTO {

    @NotNull(message = "groupId 不能为空")
    private Long groupId;
}
