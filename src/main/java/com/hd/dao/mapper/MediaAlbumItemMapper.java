package com.hd.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hd.dao.entity.MediaAlbumItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 相册图片关联数据访问接口
 *
 * @author system
 * @since 2026-04-26
 */
@Mapper
public interface MediaAlbumItemMapper extends BaseMapper<MediaAlbumItem> {
}
