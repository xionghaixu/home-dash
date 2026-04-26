package com.hd.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hd.dao.entity.Subtitle;
import org.apache.ibatis.annotations.Mapper;

/**
 * 字幕关联数据访问接口
 *
 * @author system
 * @since 2026-04-26
 */
@Mapper
public interface SubtitleMapper extends BaseMapper<Subtitle> {
}
