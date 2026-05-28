package com.hd.dao.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hd.dao.entity.SysTask;
import com.hd.dao.mapper.SysTaskMapper;
import com.hd.dao.service.SysTaskDataService;
import org.springframework.stereotype.Service;

@Service
public class SysTaskDataServiceImpl extends ServiceImpl<SysTaskMapper, SysTask> implements SysTaskDataService {
}
