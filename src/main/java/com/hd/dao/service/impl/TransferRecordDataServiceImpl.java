package com.hd.dao.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hd.dao.entity.TransferRecord;
import com.hd.dao.mapper.TransferRecordMapper;
import com.hd.dao.service.TransferRecordDataService;
import org.springframework.stereotype.Service;

@Service
public class TransferRecordDataServiceImpl extends ServiceImpl<TransferRecordMapper, TransferRecord> implements TransferRecordDataService {
}
