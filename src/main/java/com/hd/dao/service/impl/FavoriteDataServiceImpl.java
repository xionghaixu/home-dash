package com.hd.dao.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hd.dao.entity.Favorite;
import com.hd.dao.mapper.FavoriteMapper;
import com.hd.dao.service.FavoriteDataService;
import org.springframework.stereotype.Service;

/**
 * 收藏数据访问服务实现类。
 * 继承MyBatis-Plus的ServiceImpl，提供通用的CRUD功能。
 *
 * @author team-lead
 * @version 1.0
 * @createTime 2026/04/25
 */
@Service
public class FavoriteDataServiceImpl extends ServiceImpl<FavoriteMapper, Favorite> implements FavoriteDataService {

}
