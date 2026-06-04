package com.hd.model.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 资源分块数据传输对象。
 * 用于接收客户端上传的文件分块请求参数，替代直接使用实体类 ResourceChunk。
 */
@Data
public class ResourceChunkDTO {

    /** 当前分块的编号（从1开始）。 */
    private Integer chunkNumber;

    /** 分块大小（字节）。 */
    private Long chunkSize;

    /** 当前分块的实际大小（字节）。 */
    private Long currentChunkSize;

    /** 文件总大小（字节）。 */
    private Long totalSize;

    /** 文件唯一标识符（MD5或UUID）。 */
    private String identifier;

    /** 原始文件名。 */
    private String filename;

    /** 相对路径。 */
    private String relativePath;

    /** 总分块数。 */
    private Integer totalChunks;

    /** 分块文件数据。 */
    private transient MultipartFile file;
}
