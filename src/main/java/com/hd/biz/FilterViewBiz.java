package com.hd.biz;

import com.hd.model.vo.FilterViewVo;
import java.util.List;

/**
 * 筛选视图业务接口。
 * 定义筛选视图的保存、读取和管理操作。
 *
 * @author team-lead
 * @version 1.0
 * @createTime 2026/04/25
 */
public interface FilterViewBiz {

    /**
     * 保存筛选视图。
     *
     * @param viewName 视图名称
     * @param viewParams 视图参数（JSON格式）
     * @param isDefault 是否设为默认
     * @return 视图ID
     */
    Long saveFilterView(String viewName, String viewParams, Boolean isDefault);

    /**
     * 更新筛选视图。
     *
     * @param id 视图ID
     * @param viewName 视图名称
     * @param viewParams 视图参数
     * @param isDefault 是否默认
     * @return 是否成功
     */
    boolean updateFilterView(Long id, String viewName, String viewParams, Boolean isDefault);

    /**
     * 删除筛选视图。
     *
     * @param id 视图ID
     * @return 是否成功
     */
    boolean deleteFilterView(Long id);

    /**
     * 获取所有筛选视图。
     *
     * @return 筛选视图列表
     */
    List<FilterViewVo> getAllFilterViews();

    /**
     * 获取默认筛选视图。
     *
     * @return 默认视图
     */
    FilterViewVo getDefaultFilterView();

    /**
     * 设置默认视图。
     *
     * @param id 视图ID
     * @return 是否成功
     */
    boolean setDefaultView(Long id);
}
