package com.hd.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.hd.dao.entity.ResourceChunk;

/**
 * @author xhx
 * @version 1.0
 * @package com.hd.dao.mapper
 * @createTime 2026/04/23 23:34
 * @description 资源分块数据访问接口。提供资源分块实体的数据库操作方法，用于支持大文件分块上传功能。使用MyBatis-Plus进行数据持久化。
 *
 * 支持的功能：
 * - 分块信息的增删改查
 * - 断点续传支持（通过identifier查询分块）
 * - 完整性校验支持（MD5值存储和更新）
 */
@Mapper
public interface ResourceChunkMapper extends BaseMapper<ResourceChunk> {
}
