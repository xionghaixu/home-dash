package com.hd.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import com.hd.dao.entity.File;

import java.util.Date;

/**
 * @author xhx
 * @version 1.0
 * @package com.hd.model.dto
 * @createTime 2026/04/23 23:34
 * @description 文件合并数据传输对象。用于分块上传时将所有分块合并成完整文件的请求参数。
 *
 * <p><b>使用场景：</b>
 * <ul>
 *   <li>分块上传完成后，调用合并接口将所有分块合并成完整文件</li>
 *   <li>秒传场景下也会调用合并接口（只是不走实际合并流程）</li>
 * </ul>
 *
 * <p><b>字段说明：</b>
 * <ul>
 *   <li>identifier - 文件唯一标识符（MD5或UUID），用于关联所有分块</li>
 *   <li>fileName - 原始文件名</li>
 *   <li>size - 文件总大小（字节）</li>
 *   <li>parentId - 目标父目录ID</li>
 *   <li>totalChunks -总分块数</li>
 *   <li>createTime - 创建时间（用于文件存储路径组织）</li>
 * </ul>
 *
 * <p><b>请求示例：</b>
 * <pre>
 * {
 *   "identifier": "d41d8cd98f00b204e9800998ecf8427e",
 *   "fileName": "document.pdf",
 *   "size": 1048576,
 *   "parentId": 1,
 *   "totalChunks": 10,
 *   "createTime": "2024-01-15T10:30:00"
 * }
 * </pre>
 */
@Setter
@Getter
public class MergeFileDto extends File {

    /**
     * 文件唯一标识符。
     * 通常为文件MD5值，用于关联所有分块。
     */
    @NotBlank(message = "文件标识符不能为空")
    private String identifier;

    /**
     * 文件总大小（字节）。
     */
    @NotNull(message = "文件大小不能为空")
    private Long size;

    /**
     * 总分块数。
     */
    @NotNull(message = "总分块数不能为空")
    private Integer totalChunks;

    /**
     * 创建时间。
     * 用于确定文件的存储路径（按年月组织）。
     */
    private Date createTime;

    /**
     * 资源ID（合并成功后由服务端填充）。
     */
    private Long resourceId;
}
