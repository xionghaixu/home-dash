package com.hd.model.vo;

import lombok.*;
import java.util.List;

/**
 * 文件详情VO。
 * 包含文件的完整详情及关联信息。
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
public class FileDetailVo {

    /** 资源ID。 */
    private Long resourceId;

    /** 文件ID。 */
    private Long fileId;

    /** 文件名称。 */
    private String fileName;

    /** 文件路径。 */
    private String filePath;

    /** 扩展名。 */
    private String extension;

    /** 文件大小（字节）。 */
    private Long size;

    /** 文件类型。 */
    private String type;

    /** 父目录ID。 */
    private Long parentId;

    /** 父目录路径。 */
    private String parentPath;

    /** 更新时间。 */
    private String updateTime;

    /** 标签列表。 */
    private List<TagVo> tags;

    /** 是否收藏。 */
    private Boolean isFavorite;

    /** 原始预览URL。 */
    private String previewUrl;

    /** 缩略图URL。 */
    private String thumbnailUrl;

    /** 预览状态信息。 */
    private PreviewStatusVo previewStatus;

    /** 同目录文件数量。 */
    private Integer sameDirCount;

    /** 同类型文件数量。 */
    private Integer sameTypeCount;

    /** 最近关联文件数量。 */
    private Integer recentRelatedCount;

    /** 文件备注。 */
    private String remark;

    /** 最近访问时间。 */
    private String lastAccessTime;
}