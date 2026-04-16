package com.hd.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.hd.entity.ResourceChunk;

/**
 * 资源分块数据访问接口。
 * 提供资源分块实体的数据库操作方法，用于支持大文件分块上传功能。
 * 使用MyBatis进行数据持久化。
 *
 * 支持的功能：
 * - 分块信息的增删改查
 * - 断点续传支持（通过identifier查询分块）
 * - 完整性校验支持（MD5值存储和更新）
 *
 * @author john
 * @date 2020-01-27
 */
@Mapper
public interface ResourceChunkMapper {

    /**
     * 根据identifier和chunkNumber查找块
     *
     * @param identifier  identifier
     * @param chunkNumber 当前块编号
     * @return ResourceChunk
     */
    ResourceChunk findByIdentifierAndChunkNumber(@Param("identifier") String identifier,
            @Param("chunkNumber") Integer chunkNumber);

    /**
     * 保存块
     *
     * @param resourceChunk chunk
     * @return id
     */
    Long save(ResourceChunk resourceChunk);

    /**
     * 删除文件块
     *
     * @param identifier identifier
     */
    void deleteChunk(@Param("identifier") String identifier);

    /**
     * 根据identifier查询所有已上传的分块
     * 用于断点续传功能，获取文件的所有已上传分块信息
     *
     * @param identifier 文件唯一标识符
     * @return 已上传的分块列表
     */
    java.util.List<ResourceChunk> findByIdentifier(@Param("identifier") String identifier);

    /**
     * 更新分块的MD5值和更新时间。
     * 用于断点续传场景下重新上传分块时更新完整性信息。
     *
     * @param identifier   文件唯一标识符
     * @param chunkNumber  分块编号
     * @param md5          新的MD5值
     * @return 更新的记录数
     */
    int updateChunkMd5(@Param("identifier") String identifier,
                       @Param("chunkNumber") Integer chunkNumber,
                       @Param("md5") String md5);
}
