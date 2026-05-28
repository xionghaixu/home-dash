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
    @org.apache.ibatis.annotations.Update("UPDATE file SET is_deleted = 0, parent_id = #{parentId} WHERE id = #{id}")
    void restoreFile(@org.apache.ibatis.annotations.Param("id") Long id, @org.apache.ibatis.annotations.Param("parentId") Long parentId);

    @org.apache.ibatis.annotations.Delete("DELETE FROM file WHERE id = #{id}")
    void permanentlyDelete(@org.apache.ibatis.annotations.Param("id") Long id);

    @org.apache.ibatis.annotations.Select("SELECT * FROM file WHERE id = #{id}")
    File selectByIdWithDeleted(@org.apache.ibatis.annotations.Param("id") Long id);

    @org.apache.ibatis.annotations.Select("SELECT * FROM file WHERE parent_id = #{parentId} AND is_deleted = 1")
    java.util.List<File> selectDeletedFilesByParentId(@org.apache.ibatis.annotations.Param("parentId") Long parentId);

    @org.apache.ibatis.annotations.Select("SELECT * FROM file WHERE parent_id = #{parentId}")
    java.util.List<File> selectAllChildrenByParentId(@org.apache.ibatis.annotations.Param("parentId") Long parentId);
}
