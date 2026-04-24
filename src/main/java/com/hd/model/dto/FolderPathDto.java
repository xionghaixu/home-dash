package com.hd.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * @author xhx
 * @version 1.0
 * @package com.hd.model.dto
 * @createTime 2026/04/23 23:34
 * @description 文件夹路径数据传输对象。封装文件夹路径信息，用于显示面包屑导航。
 */
@Setter
@Getter
@AllArgsConstructor
public class FolderPathDto {

    private Long id;
    private String fileName;
}
