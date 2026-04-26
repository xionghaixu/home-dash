package com.hd.dao.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hd.dao.entity.MediaScanTask;
import com.hd.dao.mapper.MediaScanTaskMapper;
import com.hd.dao.service.MediaScanTaskDataService;
import org.springframework.stereotype.Service;

/**
 * 媒体扫描任务数据服务实现
 *
 * @author system
 * @since 2026-04-26
 */
@Service
public class MediaScanTaskDataServiceImpl extends ServiceImpl<MediaScanTaskMapper, MediaScanTask> implements MediaScanTaskDataService {
}
