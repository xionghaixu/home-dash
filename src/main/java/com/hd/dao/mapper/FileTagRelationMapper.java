package com.hd.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hd.dao.entity.FileTagRelation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件-标签关联Mapper接口。
 * 提供文件与标签关联关系的数据库访问能力。
 *
 * @author team-lead
 * @version 1.0
 * @createTime 2026/04/25
 */
@Mapper
public interface FileTagRelationMapper extends BaseMapper<FileTagRelation> {
}