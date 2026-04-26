package com.hd.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hd.dao.entity.RecentUse;
import org.apache.ibatis.annotations.Mapper;

/**
 * 最近使用Mapper接口。
 * 提供最近使用记录的数据库访问能力。
 *
 * @author team-lead
 * @version 1.0
 * @createTime 2026/04/25
 */
@Mapper
public interface RecentUseMapper extends BaseMapper<RecentUse> {
}