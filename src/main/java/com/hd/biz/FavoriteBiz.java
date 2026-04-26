package com.hd.biz;

import com.hd.model.vo.*;
import java.util.List;

/**
 * 收藏业务接口。
 * 定义文件收藏的相关操作。
 *
 * @author team-lead
 * @version 1.0
 * @createTime 2026/04/25
 */
public interface FavoriteBiz {

    /**
     * 添加收藏。
     *
     * @param resourceId 资源ID
     * @return 操作结果
     */
    boolean addFavorite(Long resourceId);

    /**
     * 取消收藏。
     *
     * @param resourceId 资源ID
     * @return 操作结果
     */
    boolean removeFavorite(Long resourceId);

    /**
     * 批量添加收藏。
     *
     * @param resourceIds 资源ID列表
     * @return 批量操作结果
     */
    BatchOperationResultVo batchAddFavorite(List<Long> resourceIds);

    /**
     * 批量取消收藏。
     *
     * @param resourceIds 资源ID列表
     * @return 批量操作结果
     */
    BatchOperationResultVo batchRemoveFavorite(List<Long> resourceIds);

    /**
     * 检查收藏状态。
     *
     * @param resourceId 资源ID
     * @return 是否已收藏
     */
    boolean isFavorite(Long resourceId);

    /**
     * 获取收藏列表。
     *
     * @param page 页码
     * @param pageSize 每页数量
     * @return 收藏文件列表
     */
    List<FileDetailVo> getFavoriteList(Integer page, Integer pageSize);

    /**
     * 获取收藏数量。
     *
     * @return 收藏总数
     */
    Long getFavoriteCount();
}