package com.hd.service;

import com.hd.controller.dto.SystemInfoDto;

/**
 * 系统服务接口。
 * 定义系统信息查询的核心业务操作。
 *
 * @author john
 * @date 2020-02-10
 */
public interface SystemService {

    /**
     * 系统信息
     * 
     * @return SystemInfoDto
     */
    SystemInfoDto systemInfo();
}
