package com.hd.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.hd.Constants;
import com.hd.controller.dto.ResponseDto;
import com.hd.controller.dto.SystemInfoDto;
import com.hd.service.SystemService;

/**
 * 系统信息控制器。
 * 提供系统状态信息的查询接口，包括存储容量、文件统计等信息。
 *
 * @author john
 * @date 2020-02-10
 */
@Slf4j
@RestController
@RequestMapping(Constants.API_VERSION)
public class SystemController {

    private final SystemService systemService;

    /**
     * 构造函数，注入SystemService依赖。
     *
     * @param systemService 系统服务接口
     */
    @Autowired
    public SystemController(SystemService systemService) {
        this.systemService = systemService;
    }

    /**
     * 获取系统信息。
     * 返回系统存储容量、文件数量统计等信息。
     *
     * @return 包含系统信息的响应对象
     */
    @GetMapping({"/system", "/system/info"})
    public ResponseEntity<ResponseDto> getSystemInfo() {
        log.info("收到获取系统信息请求");
        SystemInfoDto systemInfo = systemService.systemInfo();
        log.info("系统信息查询成功 [totalFiles={}, totalCap={}, usableCap={}]",
                systemInfo.getTotalNum(), systemInfo.getTotalCap(), systemInfo.getUsableCap());
        return ResponseEntity.ok(ResponseDto.success(systemInfo));
    }
}
