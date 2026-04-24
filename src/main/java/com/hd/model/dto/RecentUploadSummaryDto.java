package com.hd.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author xhx
 * @version 1.0
 * @package com.hd.model.dto
 * @createTime 2026/04/23 23:34
 * @description 最近上传摘要数据传输对象。封装最近上传文件的统计摘要信息，用于工作台和传输列表展示。
 *
 * <p><b>包含内容：</b>
 * <ul>
 *   <li>最近上传文件列表</li>
 *   <li>今日上传数量</li>
 *   <li>本周上传数量</li>
 *   <li>本月上传数量</li>
 *   <li>总上传数量</li>
 *   <li>今日上传总大小</li>
 * </ul>
 *
 * <p><b>使用场景：</b>
 * <ul>
 *   <li>首页工作台统计卡片</li>
 *   <li>最近上传页面数据展示</li>
 *   <li>传输列表摘要信息</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentUploadSummaryDto {

    /**
     * 最近上传文件列表。
     */
    private List<File> recentFiles;

    /**
     * 今日上传数量。
     */
    private Integer todayCount;

    /**
     * 本周上传数量。
     */
    private Integer weekCount;

    /**
     * 本月上传数量。
     */
    private Integer monthCount;

    /**
     * 总上传数量（最近上传范围内的）。
     */
    private Integer totalCount;

    /**
     * 今日上传总大小（字节）。
     */
    private Long todaySize;

    /**
     * 本周上传总大小（字节）。
     */
    private Long weekSize;

    /**
     * 本月上传总大小（字节）。
     */
    private Long monthSize;

    /**
     * 查询参数摘要信息。
     */
    private Map<String, Object> querySummary;

    /**
     * 文件基础信息。
     * 用于列表展示。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class File {

        /**
         * 文件ID。
         */
        private Long id;

        /**
         * 文件名。
         */
        private String fileName;

        /**
         * 文件类型。
         */
        private String type;

        /**
         * 文件大小（字节）。
         */
        private Long size;

        /**
         * 创建时间。
         */
        private Date createTime;

        /**
         * 父目录ID。
         */
        private Long parentId;
    }
}