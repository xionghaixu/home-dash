package com.hd.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.hd.dao.entity.File;

/**
 * @author xhx
 * @version 1.0
 * @package com.hd.dao.mapper
 * @createTime 2026/04/23 23:34
 * @description 文件数据访问接口。提供文件实体的数据库操作方法，包括增删改查等基本操作。使用MyBatis-Plus进行数据持久化。
 */
@Mapper
public interface FileMapper extends BaseMapper<File> {
}
