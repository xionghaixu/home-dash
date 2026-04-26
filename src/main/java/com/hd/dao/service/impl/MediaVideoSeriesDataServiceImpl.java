package com.hd.dao.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hd.dao.entity.MediaVideoSeries;
import com.hd.dao.mapper.MediaVideoSeriesMapper;
import com.hd.dao.service.MediaVideoSeriesDataService;
import org.springframework.stereotype.Service;

/**
 * 视频系列数据服务实现
 *
 * @author system
 * @since 2026-04-26
 */
@Service
public class MediaVideoSeriesDataServiceImpl extends ServiceImpl<MediaVideoSeriesMapper, MediaVideoSeries> implements MediaVideoSeriesDataService {
}
