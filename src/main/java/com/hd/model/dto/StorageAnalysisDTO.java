package com.hd.model.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * 存储分析响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageAnalysisDTO {

    /** 总已用空间（字节） */
    private Long totalSize;

    /** 文件总数 */
    private Integer totalFileCount;

    /** 按类型分布 */
    private List<TypeBreakdown> typeBreakdown;

    /** 目录占用排行 Top10 */
    private List<DirRanking> topDirs;

    /** 大文件排行 Top20 */
    private List<FileRanking> topFiles;

    /** 可清理空间估算 */
    private CleanableEstimate cleanable;

    /** 清理建议列表 */
    private List<CleanupSuggestion> suggestions;

    /**
     * 类型分布
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TypeBreakdown {
        private String type;
        private String label;
        private Long size;
        private Integer count;
        private Double percent;
    }

    /**
     * 目录排行
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DirRanking {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long dirId;
        private String dirName;
        private String path;
        private Long size;
        private Integer fileCount;
    }

    /**
     * 文件排行
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileRanking {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long fileId;
        private String fileName;
        private Long size;
        private String type;
        private Date updateTime;
    }

    /**
     * 可清理空间估算
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CleanableEstimate {
        private Long recycleBinSize;
        private Integer recycleBinCount;
        private Long duplicateSize;
        private Integer duplicateGroupCount;
        private Integer emptyDirCount;
    }

    /**
     * 清理建议
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CleanupSuggestion {
        /** 类型: recycle_bin, duplicate, empty_dir, large_file */
        private String type;
        /** 标题 */
        private String title;
        /** 描述 */
        private String description;
        /** 可回收空间（字节） */
        private Long recoverableSize;
        /** 前端跳转路径 */
        private String actionPath;
    }
}
