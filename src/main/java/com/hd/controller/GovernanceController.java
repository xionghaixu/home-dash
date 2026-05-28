package com.hd.controller;

import com.hd.biz.GovernanceBiz;
import com.hd.common.HomeDashConstants;
import com.hd.model.dto.ResponseDto;
import com.hd.model.dto.StorageAnalysisDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(HomeDashConstants.API_VERSION + "/governance")
public class GovernanceController {

    private final GovernanceBiz governanceBiz;

    @Autowired
    public GovernanceController(GovernanceBiz governanceBiz) {
        this.governanceBiz = governanceBiz;
    }

    @GetMapping("/storage-analysis")
    public ResponseEntity<ResponseDto> getStorageAnalysis() {
        StorageAnalysisDto analysis = governanceBiz.getStorageAnalysis();
        return ResponseEntity.ok(ResponseDto.success(analysis));
    }

    @GetMapping("/duplicates")
    public ResponseEntity<ResponseDto> getDuplicates() {
        return ResponseEntity.ok(ResponseDto.success(governanceBiz.scanAndGetDuplicates()));
    }

    @GetMapping("/large-files")
    public ResponseEntity<ResponseDto> getLargeFiles() {
        return ResponseEntity.ok(ResponseDto.success(governanceBiz.getLargeFiles()));
    }

    @GetMapping("/empty-dirs")
    public ResponseEntity<ResponseDto> getEmptyDirs() {
        return ResponseEntity.ok(ResponseDto.success(governanceBiz.getEmptyDirectories()));
    }

    @PostMapping("/duplicates/smart-cleanup")
    public ResponseEntity<ResponseDto> smartCleanup() {
        governanceBiz.smartCleanup();
        return ResponseEntity.ok(ResponseDto.success());
    }

    @PostMapping("/duplicates/cleanup")
    public ResponseEntity<ResponseDto> cleanup(@RequestBody java.util.Map<String, Object> params) {
        if (params != null && params.containsKey("groupId")) {
            Long groupId = Long.valueOf(params.get("groupId").toString());
            governanceBiz.cleanupGroup(groupId);
        }
        return ResponseEntity.ok(ResponseDto.success());
    }
}
