package com.hd.controller;

import com.hd.biz.RemarkBiz;
import com.hd.common.HomeDashConstants;
import com.hd.model.dto.ResponseDTO;
import com.hd.model.vo.FileRemarkVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 文件备注控制器。
 * 提供文件备注的REST API接口。
 *
 * @author xhx
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
    public ResponseEntity<ResponseDTO> getFileRemark(@PathVariable Long resourceId) {
        log.info("获取文件备注请求 [resourceId={}]", resourceId);
        FileRemarkVO remark = remarkBiz.getFileRemark(resourceId);
        return ResponseEntity.ok(ResponseDTO.success(remark));
    }

    /**
     * 保存或更新文件备注。
     *
     * @param resourceId 资源ID
     * @param content 备注内容
     * @return 文件备注
     */
    @PostMapping("/remark/{resourceId}/save")
    public ResponseEntity<ResponseDTO> saveFileRemark(
            @PathVariable Long resourceId,
            @RequestBody RemarkRequest request) {
        log.info("保存文件备注请求 [resourceId={}, content={}]", resourceId, request.getContent());
        FileRemarkVO remark = remarkBiz.saveFileRemark(resourceId, request.getContent());
        return ResponseEntity.ok(ResponseDTO.success(remark));
    }

    /**
     * 删除文件备注。
     *
     * @param resourceId 资源ID
     * @return 操作结果
     */
    @PostMapping("/remark/{resourceId}/delete")
    public ResponseEntity<ResponseDTO> deleteFileRemark(@PathVariable Long resourceId) {
        log.info("删除文件备注请求 [resourceId={}]", resourceId);
        boolean result = remarkBiz.deleteFileRemark(resourceId);
        return ResponseEntity.ok(ResponseDTO.success(result));
    }

    /**
     * 批量删除文件备注。
     *
     * @param resourceIds 资源ID列表
     * @return 删除数量
     */
    @PostMapping("/remark/batch/delete")
    public ResponseEntity<ResponseDTO> batchDeleteFileRemarks(@RequestBody List<Long> resourceIds) {
        log.info("批量删除文件备注请求 [resourceIds={}]", resourceIds);
        int count = remarkBiz.batchDeleteFileRemarks(resourceIds);
        return ResponseEntity.ok(ResponseDTO.success(count));
    }

    /**
     * 批量获取文件备注。
     *
     * @param resourceIds 资源ID列表
     * @return 文件备注列表
     */
    @GetMapping("/remark/batch")
    public ResponseEntity<ResponseDTO> getFileRemarks(@RequestBody List<Long> resourceIds) {
        log.info("批量获取文件备注请求 [resourceIds={}]", resourceIds);
        List<FileRemarkVO> remarks = remarkBiz.getFileRemarks(resourceIds);
        return ResponseEntity.ok(ResponseDTO.success(remarks));
    }

    @lombok.Data
    public static class RemarkRequest {
        private String content;
    }
}
