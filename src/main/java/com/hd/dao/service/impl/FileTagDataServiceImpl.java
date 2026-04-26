package com.hd.dao.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hd.dao.entity.FileTag;
import com.hd.dao.mapper.FileTagMapper;
import com.hd.dao.service.FileTagDataService;
import org.springframework.stereotype.Service;

/**
 * 文件标签数据访问服务实现类。
 * 继承MyBatis-Plus的ServiceImpl，提供通用的CRUD功能。
 *
 * @author team-lead
 * @version 1.0
 * @createTime 2026/04/25
 */
@Service
public class FileTagDataServiceImpl extends ServiceImpl<FileTagMapper, FileTag> implements FileTagDataService {

}
