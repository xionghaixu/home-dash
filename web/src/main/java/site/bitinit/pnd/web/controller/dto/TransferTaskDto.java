package site.bitinit.pnd.web.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

/**
 * 轻量传输任务数据传输对象。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferTaskDto {

    private String identifier;
    private String fileName;
    private String status;
    private String errorMessage;
    private Long totalSize;
    private Long parentId;
    private Long fileId;
    private Integer totalChunks;
    private Integer uploadedChunks;
    private Integer progress;
    private Date createTime;
    private Date updateTime;
}
