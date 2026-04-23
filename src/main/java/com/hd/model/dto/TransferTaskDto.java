package com.hd.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

/**
 * 轻量传输任务数据传输对象。
 * 用于在内存中追踪上传/下载任务的状态，不持久化到数据库。
 *
 * <p><b>状态说明：</b>
 * <ul>
 *   <li><b>waiting</b> - 任务等待中，未开始</li>
 *   <li><b>uploading</b> - 上传进行中</li>
 *   <li><b>completed</b> - 上传成功完成</li>
 *   <li><b>failed</b> - 上传失败，附带错误信息</li>
 *   <li><b>cancelled</b> - 用户取消上传</li>
 * </ul>
 *
 * <p><b>使用场景：</b>
 * <ul>
 *   <li>传输列表页面展示</li>
 *   <li>上传进度追踪</li>
 *   <li>传输状态同步</li>
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
public class TransferTaskDto {

    /**
     * 文件唯一标识符（MD5或UUID）。
     */
    private String identifier;

    /**
     * 文件名。
     */
    private String fileName;

    /**
     * 文件类型。
     */
    private String fileType;

    /**
     * 操作类型：upload / download。
     */
    private String operationType;

    /**
     * 任务状态。
     * 取值：waiting | uploading | completed | failed | cancelled
     */
    private String status;

    /**
     * 错误信息（仅失败时填充）。
     */
    private String errorMessage;

    /**
     * 文件总大小（字节）。
     */
    private Long totalSize;

    /**
     * 目标父目录ID。
     */
    private Long parentId;

    /**
     * 创建后的文件ID（完成后填充）。
     */
    private Long fileId;

    /**
     * 总分块数。
     */
    private Integer totalChunks;

    /**
     * 已上传分块数。
     */
    private Integer uploadedChunks;

    /**
     * 上传进度百分比（0-100）。
     */
    private Integer progress;

    /**
     * 任务创建时间。
     */
    private Date createTime;

    /**
     * 最后更新时间。
     */
    private Date updateTime;
}
