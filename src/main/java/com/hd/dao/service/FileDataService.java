package com.hd.dao.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hd.dao.entity.File;

import java.util.List;

/**
 * @author xhx
 * @version 1.0
 * @package com.hd.dao.service
 * @createTime 2026/04/23 23:34
 * @description 文件数据访问服务。对外提供文件相关的持久化操作，内部委托Mapper实现。
 */
public interface FileDataService extends IService<File> {

    /**
     * 根据ID查询文件（包括已逻辑删除的文件）。
     * 绕过MyBatis-Plus的@TableLogic自动过滤。
     *
     * @param id 文件ID
     * @return 文件实体，不存在时返回null
     */
    File selectByIdWithDeleted(Long id);

    /**
     * 根据ID物理删除文件（直接从数据库中删除记录）。
     * 绕过MyBatis-Plus的逻辑删除机制。
     *
     * @param id 文件ID
     */
    void permanentlyDelete(Long id);

    /**
     * 查询指定父文件夹下的所有子文件（包括已逻辑删除的文件）。
     *
     * @param parentId 父文件夹ID
     * @return 子文件列表
     */
    List<File> selectAllChildrenByParentId(Long parentId);

    /**
     * 查询指定父文件夹下已逻辑删除的文件。
     *
     * @param parentId 父文件夹ID
     * @return 已删除的子文件列表
     */
    List<File> selectDeletedFilesByParentId(Long parentId);

    /**
     * 恢复已逻辑删除的文件，将其设为未删除状态并更新父文件夹ID。
     *
     * @param id       文件ID
     * @param parentId 恢复后的父文件夹ID
     */
    void restoreFile(Long id, Long parentId);
}

