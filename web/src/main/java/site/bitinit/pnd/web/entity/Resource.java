package site.bitinit.pnd.web.entity;

import lombok.*;
import org.apache.ibatis.type.Alias;

import java.util.Date;

/**
 * 资源实体类。
 * 表示实际存储的文件资源，包含资源的物理路径、大小、MD5校验值等信息。
 * 支持多文件引用同一资源（通过link字段实现）。
 *
 * @author john
 * @date 2020-01-08
 */
@Alias("resource")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
public class Resource {
    /** MyBatis别名常量。 */
    public static final String ALIAS = "resource";

    /** 资源唯一标识符（主键）。 */
    private Long id;

    /** 文件大小（字节）。 */
    private Long size;

    /** 文件MD5校验值，用于秒传功能。 */
    private String md5;

    /** 文件存储的相对路径，格式为：年/月/UUID.扩展名。 */
    private String path;

    /**
     * 引用计数。
     * 表示有多少个文件记录引用该资源，用于实现文件共享和自动清理。
     * 当引用计数降为0时，可以删除物理文件和资源记录。
     */
    @Builder.Default
    private Integer link = 0;

    /** 资源创建时间。 */
    @Builder.Default
    private Date createTime = new Date();

    /** 资源最后更新时间。 */
    @Builder.Default
    private Date updateTime = new Date();
}
