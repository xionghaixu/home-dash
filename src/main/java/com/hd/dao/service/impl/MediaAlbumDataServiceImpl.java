package com.hd.dao.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hd.dao.entity.MediaAlbum;
import com.hd.dao.mapper.MediaAlbumMapper;
import com.hd.dao.service.MediaAlbumDataService;
import org.springframework.stereotype.Service;

/**
 * 相册数据服务实现
 *
 * @author system
 * @since 2026-04-26
 */
@Service
public class MediaAlbumDataServiceImpl extends ServiceImpl<MediaAlbumMapper, MediaAlbum> implements MediaAlbumDataService {
}
