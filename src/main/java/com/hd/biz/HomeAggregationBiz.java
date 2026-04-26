package com.hd.biz;

import com.hd.model.dto.HomeMediaSummaryDto;

/**
 * 首页媒体聚合业务接口
 *
 * @author system
 * @since 2026-04-26
 */
public interface HomeAggregationBiz {

    /**
     * 获取首页媒体聚合数据
     *
     * @return 首页媒体聚合响应
     */
    HomeMediaSummaryDto getHomeSummary();
}
