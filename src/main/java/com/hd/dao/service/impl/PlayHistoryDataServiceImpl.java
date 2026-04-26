package com.hd.dao.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hd.dao.entity.PlayHistory;
import com.hd.dao.mapper.PlayHistoryMapper;
import com.hd.dao.service.PlayHistoryDataService;
import org.springframework.stereotype.Service;

/**
 * 播放历史数据服务实现
 *
 * @author system
 * @since 2026-04-26
 */
@Service
public class PlayHistoryDataServiceImpl extends ServiceImpl<PlayHistoryMapper, PlayHistory> implements PlayHistoryDataService {
}
