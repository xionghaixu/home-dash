package com.hd.controller;

import com.hd.biz.GovernanceBiz;
import com.hd.common.HomeDashConstants;
import com.hd.model.dto.ResponseDTO;
import com.hd.model.dto.StorageAnalysisDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping(HomeDashConstants.API_VERSION + "/governance")
@RequiredArgsConstructor
public class GovernanceController {

    private final GovernanceBiz governanceBiz;

    @GetMapping("/storage-analysis")
    public ResponseEntity<ResponseDTO> getStorageAnalysis() {
        StorageAnalysisDTO analysis = governanceBiz.getStorageAnalysis();
        return ResponseEntity.ok(ResponseDTO.success(analysis));
    }

    @GetMapping("/duplicates")
    public ResponseEntity<ResponseDTO> getDuplicates() {
        return ResponseEntity.ok(ResponseDTO.success(governanceBiz.scanAndGetDuplicates()));
    }

    @GetMapping("/large-files")
    public ResponseEntity<ResponseDTO> getLargeFiles() {
        return ResponseEntity.ok(ResponseDTO.success(governanceBiz.getLargeFiles()));
    }

    @GetMapping("/empty-dirs")
    public ResponseEntity<ResponseDTO> getEmptyDirs() {
        return ResponseEntity.ok(ResponseDTO.success(governanceBiz.getEmptyDirectories()));
    }

    @PostMapping("/duplicates/smart-cleanup")
    public ResponseEntity<ResponseDTO> smartCleanup() {
        governanceBiz.smartCleanup();
        return ResponseEntity.ok(ResponseDTO.success());
    }

    @PostMapping("/duplicates/cleanup")
    public ResponseEntity<ResponseDTO> cleanup(@RequestParam Long groupId) {
        governanceBiz.cleanupGroup(groupId);
        return ResponseEntity.ok(ResponseDTO.success());
    }
}
