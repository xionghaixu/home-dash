package com.hd.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hd.dao.entity.WatchProgress;
import org.apache.ibatis.annotations.Mapper;

/**
 * 视频观看进度数据访问接口
 *
 * @author system
 * @since 2026-04-26
 */
@Mapper
public interface WatchProgressMapper extends BaseMapper<WatchProgress> {
}
