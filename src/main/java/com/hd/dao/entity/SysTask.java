package com.hd.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.*;
import org.apache.ibatis.type.Alias;

import java.util.Date;

/**
 * 全局后台任务
 */
@Alias("sysTask")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SysTask {

    @TableId(type = IdType.AUTO)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 任务类型: SCAN_DUPLICATE, STORAGE_ANALYSIS, INTEGRITY_CHECK 等 */
    private String taskType;

    /** 状态: PENDING, RUNNING, SUCCESS, FAILED */
    private String status;

    /** 进度百分比 0-100 */
    @Builder.Default
    private Integer progressPercent = 0;

    /** 错误信息摘要 */
    private String errorMsg;

    @Builder.Default
    private Date createTime = new Date();

    private Date endTime;
}
