package com.hd.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.List;

/**
 * @author xhx
 * @version 1.0
 * @package com.hd.model.dto
 * @createTime 2026/04/23 23:34
 * @description 文件详情数据传输对象。为阶段一详情抽屉、视频页和下载入口提供稳定字段。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileDetailDto {

    private Long id;
    private Long parentId;
    private Long resourceId;
    private Long size;
    private String fileName;
    private String type;
    private String extension;
    private String folderPath;
    private Boolean downloadable;
    private Boolean playable;
    private Date createTime;
    private Date updateTime;
    private List<FolderPathDto> navigation;
}
