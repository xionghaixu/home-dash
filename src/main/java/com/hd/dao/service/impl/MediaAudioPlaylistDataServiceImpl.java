package com.hd.dao.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hd.dao.entity.MediaAudioPlaylist;
import com.hd.dao.mapper.MediaAudioPlaylistMapper;
import com.hd.dao.service.MediaAudioPlaylistDataService;
import org.springframework.stereotype.Service;

/**
 * 音频播放列表数据服务实现
 *
 * @author system
 * @since 2026-04-26
 */
@Service
public class MediaAudioPlaylistDataServiceImpl extends ServiceImpl<MediaAudioPlaylistMapper, MediaAudioPlaylist> implements MediaAudioPlaylistDataService {
}
