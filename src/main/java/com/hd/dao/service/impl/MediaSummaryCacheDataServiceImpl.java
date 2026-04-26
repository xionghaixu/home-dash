package com.hd.dao.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hd.dao.entity.MediaSummaryCache;
import com.hd.dao.mapper.MediaSummaryCacheMapper;
import com.hd.dao.service.MediaSummaryCacheDataService;
import org.springframework.stereotype.Service;

/**
 * 首页聚合缓存数据服务实现
 *
 * @author system
 * @since 2026-04-26
 */
@Service
public class MediaSummaryCacheDataServiceImpl extends ServiceImpl<MediaSummaryCacheMapper, MediaSummaryCache> implements MediaSummaryCacheDataService {
}
