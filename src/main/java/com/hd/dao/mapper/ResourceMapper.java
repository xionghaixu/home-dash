package com.hd.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.hd.dao.entity.Resource;

/**
 * 资源数据访问接口。
 * 提供资源实体的数据库操作方法，包括保存、查询、删除和更新链接数等操作。
 * 使用MyBatis-Plus进行数据持久化。
 *
 * @author john
 * @date 2020-01-11
 */
@Mapper
public interface ResourceMapper extends BaseMapper<Resource> {
}