package com.hd.biz.impl;

import com.hd.biz.FilterViewBiz;
import com.hd.dao.entity.FilterView;
import com.hd.dao.service.FilterViewDataService;
import com.hd.model.vo.FilterViewVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 筛选视图业务实现类。
 *
 * @author team-lead
 * @version 1.0
 * @createTime 2026/04/25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FilterViewBizImpl implements FilterViewBiz {

    private final FilterViewDataService filterViewDataService;

    @Override
    @Transactional
    public Long saveFilterView(String viewName, String viewParams, Boolean isDefault) {
        log.info("保存筛选视图 [viewName={}, isDefault={}]", viewName, isDefault);

        if (Boolean.TRUE.equals(isDefault)) {
            clearDefaultFlag();
        }

        FilterView filterView = FilterView.builder()
                .viewName(viewName)
                .viewParams(viewParams)
                .isDefault(isDefault != null ? isDefault : false)
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();

        filterViewDataService.save(filterView);
        log.info("筛选视图已保存 [id={}]", filterView.getId());

        return filterView.getId();
    }

    @Override
    @Transactional
    public boolean updateFilterView(Long id, String viewName, String viewParams, Boolean isDefault) {
        log.info("更新筛选视图 [id={}]", id);

        FilterView filterView = filterViewDataService.getById(id);
        if (filterView == null) {
            log.warn("筛选视图不存在 [id={}]", id);
            return false;
        }

        if (Boolean.TRUE.equals(isDefault)) {
            clearDefaultFlag();
        }

        if (viewName != null) {
            filterView.setViewName(viewName);
        }
        if (viewParams != null) {
            filterView.setViewParams(viewParams);
        }
        if (isDefault != null) {
            filterView.setIsDefault(isDefault);
        }
        filterView.setUpdatedAt(new Date());

        filterViewDataService.updateById(filterView);
        log.info("筛选视图已更新 [id={}]", id);

        return true;
    }

    @Override
    @Transactional
    public boolean deleteFilterView(Long id) {
        log.info("删除筛选视图 [id={}]", id);

        boolean result = filterViewDataService.removeById(id);
        log.info("删除筛选视图完成 [id={}, result={}]", id, result);

        return result;
    }

    @Override
    public List<FilterViewVo> getAllFilterViews() {
        log.info("获取所有筛选视图");

        List<FilterView> views = filterViewDataService.lambdaQuery()
                .orderByDesc(FilterView::getIsDefault)
                .orderByDesc(FilterView::getUpdatedAt)
                .list();

        return views.stream()
                .map(this::convertToFilterViewVo)
                .collect(Collectors.toList());
    }

    @Override
    public FilterViewVo getDefaultFilterView() {
        log.info("获取默认筛选视图");

        FilterView view = filterViewDataService.lambdaQuery()
                .eq(FilterView::getIsDefault, true)
                .last("LIMIT 1")
                .one();

        return view != null ? convertToFilterViewVo(view) : null;
    }

    @Override
    @Transactional
    public boolean setDefaultView(Long id) {
        log.info("设置默认视图 [id={}]", id);

        FilterView view = filterViewDataService.getById(id);
        if (view == null) {
            log.warn("筛选视图不存在 [id={}]", id);
            return false;
        }

        clearDefaultFlag();

        view.setIsDefault(true);
        view.setUpdatedAt(new Date());
        filterViewDataService.updateById(view);

        log.info("默认视图已设置 [id={}]", id);
        return true;
    }

    private void clearDefaultFlag() {
        List<FilterView> defaultViews = filterViewDataService.lambdaQuery()
                .eq(FilterView::getIsDefault, true)
                .list();

        for (FilterView v : defaultViews) {
            v.setIsDefault(false);
            v.setUpdatedAt(new Date());
            filterViewDataService.updateById(v);
        }
    }

    private FilterViewVo convertToFilterViewVo(FilterView view) {
        return FilterViewVo.builder()
                .id(view.getId())
                .viewName(view.getViewName())
                .viewParams(view.getViewParams())
                .isDefault(view.getIsDefault())
                .build();
    }
}
