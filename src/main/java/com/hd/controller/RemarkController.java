package com.hd.controller;

import com.hd.biz.RemarkBiz;
import com.hd.common.HomeDashConstants;
import com.hd.model.dto.ResponseDto;
import com.hd.model.vo.FileRemarkVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 文件备注控制器。
 * 提供文件备注的REST API接口。
 *
 * @author team-lead
 * @version 1.0
 * @createTime 2026/04/25
 */
@Slf4j
@RestController
@RequestMapping(HomeDashConstants.API_VERSION)
@RequiredArgsConstructor
public class RemarkController {

    private final RemarkBiz remarkBiz;

    /**
     * 获取文件备注。
     *
     * @param resourceId 资源ID
     * @return 文件备注
     */
    @GetMapping("/remark/{resourceId}")
    public ResponseEntity<ResponseDto> getFileRemark(@PathVariable Long resourceId) {
        log.info("获取文件备注请求 [resourceId={}]", resourceId);
        FileRemarkVo remark = remarkBiz.getFileRemark(resourceId);
        return ResponseEntity.ok(ResponseDto.success(remark));
    }

    /**
     * 保存或更新文件备注。
     *
     * @param resourceId 资源ID
     * @param content 备注内容
     * @return 文件备注
     */
    @PostMapping("/remark/{resourceId}")
    public ResponseEntity<ResponseDto> saveFileRemark(
            @PathVariable Long resourceId,
            @RequestBody RemarkRequest request) {
        log.info("保存文件备注请求 [resourceId={}, content={}]", resourceId, request.getContent());
        FileRemarkVo remark = remarkBiz.saveFileRemark(resourceId, request.getContent());
        return ResponseEntity.ok(ResponseDto.success(remark));
    }

    /**
     * 删除文件备注。
     *
     * @param resourceId 资源ID
     * @return 操作结果
     */
    @DeleteMapping("/remark/{resourceId}")
    public ResponseEntity<ResponseDto> deleteFileRemark(@PathVariable Long resourceId) {
        log.info("删除文件备注请求 [resourceId={}]", resourceId);
        boolean result = remarkBiz.deleteFileRemark(resourceId);
        return ResponseEntity.ok(ResponseDto.success(result));
    }

    /**
     * 批量获取文件备注。
     *
     * @param resourceIds 资源ID列表
     * @return 文件备注列表
     */
    @PostMapping("/remark/batch")
    public ResponseEntity<ResponseDto> getFileRemarks(@RequestBody List<Long> resourceIds) {
        log.info("批量获取文件备注请求 [resourceIds={}]", resourceIds);
        List<FileRemarkVo> remarks = remarkBiz.getFileRemarks(resourceIds);
        return ResponseEntity.ok(ResponseDto.success(remarks));
    }

    @lombok.Data
    public static class RemarkRequest {
        private String content;
    }
}
