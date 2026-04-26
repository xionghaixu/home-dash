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
 * @author team-lead
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
    public BatchOperationResultVo batchAddFavorite(List<Long> resourceIds) {
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

        return BatchOperationResultVo.builder()
                .successCount(successCount)
                .failCount(failCount)
                .errors(errors)
                .build();
    }

    @Override
    @Transactional
    public BatchOperationResultVo batchRemoveFavorite(List<Long> resourceIds) {
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

        return BatchOperationResultVo.builder()
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
    public IPage<FileDetailVo> getFavoriteList(Integer page, Integer pageSize) {
        log.info("获取收藏列表 [page={}, pageSize={}]", page, pageSize);

        int pageNum = page != null ? page : 1;
        int size = pageSize != null ? pageSize : 20;
        Page<Favorite> pageParam = new Page<>(pageNum, size);

        IPage<Favorite> favoritePage = favoriteDataService.lambdaQuery()
                .orderByDesc(Favorite::getCreatedAt)
                .page(pageParam);

        List<FileDetailVo> fileDetails = favoritePage.getRecords().stream()
                .map(fav -> {
                    File file = findFileByResourceId(fav.getResourceId());
                    return file != null ? convertToFileDetailVo(file, true) : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        IPage<FileDetailVo> resultPage = new Page<>(pageNum, size, favoritePage.getTotal());
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
                .list()
                .stream().findFirst().orElse(null);
    }

    private FileDetailVo convertToFileDetailVo(File file, boolean isFavorite) {
        return FileDetailVo.builder()
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