package com.hd.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * 文件夹路径数据传输对象。
 * 封装文件夹路径信息，用于显示面包屑导航。
 *
 * @author john
 * @date 2020-01-11
 */
@Setter
@Getter
@AllArgsConstructor
public class FolderPathDto {

    private Long id;
    private String fileName;
}
