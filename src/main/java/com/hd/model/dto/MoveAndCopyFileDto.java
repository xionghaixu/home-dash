package com.hd.model.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * @author xhx
 * @version 1.0
 * @package com.hd.model.dto
 * @createTime 2026/04/23 23:34
 * @description 文件移动和复制数据传输对象。封装文件移动或复制操作的请求参数，包括文件ID列表、目标文件夹ID列表和操作类型。
 *
 * <p><b>使用场景：</b>
 * <ul>
 *   <li>移动文件：{@code type="move"}，targetIds 必须只有一个值</li>
 *   <li>复制文件：{@code type="copy"}，targetIds 可以有多个值（同时复制到多个目标）</li>
 * </ul>
 *
 * <p><b>业务规则：</b>
 * <ul>
 *   <li>移动操作只能指定一个目标文件夹</li>
 *   <li>复制操作可以指定多个目标文件夹</li>
 *   <li>不能将文件夹移动到自身或其子文件夹中</li>
 *   <li>目标目录存在同名文件时返回错误</li>
 * </ul>
 *
 * <p><b>请求示例：</b>
 * <pre>
 * // 移动文件到目标文件夹
 * {
 *   "fileIds": [1, 2, 3],
 *   "targetIds": [10],
 *   "type": "move"
 * }
 *
 * // 复制文件到多个目标文件夹
 * {
 *   "fileIds": [1, 2],
 *   "targetIds": [10, 20, 30],
 *   "type": "copy"
 * }
 * </pre>
 */
@Data
public class MoveAndCopyFileDto {

    /** 移动操作类型 */
    public static final String MOVE_TYPE = "move";

    /** 复制操作类型 */
    public static final String COPY_TYPE = "copy";

    /**
     * 要操作的文件ID列表。
     * 不能为空，必须包含至少一个文件ID。
     */
    @NotEmpty(message = "文件ID列表不能为空")
    private List<@NotNull(message = "文件ID不能为空") Long> fileIds;

    /**
     * 目标文件夹ID列表。
     * 移动操作时只能有一个值，复制操作时可以有多个值。
     */
    @NotEmpty(message = "目标文件夹ID列表不能为空")
    private List<@NotNull(message = "目标文件夹ID不能为空") Long> targetIds;

    /**
     * 操作类型。
     * 取值范围：{@code "move"}（移动）或 {@code "copy"}（复制）。
     */
    @NotNull(message = "操作类型不能为空")
    private String type;
}
