package com.hd.dao.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hd.dao.entity.WatchProgress;
import com.hd.dao.mapper.WatchProgressMapper;
import com.hd.dao.service.WatchProgressDataService;
import org.springframework.stereotype.Service;

/**
 * 视频观看进度数据服务实现
 *
 * @author system
 * @since 2026-04-26
 */
@Service
public class WatchProgressDataServiceImpl extends ServiceImpl<WatchProgressMapper, WatchProgress> implements WatchProgressDataService {
}
