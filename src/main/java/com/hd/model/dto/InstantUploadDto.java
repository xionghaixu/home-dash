package com.hd.model.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author xhx
 * @version 1.0
 * @package com.hd.model.dto
 * @createTime 2026/04/23 23:34
 * @description 秒传请求数据传输对象。
 */
@Data
public class InstantUploadDTO {

    @NotBlank(message = "文件MD5不能为空")
    private String md5;

    @NotBlank(message = "文件名不能为空")
    private String fileName;

    @NotNull(message = "父文件夹ID不能为空")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;
}
