package com.hd.dao.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hd.dao.entity.Resource;
import com.hd.dao.mapper.ResourceMapper;
import com.hd.dao.service.ResourceDataService;
import org.springframework.stereotype.Service;

/**
 * @author xhx
 * @version 1.0
 * @package com.hd.dao.service.impl
 * @createTime 2026/04/23 23:34
 * @description 资源数据访问服务实现类。继承MyBatis-Plus的ServiceImpl，提供通用的CRUD功能。
 */
@Service
public class ResourceDataServiceImpl extends ServiceImpl<ResourceMapper, Resource> implements ResourceDataService {
    
}
