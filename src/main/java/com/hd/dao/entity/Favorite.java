package com.hd.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.io.Serializable;
import java.util.Date;

/**
 * 收藏实体类。
 * 记录用户收藏的文件信息。
 *
 * @author team-lead
 * @version 1.0
 * @createTime 2026/04/25
 */
@TableName("favorite")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Favorite implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 收藏记录唯一标识符（主键）。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 资源ID（唯一）。 */
    private Long resourceId;

    /** 收藏时间。 */
    private Date createdAt;
}