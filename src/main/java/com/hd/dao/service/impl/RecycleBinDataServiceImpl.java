package com.hd.dao.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hd.dao.entity.RecycleBin;
import com.hd.dao.mapper.RecycleBinMapper;
import com.hd.dao.service.RecycleBinDataService;
import org.springframework.stereotype.Service;

@Service
public class RecycleBinDataServiceImpl extends ServiceImpl<RecycleBinMapper, RecycleBin> implements RecycleBinDataService {
}
