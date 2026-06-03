package com.hd.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 重命名文件/文件夹请求DTO。
 * 用于Controller层接收重命名文件的请求参数，避免DAO实体泄漏到Controller层。
 */
@Data
public class RenameFileDTO {
    @NotBlank(message = "新文件名不能为空")
    @Size(max = 100, message = "文件名不能超过100个字符")
    private String fileName;
}
