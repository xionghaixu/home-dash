package com.hd.dao.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hd.dao.entity.MediaVideoMetadata;
import com.hd.dao.mapper.MediaVideoMetadataMapper;
import com.hd.dao.service.MediaVideoMetadataDataService;
import org.springframework.stereotype.Service;

/**
 * 视频元数据数据服务实现
 *
 * @author system
 * @since 2026-04-26
 */
@Service
public class MediaVideoMetadataDataServiceImpl extends ServiceImpl<MediaVideoMetadataMapper, MediaVideoMetadata> implements MediaVideoMetadataDataService {
}
