package com.hd.dao.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hd.dao.entity.FileTagRelation;
import com.hd.dao.mapper.FileTagRelationMapper;
import com.hd.dao.service.FileTagRelationDataService;
import org.springframework.stereotype.Service;

/**
 * 文件标签关联数据访问服务实现类。
 * 继承MyBatis-Plus的ServiceImpl，提供通用的CRUD功能。
 *
 * @author team-lead
 * @version 1.0
 * @createTime 2026/04/25
 */
@Service
public class FileTagRelationDataServiceImpl extends ServiceImpl<FileTagRelationMapper, FileTagRelation> implements FileTagRelationDataService {

}
