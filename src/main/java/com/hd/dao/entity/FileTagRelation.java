package com.hd.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.io.Serializable;
import java.util.Date;

/**
 * 文件-标签关联实体类。
 * 表示文件与标签的多对多关系。
 *
 * @author team-lead
 * @version 1.0
 * @createTime 2026/04/25
 */
@TableName("file_tag_relation")
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileTagRelation implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 记录唯一标识符（主键）。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 文件ID。 */
    private Long fileId;

    /** 标签ID。 */
    private Long tagId;

    /** 创建时间。 */
    private Date createdAt;
}