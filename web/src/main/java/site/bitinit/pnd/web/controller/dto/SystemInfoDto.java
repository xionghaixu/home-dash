package site.bitinit.pnd.web.controller.dto;

import lombok.*;

/**
 * 系统信息数据传输对象。
 * 封装系统状态信息，包括存储容量、文件数量统计等。
 *
 * @author john
 * @date 2020-02-10
 */
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemInfoDto {

    private Long totalCap;
    private Long usableCap;

    private Integer totalNum;
    private Integer folderNum;
    private Integer fileNum;

    private Integer videoNum;
    private Integer audioNum;

}
