package com.hd.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.io.Serializable;
import java.util.Date;

/**
 * 最近使用记录实体类。
 * 记录用户最近访问或操作的文件，包括预览、播放、编辑等场景。
 *
 * @author team-lead
 * @version 1.0
 * @createTime 2026/04/25
 */
@TableName("recent_use")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecentUse implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 记录唯一标识符（主键）。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 资源ID。 */
    private Long resourceId;

    /** 使用类型（PREVIEW/PLAY/EDIT等）。 */
    private String useType;

    /** 使用时间。 */
    private Date usedAt;
}