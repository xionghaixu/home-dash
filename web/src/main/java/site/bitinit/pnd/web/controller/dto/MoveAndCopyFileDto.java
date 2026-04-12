package site.bitinit.pnd.web.controller.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 文件移动和复制数据传输对象。
 * 封装文件移动或复制操作的请求参数，包括文件ID列表、目标文件夹ID列表和操作类型。
 *
 * @author john
 * @date 2020-01-22
 */
@Setter
@Getter
public class MoveAndCopyFileDto {

    public static final String MOVE_TYPE = "move";
    public static final String COPY_TYPE = "copy";

    private List<Long> fileIds;
    private List<Long> targetIds;
    private String type;
}
