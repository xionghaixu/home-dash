package com.hd.controller;

import com.hd.biz.RecycleBinBiz;
import com.hd.common.HomeDashConstants;
import com.hd.model.dto.ResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping(HomeDashConstants.API_VERSION + "/recycle-bin")
@RequiredArgsConstructor
public class RecycleBinController {

    private final RecycleBinBiz recycleBinBiz;

    @GetMapping("/list")
    public ResponseEntity<ResponseDTO> list() {
        return ResponseEntity.ok(recycleBinBiz.list());
    }

    @PostMapping("/restore")
    public ResponseEntity<ResponseDTO> restore(@RequestBody Map<String, List<Long>> params) {
        List<Long> fileIds = params.get("fileIds");
        if (fileIds == null || fileIds.isEmpty()) {
            return ResponseEntity.badRequest().body(ResponseDTO.fail("缺少fileIds参数"));
        }
        return ResponseEntity.ok(recycleBinBiz.restore(fileIds));
    }

    @PostMapping("/empty")
    public ResponseEntity<ResponseDTO> empty() {
        return ResponseEntity.ok(recycleBinBiz.empty());
    }
}
