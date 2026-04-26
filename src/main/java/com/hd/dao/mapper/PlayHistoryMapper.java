package com.hd.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hd.dao.entity.PlayHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 播放历史数据访问接口
 *
 * @author system
 * @since 2026-04-26
 */
@Mapper
public interface PlayHistoryMapper extends BaseMapper<PlayHistory> {
}
