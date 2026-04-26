package com.hd.dao.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hd.dao.entity.MediaAudioPlaylistItem;
import com.hd.dao.mapper.MediaAudioPlaylistItemMapper;
import com.hd.dao.service.MediaAudioPlaylistItemDataService;
import org.springframework.stereotype.Service;

/**
 * 播放列表曲目关联数据服务实现
 *
 * @author system
 * @since 2026-04-26
 */
@Service
public class MediaAudioPlaylistItemDataServiceImpl extends ServiceImpl<MediaAudioPlaylistItemMapper, MediaAudioPlaylistItem> implements MediaAudioPlaylistItemDataService {
}
