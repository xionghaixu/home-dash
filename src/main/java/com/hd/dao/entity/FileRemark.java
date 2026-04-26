package com.hd.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.io.Serializable;
import java.util.Date;

/**
 * 文件备注实体类。
 * 存储用户对文件的备注或短描述信息。
 *
 * @author team-lead
 * @version 1.0
 * @createTime 2026/04/25
 */
@TableName("file_remark")
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileRemark implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 记录唯一标识符（主键）。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 资源ID（唯一）。 */
    private Long resourceId;

    /** 备注内容。 */
    private String remarkContent;

    /** 创建时间。 */
    private Date createdAt;

    /** 最后更新时间。 */
    private Date updatedAt;
}