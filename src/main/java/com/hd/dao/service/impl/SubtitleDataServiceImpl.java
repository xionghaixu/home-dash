package com.hd.dao.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hd.dao.entity.Subtitle;
import com.hd.dao.mapper.SubtitleMapper;
import com.hd.dao.service.SubtitleDataService;
import org.springframework.stereotype.Service;

/**
 * 字幕关联数据服务实现
 *
 * @author system
 * @since 2026-04-26
 */
@Service
public class SubtitleDataServiceImpl extends ServiceImpl<SubtitleMapper, Subtitle> implements SubtitleDataService {
}
