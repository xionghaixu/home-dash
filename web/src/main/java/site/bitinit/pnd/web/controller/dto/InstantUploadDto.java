package site.bitinit.pnd.web.controller.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 秒传请求数据传输对象。
 */
@Getter
@Setter
public class InstantUploadDto {

    private String md5;
    private String fileName;
    private Long parentId;
}
