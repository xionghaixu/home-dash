package com.hd.dao.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hd.dao.entity.MediaThumbnail;
import com.hd.dao.mapper.MediaThumbnailMapper;
import com.hd.dao.service.MediaThumbnailDataService;
import org.springframework.stereotype.Service;

/**
 * 缩略图记录数据服务实现
 *
 * @author system
 * @since 2026-04-26
 */
@Service
public class MediaThumbnailDataServiceImpl extends ServiceImpl<MediaThumbnailMapper, MediaThumbnail> implements MediaThumbnailDataService {
}
