package com.hd.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.io.Serializable;
import java.util.Date;

/**
 * 文件标签实体类。
 * 用于管理和存储文件的分类标签信息。
 *
 * @author team-lead
 * @version 1.0
 * @createTime 2026/04/25
 */
@TableName("file_tag")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileTag implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 标签唯一标识符（主键）。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 标签名称（唯一）。 */
    private String tagName;

    /** 标签颜色（十六进制颜色码）。 */
    private String tagColor;

    /** 创建时间。 */
    private Date createdAt;

    /** 最后更新时间。 */
    private Date updatedAt;
}