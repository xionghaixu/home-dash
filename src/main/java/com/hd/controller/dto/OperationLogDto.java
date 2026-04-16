package com.hd.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

/**
 * 轻量操作记录数据传输对象。
 * 用于记录文件操作历史，支持上传、下载、移动、复制、重命名、删除等操作。
 *
 * <p><b>设计说明：</b>
 * <ul>
 *   <li>当前为内存记录，仅用于阶段一轻量级操作追踪</li>
 *   <li>不持久化到数据库，应用于传输列表和最近操作展示</li>
 *   <li>阶段四将扩展为完整的操作日志持久化机制</li>
 * </ul>
 *
 * <p><b>操作类型：</b>
 * <ul>
 *   <li><b>upload</b> - 文件上传</li>
 *   <li><b>download</b> - 文件下载</li>
 *   <li><b>move</b> - 文件移动</li>
 *   <li><b>copy</b> - 文件复制</li>
 *   <li><b>rename</b> - 文件重命名</li>
 *   <li><b>delete</b> - 文件删除</li>
 *   <li><b>create</b> - 文件/文件夹创建</li>
 * </ul>
 *
 * <p><b>状态说明：</b>
 * <ul>
 *   <li><b>success</b> - 操作成功</li>
 *   <li><b>failed</b> - 操作失败</li>
 * </ul>
 *
 * @author john
 * @date 2024-01-01
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationLogDto {

    /**
     * 操作记录唯一标识符。
     */
    private String id;

    /**
     * 操作类型。
     * 取值：upload, download, move, copy, rename, delete, create
     */
    private String operationType;

    /**
     * 操作状态。
     * 取值：success, failed
     */
    private String status;

    /**
     * 关联的文件ID。
     */
    private Long fileId;

    /**
     * 文件名。
     */
    private String fileName;

    /**
     * 文件类型。
     */
    private String fileType;

    /**
     * 文件大小（字节）。
     */
    private Long fileSize;

    /**
     * 源路径（用于移动、复制操作）。
     */
    private String sourcePath;

    /**
     * 目标路径（用于移动、复制操作）。
     */
    private String targetPath;

    /**
     * 错误信息（仅失败时填充）。
     */
    private String errorMessage;

    /**
     * 操作用户ID（预留，阶段六使用）。
     */
    private String userId;

    /**
     * 操作时间。
     */
    private Date operationTime;

    /**
     * 操作耗时（毫秒）。
     */
    private Long durationMs;
}