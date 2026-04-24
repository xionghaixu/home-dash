package com.hd.model.dto;

import lombok.Data;

/**
 * @author xhx
 * @version 1.0
 * @package com.hd.model.dto
 * @createTime 2026/04/23 23:34
 * @description 秒传请求数据传输对象。
 */
@Data
public class InstantUploadDto {

    private String md5;
    private String fileName;
    private Long parentId;
}
