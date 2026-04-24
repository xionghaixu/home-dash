package com.hd.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * @author xhx
 * @version 1.0
 * @package com.hd.model.dto
 * @createTime 2026/04/23 23:34
 * @description 批量操作结果数据传输对象。统一规范批量文件操作（移动、复制、删除等）的响应格式。
 *
 * <p><b>设计原则：</b>
 * <ul>
 *   <li>返回操作总体成功/失败状态</li>
 *   <li>提供成功和失败的数量统计</li>
 *   <li>记录每个操作项的详细结果，便于前端精确处理</li>
 *   <li>失败时提供明确的错误信息</li>
 * </ul>
 *
 * <p><b>响应示例：</b>
 * <pre>
 * // 全部成功
 * {
 *   "success": true,
 *   "totalCount": 3,
 *   "successCount": 3,
 *   "failCount": 0,
 *   "results": [
 *     {"fileId": 1, "fileName": "a.txt", "status": "success"},
 *     {"fileId": 2, "fileName": "b.txt", "status": "success"},
 *     {"fileId": 3, "fileName": "c.txt", "status": "success"}
 *   ]
 * }
 *
 * // 部分成功
 * {
 *   "success": false,
 *   "totalCount": 3,
 *   "successCount": 1,
 *   "failCount": 2,
 *   "results": [
 *     {"fileId": 1, "fileName": "a.txt", "status": "success"},
 *     {"fileId": 2, "fileName": "b.txt", "status": "failed", "error": "目标目录已存在同名文件"},
 *     {"fileId": 3, "fileName": "c.txt", "status": "failed", "error": "文件不存在"}
 *   ]
 * }
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchOperationResultDto {

    /**
     * 操作是否全部成功。
     * 当 failCount 为 0 时为 true，否则为 false。
     */
    private boolean success;

    /**
     * 操作项总数。
     */
    private int totalCount;

    /**
     * 成功数量。
     */
    private int successCount;

    /**
     * 失败数量。
     */
    private int failCount;

    /**
     * 每个操作项的详细结果。
     * 列表顺序与请求中的 fileIds 顺序一致。
     */
    private List<ItemResult> results;

    /**
     * 额外的摘要信息。
     * 可用于存储操作统计、警告信息等。
     */
    private Map<String, Object> extra;

    /**
     * 批量操作中单个操作项的结果。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemResult {

        /**
         * 文件ID。
         */
        private Long fileId;

        /**
         * 文件名（便于前端展示）。
         */
        private String fileName;

        /**
         * 操作状态。
         * 取值：success | failed | skipped
         */
        private String status;

        /**
         * 错误信息（仅失败时填充）。
         */
        private String error;

        /**
         * 错误码（仅失败时填充）。
         */
        private Integer errorCode;

        /**
         * 新文件ID（仅复制成功时填充，用于复制到新位置）。
         */
        private Long newFileId;

        /**
         * 创建成功结果。
         */
        public static ItemResult success(Long fileId, String fileName) {
            return ItemResult.builder()
                    .fileId(fileId)
                    .fileName(fileName)
                    .status("success")
                    .build();
        }

        /**
         * 创建成功结果（复制操作）。
         */
        public static ItemResult success(Long fileId, String fileName, Long newFileId) {
            return ItemResult.builder()
                    .fileId(fileId)
                    .fileName(fileName)
                    .status("success")
                    .newFileId(newFileId)
                    .build();
        }

        /**
         * 创建失败结果。
         */
        public static ItemResult failed(Long fileId, String fileName, String error) {
            return ItemResult.builder()
                    .fileId(fileId)
                    .fileName(fileName)
                    .status("failed")
                    .error(error)
                    .build();
        }

        /**
         * 创建失败结果（带错误码）。
         */
        public static ItemResult failed(Long fileId, String fileName, String error, Integer errorCode) {
            return ItemResult.builder()
                    .fileId(fileId)
                    .fileName(fileName)
                    .status("failed")
                    .error(error)
                    .errorCode(errorCode)
                    .build();
        }

        /**
         * 创建跳过结果（如目标不支持操作）。
         */
        public static ItemResult skipped(Long fileId, String fileName, String reason) {
            return ItemResult.builder()
                    .fileId(fileId)
                    .fileName(fileName)
                    .status("skipped")
                    .error(reason)
                    .build();
        }
    }

    /**
     * 创建全部成功的结果。
     */
    public static BatchOperationResultDto allSuccess(List<ItemResult> results) {
        return BatchOperationResultDto.builder()
                .success(true)
                .totalCount(results.size())
                .successCount(results.size())
                .failCount(0)
                .results(results)
                .build();
    }

    /**
     * 根据结果列表创建结果。
     * 自动计算成功/失败数量。
     */
    public static BatchOperationResultDto of(List<ItemResult> results) {
        int successCount = (int) results.stream()
                .filter(r -> "success".equals(r.getStatus()))
                .count();
        int failCount = results.size() - successCount;

        return BatchOperationResultDto.builder()
                .success(failCount == 0)
                .totalCount(results.size())
                .successCount(successCount)
                .failCount(failCount)
                .results(results)
                .build();
    }
}