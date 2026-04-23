package com.hd.dao.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hd.dao.entity.File;

/**
 * 文件数据访问服务。
 * 对外提供文件相关的持久化操作，内部委托Mapper实现。
 */
public interface FileDataService extends IService<File> {
    
}

