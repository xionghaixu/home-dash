package com.hd.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.hd.biz.SystemBiz;
import com.hd.common.HomeDashConstants;
import com.hd.model.dto.ResponseDTO;
import com.hd.model.dto.SystemInfoDTO;

/**
 * @author xhx
 * @version 1.0
 * @package com.hd.controller
 * @createTime 2026/04/16 18:36
 * @description 系统信息控制器。提供系统状态信息的查询接口，包括存储容量、文件统计等信息。
 */
@Slf4j
@RestController
@RequestMapping(HomeDashConstants.API_VERSION)
@RequiredArgsConstructor
public class SystemController {

    private final SystemBiz systemBiz;

    /**
     * 获取系统信息。
     * 返回系统存储容量、文件数量统计等信息。
     *
     * @return 包含系统信息的响应对象
     */
    @GetMapping({"/system", "/system/info"})
    public ResponseEntity<ResponseDTO> getSystemInfo() {
        log.info("收到获取系统信息请求");
        SystemInfoDTO systemInfo = systemBiz.systemInfo();
        log.info("系统信息查询成功 [totalFiles={}, totalCap={}, usableCap={}]",
                systemInfo.getTotalNum(), systemInfo.getTotalCap(), systemInfo.getUsableCap());
        return ResponseEntity.ok(ResponseDTO.success(systemInfo));
    }
}
