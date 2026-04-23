package com.hd.model.dto;

import lombok.*;

/**
 * 系统信息数据传输对象。
 * 封装系统状态信息，包括存储容量、文件数量统计、容量预警等。
 *
 * <p><b>字段分类：</b>
 * <ul>
 *   <li>容量信息 - totalCap, usableCap, usedCap, usagePercent</li>
 *   <li>文件统计 - totalNum, folderNum, fileNum</li>
 *   <li>类型统计 - videoNum, audioNum, pictureNum, docNum, compressNum, otherNum</li>
 *   <li>容量预警 - warningLevel, warningMessage</li>
 *   <li>预留字段 - healthStatus, healthScore, lastCheckTime（阶段四使用）</li>
 * </ul>
 *
 * <p><b>容量预警级别：</b>
 * <ul>
 *   <li>normal - 正常（使用率 < 80%）</li>
 *   <li>warning - 警告（使用率 80%-90%）</li>
 *   <li>critical - 危险（使用率 > 90%）</li>
 * </ul>
 *
 * @author john
 * @date 2020-02-10
 */
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemInfoDto {

    // ========== 容量信息 ==========

    /**
     * 总存储容量（字节）。
     */
    private Long totalCap;

    /**
     * 可用存储容量（字节）。
     */
    private Long usableCap;

    /**
     * 已使用存储容量（字节）。
     */
    private Long usedCap;

    /**
     * 存储使用百分比（0-100）。
     */
    private Integer usagePercent;

    // ========== 文件统计 ==========

    /**
     * 文件和文件夹总数。
     */
    private Integer totalNum;

    /**
     * 文件夹数量。
     */
    private Integer folderNum;

    /**
     * 普通文件数量（不含文件夹）。
     */
    private Integer fileNum;

    // ========== 类型统计 ==========

    /**
     * 视频文件数量。
     */
    private Integer videoNum;

    /**
     * 音频文件数量。
     */
    private Integer audioNum;

    /**
     * 图片文件数量。
     */
    private Integer pictureNum;

    /**
     * 文档文件数量。
     */
    private Integer docNum;

    /**
     * 压缩文件数量。
     */
    private Integer compressNum;

    /**
     * 其他文件数量。
     */
    private Integer otherNum;

    /**
     * 最近上传文件数量。
     */
    private Integer recentUploadNum;

    // ========== 容量预警 ==========

    /**
     * 容量预警级别。
     * 取值：normal（正常）, warning（警告）, critical（危险）
     */
    private String warningLevel;

    /**
     * 容量预警消息。
     */
    private String warningMessage;

    // ========== 预留字段（阶段四使用）==========

    /**
     * 磁盘健康状态。
     * 取值：healthy（健康）, degraded（退化）, unhealthy（不健康）
     */
    private String healthStatus;

    /**
     * 健康评分（0-100）。
     */
    private Integer healthScore;

    /**
     * 最后检查时间。
     */
    private java.util.Date lastCheckTime;
}
