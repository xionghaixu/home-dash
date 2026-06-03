package com.hd.dao.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hd.dao.entity.File;
import com.hd.dao.mapper.FileMapper;
import com.hd.dao.service.FileDataService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author xhx
 * @version 1.0
 * @package com.hd.dao.service.impl
 * @createTime 2026/04/23 23:34
 * @description 文件数据访问服务实现类。继承MyBatis-Plus的ServiceImpl，提供通用的CRUD功能。
 */
@Service
public class FileDataServiceImpl extends ServiceImpl<FileMapper, File> implements FileDataService {

    @Override
    public File selectByIdWithDeleted(Long id) {
        return getBaseMapper().selectByIdWithDeleted(id);
    }

    @Override
    public void permanentlyDelete(Long id) {
        getBaseMapper().permanentlyDelete(id);
    }

    @Override
    public List<File> selectAllChildrenByParentId(Long parentId) {
        return getBaseMapper().selectAllChildrenByParentId(parentId);
    }

    @Override
    public List<File> selectDeletedFilesByParentId(Long parentId) {
        return getBaseMapper().selectDeletedFilesByParentId(parentId);
    }

    @Override
    public void restoreFile(Long id, Long parentId) {
        getBaseMapper().restoreFile(id, parentId);
    }
}

