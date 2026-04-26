package com.hd.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hd.dao.entity.FilterView;
import org.apache.ibatis.annotations.Mapper;

/**
 * 筛选视图Mapper接口。
 * 提供筛选视图配置的数据库访问能力。
 *
 * @author team-lead
 * @version 1.0
 * @createTime 2026/04/25
 */
@Mapper
public interface FilterViewMapper extends BaseMapper<FilterView> {
}