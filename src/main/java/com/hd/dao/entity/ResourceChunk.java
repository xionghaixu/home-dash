package com.hd.dao.entity;

import lombok.*;
import org.apache.ibatis.type.Alias;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;

/**
 * 资源分块实体类。
 * 表示大文件上传过程中的文件分块信息，用于实现断点续传和分块上传功能。
 * 包含分块编号、分块大小、文件标识符、MD5校验值等信息。
 *
 * @author john
 * @date 2020-01-27
 */
@Alias("resourceChunk")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResourceChunk {

    /** MyBatis别名常量。 */
    public static final String ALIAS = "resourceChunk";

    /** 当前分块的编号（从1开始）。 */
    private Integer chunkNumber;

    /** 分块大小（字节），所有分块大小相同（最后一个可能不同）。 */
    private Long chunkSize;

    /** 当前分块的实际大小（字节）。 */
    private Long currentChunkSize;

    /** 文件总大小（字节）。 */
    private Long totalSize;

    /**
     * 文件唯一标识符。
     * 用于标识同一个文件的所有分块，通常使用文件MD5或随机生成的唯一ID。
     */
    private String identifier;

    /** 原始文件名，包含扩展名。 */
    private String filename;

    /**
     * 相对路径。
     * 如果上传的是文件夹中的文件，此字段表示文件的相对路径。
     */
    private String relativePath;

    /** 总分块数。 */
    private Integer totalChunks;

    /** 分块文件数据（非持久化，仅用于接收上传）。 */
    private transient MultipartFile file;

    /** 分块创建时间。 */
    @Builder.Default
    private Date createTime = new Date();

    /** 分块最后更新时间。 */
    @Builder.Default
    private Date updateTime = new Date();

    /**
     * 分块数据的MD5校验值。
     * 用于验证分块数据的完整性和一致性，支持断点续传时的完整性校验。
     * 在保存分块时自动计算并存储。
     */
    private String md5;
}
