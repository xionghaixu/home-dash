package com.hd.controller;

import com.hd.biz.FavoriteBiz;
import com.hd.common.HomeDashConstants;
import com.hd.model.dto.ResponseDto;
import com.hd.model.vo.BatchOperationResultVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 收藏控制器。
 * 提供文件收藏的REST API接口。
 *
 * @author team-lead
 * @version 1.0
 * @createTime 2026/04/25
 */
@Slf4j
@RestController
@RequestMapping(HomeDashConstants.API_VERSION)
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteBiz favoriteBiz;

    /**
     * 添加收藏。
     */
    @PostMapping("/favorite/{resourceId}")
    public ResponseEntity<ResponseDto> addFavorite(@PathVariable Long resourceId) {
        log.info("添加收藏请求 [resourceId={}]", resourceId);
        boolean result = favoriteBiz.addFavorite(resourceId);
        return ResponseEntity.ok(ResponseDto.success(result));
    }

    /**
     * 取消收藏。
     */
    @DeleteMapping("/favorite/{resourceId}")
    public ResponseEntity<ResponseDto> removeFavorite(@PathVariable Long resourceId) {
        log.info("取消收藏请求 [resourceId={}]", resourceId);
        boolean result = favoriteBiz.removeFavorite(resourceId);
        return ResponseEntity.ok(ResponseDto.success(result));
    }

    /**
     * 批量添加收藏。
     */
    @PostMapping("/favorite/batch/add")
    public ResponseEntity<ResponseDto> batchAddFavorite(@RequestBody List<Long> resourceIds) {
        log.info("批量添加收藏请求 [count={}]", resourceIds.size());
        BatchOperationResultVo result = favoriteBiz.batchAddFavorite(resourceIds);
        return ResponseEntity.ok(ResponseDto.success(result));
    }

    /**
     * 批量取消收藏。
     */
    @PostMapping("/favorite/batch/remove")
    public ResponseEntity<ResponseDto> batchRemoveFavorite(@RequestBody List<Long> resourceIds) {
        log.info("批量取消收藏请求 [count={}]", resourceIds.size());
        BatchOperationResultVo result = favoriteBiz.batchRemoveFavorite(resourceIds);
        return ResponseEntity.ok(ResponseDto.success(result));
    }

    /**
     * 检查收藏状态。
     */
    @GetMapping("/favorite/status/{resourceId}")
    public ResponseEntity<ResponseDto> isFavorite(@PathVariable Long resourceId) {
        log.info("检查收藏状态 [resourceId={}]", resourceId);
        boolean result = favoriteBiz.isFavorite(resourceId);
        return ResponseEntity.ok(ResponseDto.success(result));
    }

    /**
     * 获取收藏列表。
     */
    @GetMapping("/favorite/list")
    public ResponseEntity<ResponseDto> getFavoriteList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        log.info("获取收藏列表 [page={}, pageSize={}]", page, pageSize);
        return ResponseEntity.ok(ResponseDto.success(favoriteBiz.getFavoriteList(page, pageSize)));
    }

    /**
     * 获取收藏数量。
     */
    @GetMapping("/favorite/count")
    public ResponseEntity<ResponseDto> getFavoriteCount() {
        log.info("获取收藏数量请求");
        return ResponseEntity.ok(ResponseDto.success(favoriteBiz.getFavoriteCount()));
    }
}