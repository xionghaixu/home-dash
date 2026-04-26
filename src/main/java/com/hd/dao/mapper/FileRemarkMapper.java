package com.hd.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hd.dao.entity.FileRemark;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件备注Mapper接口。
 * 提供文件备注的数据库访问能力。
 *
 * @author team-lead
 * @version 1.0
 * @createTime 2026/04/25
 */
@Mapper
public interface FileRemarkMapper extends BaseMapper<FileRemark> {
}