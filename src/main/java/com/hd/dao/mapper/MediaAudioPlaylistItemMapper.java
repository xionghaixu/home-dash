package com.hd.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hd.dao.entity.MediaAudioPlaylistItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 播放列表曲目关联数据访问接口
 *
 * @author system
 * @since 2026-04-26
 */
@Mapper
public interface MediaAudioPlaylistItemMapper extends BaseMapper<MediaAudioPlaylistItem> {
}
