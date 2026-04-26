package com.hd.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hd.dao.entity.MediaSummaryCache;
import org.apache.ibatis.annotations.Mapper;

/**
 * 首页聚合缓存数据访问接口
 *
 * @author system
 * @since 2026-04-26
 */
@Mapper
public interface MediaSummaryCacheMapper extends BaseMapper<MediaSummaryCache> {
}
