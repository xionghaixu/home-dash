package com.hd.dao.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hd.dao.entity.ResourceChunk;
import com.hd.dao.mapper.ResourceChunkMapper;
import com.hd.dao.service.ResourceChunkDataService;
import org.springframework.stereotype.Service;

/**
 * 资源分块数据访问服务实现类。
 * 继承MyBatis-Plus的ServiceImpl，提供通用的CRUD功能。
 */
@Service
public class ResourceChunkDataServiceImpl extends ServiceImpl<ResourceChunkMapper, ResourceChunk> implements ResourceChunkDataService {
    
}

