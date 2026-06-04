package com.hd.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建文件/文件夹请求DTO。
 * 用于Controller层接收创建文件的请求参数，避免DAO实体泄漏到Controller层。
 */
@Data
public class CreateFileDTO {
    @NotBlank(message = "文件名不能为空")
    @Size(max = 100, message = "文件名不能超过100个字符")
    private String fileName;

    @NotNull(message = "parentId不能为null")
    private Long parentId;

    @NotBlank(message = "type不能为空")
    private String type; // folder, picture, video, audio, document, etc.
}
