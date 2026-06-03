package com.hd.biz.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hd.biz.FavoriteBiz;
import com.hd.dao.entity.*;
import com.hd.dao.service.*;
import com.hd.model.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 收藏业务实现类。
 *
 * @author xhx
 * @version 1.0
 * @createTime 2026/04/25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteBizImpl implements FavoriteBiz {

    private final FavoriteDataService favoriteDataService;
    private final FileDataService fileDataService;
    private final ResourceDataService resourceDataService;

    @Override
    @Transactional
    public boolean addFavorite(Long resourceId) {
        log.info("添加收藏 [resourceId={}]", resourceId);

        if (isFavorite(resourceId)) {
            log.info("资源已收藏 [resourceId={}]", resourceId);
            return true;
        }

        Favorite favorite = Favorite.builder()
                .resourceId(resourceId)
                .createdAt(new Date())
                .build();

        boolean result = favoriteDataService.save(favorite);
        log.info("添加收藏完成 [resourceId={}, result={}]", resourceId, result);
        return result;
    }

    @Override
    @Transactional
    public boolean removeFavorite(Long resourceId) {
        log.info("取消收藏 [resourceId={}]", resourceId);

        boolean result = favoriteDataService.lambdaUpdate()
                .eq(Favorite::getResourceId, resourceId)
                .remove();
        log.info("取消收藏完成 [resourceId={}, result={}]", resourceId, result);
        return result;
    }

    @Override
    @Transactional
    public BatchOperationResultVO batchAddFavorite(List<Long> resourceIds) {
        log.info("批量添加收藏 [count={}]", resourceIds.size());

        List<String> errors = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        for (Long resourceId : resourceIds) {
            try {
                if (addFavorite(resourceId)) {
                    successCount++;
                } else {
                    failCount++;
                    errors.add("添加收藏失败: resourceId=" + resourceId);
                }
            } catch (Exception e) {
                failCount++;
                errors.add("添加收藏异常: resourceId=" + resourceId + ", error=" + e.getMessage());
            }
        }

        return BatchOperationResultVO.builder()
                .successCount(successCount)
                .failCount(failCount)
                .errors(errors)
                .build();
    }

    @Override
    @Transactional
    public BatchOperationResultVO batchRemoveFavorite(List<Long> resourceIds) {
        log.info("批量取消收藏 [count={}]", resourceIds.size());

        List<String> errors = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        for (Long resourceId : resourceIds) {
            try {
                if (removeFavorite(resourceId)) {
                    successCount++;
                } else {
                    failCount++;
                    errors.add("取消收藏失败: resourceId=" + resourceId);
                }
            } catch (Exception e) {
                failCount++;
                errors.add("取消收藏异常: resourceId=" + resourceId + ", error=" + e.getMessage());
            }
        }

        return BatchOperationResultVO.builder()
                .successCount(successCount)
                .failCount(failCount)
                .errors(errors)
                .build();
    }

    @Override
    public boolean isFavorite(Long resourceId) {
        return favoriteDataService.lambdaQuery()
                .eq(Favorite::getResourceId, resourceId)
                .count() > 0;
    }

    @Override
    public IPage<FileDetailVO> getFavoriteList(Integer page, Integer pageSize) {
        log.info("获取收藏列表 [page={}, pageSize={}]", page, pageSize);

        int pageNum = page != null ? page : 1;
        int size = pageSize != null ? pageSize : 20;
        Page<Favorite> pageParam = new Page<>(pageNum, size);

        IPage<Favorite> favoritePage = favoriteDataService.lambdaQuery()
                .orderByDesc(Favorite::getCreatedAt)
                .page(pageParam);

        // 批量加载当前页所有收藏对应的文件，避免 N+1 查询
        List<Long> resourceIds = favoritePage.getRecords().stream()
                .map(Favorite::getResourceId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, File> fileMap = Collections.emptyMap();
        if (!resourceIds.isEmpty()) {
            fileMap = fileDataService.lambdaQuery()
                    .in(File::getResourceId, resourceIds)
                    .list()
                    .stream()
                    .collect(Collectors.toMap(File::getResourceId, f -> f, (a, b) -> a));
        }

        Map<Long, File> finalFileMap = fileMap;
        List<FileDetailVO> fileDetails = favoritePage.getRecords().stream()
                .map(fav -> {
                    File file = finalFileMap.get(fav.getResourceId());
                    return file != null ? convertToFileDetailVO(file, true) : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 使用过滤后的实际数量作为总数，避免孤立收藏记录导致总数虚高
        long total = fileDetails.size() < favoritePage.getRecords().size()
                ? fileDetails.size() + (long) (pageNum - 1) * size
                : favoritePage.getTotal();

        IPage<FileDetailVO> resultPage = new Page<>(pageNum, size, total);
        resultPage.setRecords(fileDetails);

        return resultPage;
    }

    @Override
    public Long getFavoriteCount() {
        return favoriteDataService.count();
    }

    private File findFileByResourceId(Long resourceId) {
        return fileDataService.lambdaQuery()
                .eq(File::getResourceId, resourceId)
                .last("LIMIT 1")
                .one();
    }

    private FileDetailVO convertToFileDetailVO(File file, boolean isFavorite) {
        return FileDetailVO.builder()
                .fileId(file.getId())
                .fileName(file.getFileName())
                .type(file.getType())
                .size(file.getSize())
                .parentId(file.getParentId())
                .resourceId(file.getResourceId())
                .updateTime(file.getUpdateTime() != null ? file.getUpdateTime().toString() : null)
                .isFavorite(isFavorite)
                .build();
    }
}