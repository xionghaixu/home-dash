package com.hd.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.io.Serializable;
import java.util.Date;

/**
 * 筛选视图实体类。
 * 存储用户保存的常用筛选条件配置。
 *
 * @author team-lead
 * @version 1.0
 * @createTime 2026/04/25
 */
@TableName("filter_view")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FilterView implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 视图唯一标识符（主键）。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 视图名称。 */
    private String viewName;

    /** 视图参数（JSON格式）。 */
    private String viewParams;

    /** 是否为默认视图。 */
    private Boolean isDefault;

    /** 创建时间。 */
    private Date createdAt;

    /** 最后更新时间。 */
    private Date updatedAt;
}