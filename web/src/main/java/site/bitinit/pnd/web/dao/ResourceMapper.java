package site.bitinit.pnd.web.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import site.bitinit.pnd.web.entity.Resource;

/**
 * 资源数据访问接口。
 * 提供资源实体的数据库操作方法，包括保存、查询、删除和更新链接数等操作。
 * 使用MyBatis进行数据持久化。
 *
 * @author john
 * @date 2020-01-11
 */
@Mapper
public interface ResourceMapper {

    /**
     * 保存resource
     * 
     * @param resource resource
     * @return id
     */
    Long save(Resource resource);

    /**
     * 根据id获取Resource
     * 
     * @param id id
     * @return Resource
     */
    Resource findById(Long id);

    /**
     * 根据id获取Resource，并加锁
     * 
     * @param id id
     * @return Resource
     */
    Resource findByIdForUpdate(Long id);

    /**
     * 根据id删除resource
     * 
     * @param id id
     */
    void delete(Long id);

    /**
     * 更新link
     * 
     * @param id   id
     * @param link link
     */
    void updateLink(@Param("id") Long id, @Param("link") Integer link);

    /**
     * 根据MD5值查询资源。
     * 用于实现秒传功能，检查是否已存在相同MD5的文件。
     *
     * @param md5 MD5值
     * @return 资源对象，如果不存在则返回null
     */
    Resource findByMd5(@Param("md5") String md5);

    /**
     * 更新资源的MD5值。
     * 在文件上传完成后，将计算得到的MD5值更新到数据库。
     *
     * @param id  资源ID
     * @param md5 MD5值
     */
    @org.apache.ibatis.annotations.Update("UPDATE resource SET md5 = #{md5}, update_time = NOW() WHERE id = #{id}")
    void updateResourceMD5(@Param("id") Long id, @Param("md5") String md5);

    /**
     * 批量更新资源引用计数。
     * 用于批量复制文件时，一次性更新多个资源的引用计数，提高性能。
     * 
     * 性能优化：
     * - 使用批量更新，减少数据库操作次数
     * - 使用 CASE WHEN 语句，一次性更新多个资源
     * 
     * @param resourceIdList 资源ID列表
     * @param increment      增量值（正数表示增加，负数表示减少）
     */
    @org.apache.ibatis.annotations.Update({
            "<script>",
            "UPDATE resource SET link = link + #{increment} WHERE id IN",
            "<foreach collection='resourceIdList' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</script>"
    })
    void batchUpdateLink(@Param("resourceIdList") java.util.List<Long> resourceIdList,
            @Param("increment") Integer increment);

    /**
     * 批量查询资源。
     * 用于批量删除文件时，一次性查询多个资源信息，提高性能。
     * 
     * 性能优化：
     * - 使用批量查询，减少数据库操作次数
     * - 一次查询多个资源，避免循环查询
     * 
     * @param ids 资源ID列表
     * @return 资源列表
     */
    @org.apache.ibatis.annotations.Select({
            "<script>",
            "SELECT * FROM resource WHERE id IN",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</script>"
    })
    java.util.List<Resource> findByIds(@Param("ids") java.util.List<Long> ids);

    /**
     * 批量删除资源。
     * 用于批量删除文件时，一次性删除多个资源记录，提高性能。
     * 
     * 性能优化：
     * - 使用批量删除，减少数据库操作次数
     * - 一次删除多个资源，避免循环删除
     * 
     * @param ids 资源ID列表
     */
    @org.apache.ibatis.annotations.Delete({
            "<script>",
            "DELETE FROM resource WHERE id IN",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</script>"
    })
    void batchDelete(@Param("ids") java.util.List<Long> ids);
}
