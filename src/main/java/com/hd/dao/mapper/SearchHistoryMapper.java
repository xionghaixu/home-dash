package com.hd.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hd.dao.entity.SearchHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 搜索历史Mapper接口。
 * 提供搜索历史的数据库访问能力。
 *
 * @author team-lead
 * @version 1.0
 * @createTime 2026/04/25
 */
@Mapper
public interface SearchHistoryMapper extends BaseMapper<SearchHistory> {
}