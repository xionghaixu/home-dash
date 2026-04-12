package site.bitinit.pnd.web.entity;

import lombok.*;
import org.apache.ibatis.type.Alias;
import site.bitinit.pnd.web.config.FileType;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Date;

/**
 * 文件实体类。
 * 表示文件系统中的文件或文件夹，包含文件的基本信息如名称、类型、大小、父目录等。
 * 支持文件和文件夹的层级结构管理。
 *
 * @author john
 * @date 2020-01-08
 */
@Alias("file")
@Setter
@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class File {
    /** MyBatis别名常量。 */
    public static final String ALIAS = "file";

    /**
     * 根文件对象。
     * 表示文件系统的根目录，ID为0，名称为"全部文件"。
     */
    public static final File ROOT_FILE = File.builder()
            .id(0L).fileName("全部文件").type(FileType.FOLDER.toString()).parentId(0L)
            .build();

    /** 文件唯一标识符（主键）。 */
    private Long id;

    /** 父文件夹ID，0表示根目录。 */
    @NotNull(message = "parentId不能为null")
    private Long parentId;

    /** 文件或文件夹名称，最大长度100个字符。 */
    @NotEmpty(message = "fileName不能为空")
    private String fileName;

    /** 文件类型，如FOLDER、VIDEO、AUDIO等。 */
    @NotEmpty(message = "type不能为空")
    private String type;

    /** 文件大小（字节），文件夹可为null。 */
    private Long size;

    /** 关联的资源ID，文件夹可为null。 */
    private Long resourceId;

    /** 文件创建时间。 */
    @Builder.Default
    private Date createTime = new Date();

    /** 文件最后更新时间。 */
    @Builder.Default
    private Date updateTime = new Date();
}
