package com.hd.dao.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hd.dao.entity.SearchHistory;
import com.hd.dao.mapper.SearchHistoryMapper;
import com.hd.dao.service.SearchHistoryDataService;
import org.springframework.stereotype.Service;

/**
 * 搜索历史数据访问服务实现类。
 * 继承MyBatis-Plus的ServiceImpl，提供通用的CRUD功能。
 *
 * @author team-lead
 * @version 1.0
 * @createTime 2026/04/25
 */
@Service
public class SearchHistoryDataServiceImpl extends ServiceImpl<SearchHistoryMapper, SearchHistory> implements SearchHistoryDataService {

}
