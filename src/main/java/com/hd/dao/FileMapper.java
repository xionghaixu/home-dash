package com.hd.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.hd.entity.File;

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
     * 获取所有文件（不含目录筛选）。
     *
     * @param sortBy    排序字段
     * @param sortOrder 排序方式
     * @return 文件列表
     */
    List<File> findAll(@Param("sortBy") String sortBy, @Param("sortOrder") String sortOrder);

    /**
     * 获取最近上传文件列表。
     *
     * @param limit 数量限制
     * @return 最近上传文件
     */
    List<File> findRecentFiles(@Param("limit") Integer limit);

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
     * 检查同目录下是否已存在同名文件。
     *
     * @param parentId  父目录ID
     * @param fileName  文件名
     * @param excludeId 排除的文件ID
     * @return 数量
     */
    Integer countByParentIdAndFileName(@Param("parentId") Long parentId,
                                       @Param("fileName") String fileName,
                                       @Param("excludeId") Long excludeId);

    /**
     * 根据父目录ID和文件名查找文件。
     * 用于获取冲突文件详情。
     *
     * @param parentId 父目录ID
     * @param fileName 文件名
     * @return 文件对象，如果不存在则返回null
     */
    File findByParentIdAndFileName(@Param("parentId") Long parentId,
                                   @Param("fileName") String fileName);

    /**
     * 根据创建时间范围查询文件。
     * 用于统计最近上传文件。
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param sortBy    排序字段
     * @param sortOrder 排序方式
     * @return 文件列表
     */
    List<File> findByCreateTimeBetween(@Param("startTime") java.util.Date startTime,
                                       @Param("endTime") java.util.Date endTime,
                                       @Param("sortBy") String sortBy,
                                       @Param("sortOrder") String sortOrder);

    /**
     * 根据创建时间范围统计文件数量。
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 文件数量
     */
    Integer countByCreateTimeBetween(@Param("startTime") java.util.Date startTime,
                                    @Param("endTime") java.util.Date endTime);

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
            "INSERT INTO file (parent_id, file_name, type, resource_id, create_time, update_time)",
            "VALUES",
            "<foreach collection='files' item='file' separator=','>",
            "(#{file.parentId}, #{file.fileName}, #{file.type}, #{file.resourceId}, #{file.createTime}, #{file.updateTime})",
            "</foreach>",
            "</script>"
    })
    void batchSave(@Param("files") java.util.List<File> files);
}
