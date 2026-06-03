package com.hd.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 传输任务状态更新请求对象。
 * 用于替换无类型的 Map<String, Object> 接收方式。
 */
@Data
public class TransferStatusDTO {

    @NotBlank(message = "identifier 不能为空")
    private String identifier;

    @NotBlank(message = "fileName 不能为空")
    private String fileName;

    @NotBlank(message = "status 不能为空")
    private String status;

    private Long totalSize;

    private Long parentId;
}
