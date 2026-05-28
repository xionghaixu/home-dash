package com.hd.dao.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hd.dao.entity.DuplicateGroup;
import com.hd.dao.mapper.DuplicateGroupMapper;
import com.hd.dao.service.DuplicateGroupDataService;
import org.springframework.stereotype.Service;

@Service
public class DuplicateGroupDataServiceImpl extends ServiceImpl<DuplicateGroupMapper, DuplicateGroup> implements DuplicateGroupDataService {
}
