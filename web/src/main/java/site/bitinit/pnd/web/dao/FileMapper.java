package site.bitinit.pnd.web.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import site.bitinit.pnd.web.entity.File;

import java.util.Date;
import java.util.List;

/**
 * 文件数据访问接口。
 * 提供文件实体的数据库操作方法，包括增删改查等基本操作。
 * 使用MyBatis进行数据持久化。
 *
 * @author john
 * @date 2020-01-08
 */
@Mapper
public interface FileMapper {

    /**
     * 根据id获取文件
     * 
     * @param id id
     * @return file
     */
    File findById(Long id);

    /**
     * 根据id列表批量获取文件
     * 
     * @param ids id列表
     * @return 文件列表
     */
    List<File> findByIds(@Param("ids") List<Long> ids);

    /**
     * 通过parentId获取所有子文件
     *
     * @param parentId  parentId
     * @param sort      是否根据id排序
     * @param sortBy    排序字段
     * @param sortOrder 排序方式
     * @return list
     */
    List<File> findByParentId(@Param("parentId") Long parentId, @Param("sort") boolean sort,
                              @Param("sortBy") String sortBy, @Param("sortOrder") String sortOrder);

    /**
     * 通过parentId获取所有子文件，悲观锁
     * 
     * @param parentId parentId
     * @return list
     */
    List<File> findByParentIdForUpdate(@Param("parentId") Long parentId);

    /**
     * 保存文件
     * 
     * @param file file
     */
    void save(File file);

    /**
     * 更新文件：fileName或parentId
     * 
     * @param file file
     */
    void update(File file);

    /**
     * 批量更新parentId
     * 
     * @param ids        ids
     * @param parentId   parentId
     * @param updateTime updateTime
     */
    void updateParentId(@Param("ids") List<Long> ids,
            @Param("parentId") Long parentId,
            @Param("updateTime") Date updateTime);

    /**
     * 根据id批量删除file
     * 
     * @param ids ids
     */
    void deleteByIds(@Param("ids") List<Long> ids);

    /**
     * 获取文件信息
     * 
     * @return list
     */
    List<File> getAllFileType();

    /**
     * 批量保存文件。
     * 用于批量复制文件时，一次性插入多个文件记录，提高性能。
     * 
     * 性能优化：
     * - 使用批量插入，减少数据库操作次数
     * - 使用 MyBatis 的 foreach 标签，一次性插入多条记录
     * 
     * @param files 文件列表
     */
    @org.apache.ibatis.annotations.Insert({
            "<script>",
            "INSERT INTO pnd_file (parent_id, file_name, type, resource_id, create_time, update_time)",
            "VALUES",
            "<foreach collection='files' item='file' separator=','>",
            "(#{file.parentId}, #{file.fileName}, #{file.type}, #{file.resourceId}, #{file.createTime}, #{file.updateTime})",
            "</foreach>",
            "</script>"
    })
    void batchSave(@Param("files") java.util.List<File> files);
}
