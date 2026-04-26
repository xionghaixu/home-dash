package com.hd.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hd.dao.entity.FileTag;
import org.apache.ibatis.annotations.Mapper;

/**
 * 标签Mapper接口。
 * 提供标签的数据库访问能力。
 *
 * @author team-lead
 * @version 1.0
 * @createTime 2026/04/25
 */
@Mapper
public interface FileTagMapper extends BaseMapper<FileTag> {
}