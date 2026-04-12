package site.bitinit.pnd.web.controller.dto;

import lombok.Getter;
import lombok.Setter;
import site.bitinit.pnd.web.entity.File;

/**
 * 文件合并数据传输对象。
 * 继承自File实体，用于封装分块上传合并时的文件信息。
 * 包含文件唯一标识符用于关联分块。
 *
 * @author john
 * @date 2020-01-27
 */
@Setter
@Getter
public class MergeFileDto extends File {

    private String identifier;
}
