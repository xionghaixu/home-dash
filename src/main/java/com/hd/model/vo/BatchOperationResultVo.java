package com.hd.model.vo;

import lombok.*;
import java.util.List;

/**
 * 批量操作结果VO。
 * 返回批量操作的成功/失败统计。
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
public class BatchOperationResultVo {

    /** 成功数量。 */
    private Integer successCount;

    /** 失败数量。 */
    private Integer failCount;

    /** 错误信息列表。 */
    private List<String> errors;
}