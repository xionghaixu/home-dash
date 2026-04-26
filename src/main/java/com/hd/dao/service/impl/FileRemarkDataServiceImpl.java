package com.hd.dao.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hd.dao.entity.FileRemark;
import com.hd.dao.mapper.FileRemarkMapper;
import com.hd.dao.service.FileRemarkDataService;
import org.springframework.stereotype.Service;

/**
 * 文件备注数据访问服务实现类。
 * 继承MyBatis-Plus的ServiceImpl，提供通用的CRUD功能。
 *
 * @author team-lead
 * @version 1.0
 * @createTime 2026/04/25
 */
@Service
public class FileRemarkDataServiceImpl extends ServiceImpl<FileRemarkMapper, FileRemark> implements FileRemarkDataService {

}
