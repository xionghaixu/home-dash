package com.hd.dao.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hd.dao.entity.MediaAudioMetadata;
import com.hd.dao.mapper.MediaAudioMetadataMapper;
import com.hd.dao.service.MediaAudioMetadataDataService;
import org.springframework.stereotype.Service;

/**
 * 音频元数据数据服务实现
 *
 * @author system
 * @since 2026-04-26
 */
@Service
public class MediaAudioMetadataDataServiceImpl extends ServiceImpl<MediaAudioMetadataMapper, MediaAudioMetadata> implements MediaAudioMetadataDataService {
}
