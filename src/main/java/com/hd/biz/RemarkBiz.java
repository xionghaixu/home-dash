package com.hd.biz;

import com.hd.model.vo.FileRemarkVo;
import java.util.List;

/**
 * 文件备注业务接口。
 * 定义文件备注的CRUD操作。
 *
 * @author team-lead
 * @version 1.0
 * @createTime 2026/04/25
 */
public interface RemarkBiz {

    /**
     * 获取文件备注。
     *
     * @param resourceId 资源ID
     * @return 文件备注VO
     */
    FileRemarkVo getFileRemark(Long resourceId);

    /**
     * 保存或更新文件备注。
     *
     * @param resourceId 资源ID
     * @param remarkContent 备注内容
     * @return 文件备注VO
     */
    FileRemarkVo saveFileRemark(Long resourceId, String remarkContent);

    /**
     * 删除文件备注。
     *
     * @param resourceId 资源ID
     * @return 是否成功
     */
    boolean deleteFileRemark(Long resourceId);

    /**
     * 批量删除文件备注。
     *
     * @param resourceIds 资源ID列表
     * @return 删除数量
     */
    int batchDeleteFileRemarks(List<Long> resourceIds);

    /**
     * 批量获取文件备注。
     *
     * @param resourceIds 资源ID列表
     * @return 文件备注列表
     */
    List<FileRemarkVo> getFileRemarks(List<Long> resourceIds);
}
