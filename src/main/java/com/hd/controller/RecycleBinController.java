package com.hd.controller;

import com.hd.biz.RecycleBinBiz;
import com.hd.common.HomeDashConstants;
import com.hd.model.dto.ResponseDto;
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

    @PostMapping("/list")
    public ResponseEntity<ResponseDto> list() {
        return ResponseEntity.ok(recycleBinBiz.list());
    }

    @PostMapping("/restore")
    public ResponseEntity<ResponseDto> restore(@RequestBody Map<String, List<Long>> params) {
        List<Long> fileIds = params.get("fileIds");
        return ResponseEntity.ok(recycleBinBiz.restore(fileIds));
    }

    @DeleteMapping("/empty")
    public ResponseEntity<ResponseDto> empty() {
        return ResponseEntity.ok(recycleBinBiz.empty());
    }
}
