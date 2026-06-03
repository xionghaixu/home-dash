package com.hd.controller;

import com.hd.biz.HomeAggregationBiz;
import com.hd.model.dto.HomeMediaSummaryDTO;
import com.hd.model.dto.ResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页媒体聚合控制器
 *
 * @author xhx
 * @since 2026-04-26
 */
@RestController
@RequestMapping("/v1/media/home")
@RequiredArgsConstructor
public class HomeMediaController {

    private final HomeAggregationBiz homeAggregationBiz;

    /**
     * 获取首页媒体聚合数据
     *
     * @return 首页媒体聚合响应
     */
    @GetMapping("/summary")
    public ResponseEntity<ResponseDTO> getHomeSummary() {
        HomeMediaSummaryDTO summary = homeAggregationBiz.getHomeSummary();
        return ResponseEntity.ok(ResponseDTO.success(summary));
    }
}
