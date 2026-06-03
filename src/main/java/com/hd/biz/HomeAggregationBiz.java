package com.hd.biz;

import com.hd.model.dto.HomeMediaSummaryDTO;

/**
 * 首页媒体聚合业务接口
 *
 * @author xhx
 * @since 2026-04-26
 */
public interface HomeAggregationBiz {

    /**
     * 获取首页媒体聚合数据
     *
     * @return 首页媒体聚合响应
     */
    HomeMediaSummaryDTO getHomeSummary();
}
