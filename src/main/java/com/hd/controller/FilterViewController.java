package com.hd.controller;

import com.hd.biz.FilterViewBiz;
import com.hd.common.HomeDashConstants;
import com.hd.model.dto.FilterViewDto;
import com.hd.model.dto.ResponseDto;
import com.hd.model.vo.FilterViewVo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 筛选视图控制器。
 * 提供筛选视图的CRUD REST API接口。
 *
 * @author team-lead
 * @version 1.0
 * @createTime 2026/04/25
 */
@Slf4j
@RestController
@RequestMapping(HomeDashConstants.API_VERSION)
@RequiredArgsConstructor
public class FilterViewController {

    private final FilterViewBiz filterViewBiz;

    /**
     * 保存筛选视图。
     */
    @PostMapping("/filter-view")
    public ResponseEntity<ResponseDto> saveFilterView(@Valid @RequestBody FilterViewDto dto) {
        log.info("保存筛选视图请求 [viewName={}]", dto.getViewName());
        Long id = filterViewBiz.saveFilterView(dto.getViewName(), dto.getViewParams(), dto.getIsDefault());
        return ResponseEntity.ok(ResponseDto.success(id));
    }

    /**
     * 更新筛选视图。
     */
    @PutMapping("/filter-view/{id}")
    public ResponseEntity<ResponseDto> updateFilterView(
            @PathVariable Long id,
            @RequestBody FilterViewDto dto) {
        log.info("更新筛选视图请求 [id={}]", id);
        boolean result = filterViewBiz.updateFilterView(id, dto.getViewName(), dto.getViewParams(), dto.getIsDefault());
        return ResponseEntity.ok(ResponseDto.success(result));
    }

    /**
     * 删除筛选视图。
     */
    @DeleteMapping("/filter-view/{id}")
    public ResponseEntity<ResponseDto> deleteFilterView(@PathVariable Long id) {
        log.info("删除筛选视图请求 [id={}]", id);
        boolean result = filterViewBiz.deleteFilterView(id);
        return ResponseEntity.ok(ResponseDto.success(result));
    }

    /**
     * 获取所有筛选视图。
     */
    @GetMapping("/filter-view/list")
    public ResponseEntity<ResponseDto> getAllFilterViews() {
        log.info("获取所有筛选视图请求");
        List<FilterViewVo> views = filterViewBiz.getAllFilterViews();
        return ResponseEntity.ok(ResponseDto.success(views));
    }

    /**
     * 获取默认筛选视图。
     */
    @GetMapping("/filter-view/default")
    public ResponseEntity<ResponseDto> getDefaultFilterView() {
        log.info("获取默认筛选视图请求");
        FilterViewVo view = filterViewBiz.getDefaultFilterView();
        return ResponseEntity.ok(ResponseDto.success(view));
    }

    /**
     * 设置默认视图。
     */
    @PutMapping("/filter-view/{id}/default")
    public ResponseEntity<ResponseDto> setDefaultView(@PathVariable Long id) {
        log.info("设置默认视图请求 [id={}]", id);
        boolean result = filterViewBiz.setDefaultView(id);
        return ResponseEntity.ok(ResponseDto.success(result));
    }
}
