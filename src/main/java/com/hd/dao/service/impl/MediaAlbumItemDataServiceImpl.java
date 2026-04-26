package com.hd.dao.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hd.dao.entity.MediaAlbumItem;
import com.hd.dao.mapper.MediaAlbumItemMapper;
import com.hd.dao.service.MediaAlbumItemDataService;
import org.springframework.stereotype.Service;

/**
 * 相册图片关联数据服务实现
 *
 * @author system
 * @since 2026-04-26
 */
@Service
public class MediaAlbumItemDataServiceImpl extends ServiceImpl<MediaAlbumItemMapper, MediaAlbumItem> implements MediaAlbumItemDataService {
}
