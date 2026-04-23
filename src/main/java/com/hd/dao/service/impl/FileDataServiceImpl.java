package com.hd.dao.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hd.dao.entity.File;
import com.hd.dao.mapper.FileMapper;
import com.hd.dao.service.FileDataService;
import org.springframework.stereotype.Service;

/**
 * 文件数据访问服务实现类。
 * 继承MyBatis-Plus的ServiceImpl，提供通用的CRUD功能。
 */
@Service
public class FileDataServiceImpl extends ServiceImpl<FileMapper, File> implements FileDataService {
    
}

