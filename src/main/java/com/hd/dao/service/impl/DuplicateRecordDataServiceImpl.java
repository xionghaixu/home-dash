package com.hd.dao.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hd.dao.entity.DuplicateRecord;
import com.hd.dao.mapper.DuplicateRecordMapper;
import com.hd.dao.service.DuplicateRecordDataService;
import org.springframework.stereotype.Service;

@Service
public class DuplicateRecordDataServiceImpl extends ServiceImpl<DuplicateRecordMapper, DuplicateRecord> implements DuplicateRecordDataService {
}
