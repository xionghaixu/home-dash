package com.hd.biz.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hd.biz.PictureMediaBiz;
import com.hd.common.enums.ErrorCodeEnum;
import com.hd.common.enums.FileTypeEnum;
import com.hd.common.exception.BusinessException;
import com.hd.common.exception.DataNotFoundException;
import com.hd.dao.entity.*;
import com.hd.dao.service.*;
import com.hd.model.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 图片媒体业务实现类
 *
 * @author xhx
 * @since 2026-04-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PictureMediaBizImpl implements PictureMediaBiz {

    private final FileDataService fileDataService;
    private final MediaPictureMetadataDataService pictureMetadataDataService;
    private final MediaAlbumDataService albumDataService;
    private final MediaAlbumItemDataService albumItemDataService;

    @Override
    public PageResponseDTO<PictureListDTO> getPictureList(Integer page, Integer pageSize, String sortBy, String sortOrder,
                                                           Integer year, Integer month, Long albumId, String directory) {
        int current = page != null && page > 0 ? page : 1;
        int size = pageSize != null && pageSize > 0 ? pageSize : 40;

        LambdaQueryWrapper<File> wrapper = buildPictureQuery(year, month, albumId, directory);
        applyPictureSort(wrapper, sortBy, sortOrder);

        Page<File> pageResult = fileDataService.page(new Page<>(current, size), wrapper);
        Map<Long, MediaPictureMetadata> metadataMap = batchLoadPictureMetadata(pageResult.getRecords());
        List<PictureListDTO> list = pageResult.getRecords().stream()
                .map(f -> convertToPictureListDTO(f, metadataMap))
                .collect(Collectors.toList());
        return PageResponseDTO.of(list, pageResult.getTotal(), current, size);
    }

    @Override
    public PictureDetailDTO getPictureDetail(Long fileId) {
        File file = fileDataService.getById(fileId);
        if (file == null || !FileTypeEnum.PICTURE.toString().equals(file.getType())) {
            throw new DataNotFoundException(String.format("图片不存在 [fileId=%d]", fileId));
        }

        MediaPictureMetadata metadata = pictureMetadataDataService.lambdaQuery()
                .eq(MediaPictureMetadata::getFileId, fileId)
                .one();

        PictureDetailDTO.PictureDetailDTOBuilder builder = PictureDetailDTO.builder()
                .fileId(fileId)
                .fileName(file.getFileName())
                .size(file.getSize())
                .thumbnailUrls(getThumbnailUrls(file))
                .originalUrl(buildPictureOriginalUrl(file.getResourceId()));

        if (metadata != null) {
            builder.width(metadata.getWidth())
                    .height(metadata.getHeight())
                    .takenAt(metadata.getTakenAt())
                    .locationName(metadata.getLocationName())
                    .cameraMake(metadata.getCameraMake())
                    .cameraModel(metadata.getCameraModel())
                    .lensModel(metadata.getLensModel())
                    .focalLength(metadata.getFocalLength())
                    .aperture(metadata.getAperture())
                    .exposureTime(metadata.getExposureTime())
                    .iso(metadata.getIso())
                    .gpsLatitude(metadata.getGpsLatitude())
                    .gpsLongitude(metadata.getGpsLongitude())
                    .orientation(metadata.getOrientation())
                    .colorMode(metadata.getColorMode());
        }

        return builder.build();
    }

    @Override
    public List<PictureTimelineGroupDTO> getPictureTimeline(String groupBy) {
        String normalizedGroupBy = groupBy != null ? groupBy.toLowerCase() : "month";
        List<MediaPictureMetadata> metadataList = pictureMetadataDataService.lambdaQuery()
                .isNotNull(MediaPictureMetadata::getTakenAt)
                .orderByDesc(MediaPictureMetadata::getTakenAt)
                .list();

        Map<String, List<MediaPictureMetadata>> groups;
        switch (normalizedGroupBy) {
            case "year" -> groups = metadataList.stream()
                    .collect(Collectors.groupingBy(m -> String.valueOf(m.getTakenAt().getYear())));
            case "day" -> groups = metadataList.stream()
                    .collect(Collectors.groupingBy(m -> m.getTakenAt().toLocalDate().toString()));
            default -> groups = metadataList.stream()
                    .collect(Collectors.groupingBy(m -> m.getTakenAt().getYear() + "-" + m.getTakenAt().getMonthValue()));
        }

        return groups.entrySet().stream().map(entry -> {
            List<MediaPictureMetadata> list = entry.getValue();
            MediaPictureMetadata first = list.get(0);
            return PictureTimelineGroupDTO.builder()
                    .year(first.getTakenAt().getYear())
                    .month("month".equals(normalizedGroupBy) || "day".equals(normalizedGroupBy) ? first.getTakenAt().getMonthValue() : null)
                    .day("day".equals(normalizedGroupBy) ? first.getTakenAt().getDayOfMonth() : null)
                    .count((long) list.size())
                    .coverFileId(first.getFileId())
                    .coverThumbnail(buildPictureTimelineCover(first.getFileId()))
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public List<AlbumDTO> getAlbumList(String albumType) {
        LambdaQueryWrapper<MediaAlbum> wrapper = new LambdaQueryWrapper<>();
        if (albumType != null && !albumType.isEmpty()) {
            wrapper.eq(MediaAlbum::getAlbumType, albumType.toUpperCase());
        }
        return albumDataService.list(wrapper).stream().map(this::convertToAlbumDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AlbumDTO createAlbum(String albumName, String description, Long coverFileId) {
        MediaAlbum album = new MediaAlbum();
        album.setAlbumName(albumName);
        album.setDescription(description);
        album.setCoverFileId(coverFileId);
        album.setAlbumType("MANUAL");
        album.setPhotoCount(0);
        albumDataService.save(album);
        return convertToAlbumDTO(album);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAlbum(Long albumId, String albumName, String description, Long coverFileId) {
        MediaAlbum album = albumDataService.getById(albumId);
        if (album == null) {
            throw new BusinessException(ErrorCodeEnum.ALBUM_NOT_FOUND);
        }
        album.setAlbumName(albumName);
        album.setDescription(description);
        album.setCoverFileId(coverFileId);
        albumDataService.updateById(album);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAlbum(Long albumId) {
        MediaAlbum album = albumDataService.getById(albumId);
        if (album == null) {
            throw new BusinessException(ErrorCodeEnum.ALBUM_NOT_FOUND);
        }
        albumItemDataService.lambdaUpdate().eq(MediaAlbumItem::getAlbumId, albumId).remove();
        albumDataService.removeById(albumId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addPictureToAlbum(Long albumId, Long fileId) {
        MediaAlbum album = albumDataService.getById(albumId);
        if (album == null) {
            throw new BusinessException(ErrorCodeEnum.ALBUM_NOT_FOUND);
        }

        long count = albumItemDataService.lambdaQuery()
                .eq(MediaAlbumItem::getAlbumId, albumId)
                .eq(MediaAlbumItem::getFileId, fileId)
                .count();
        if (count > 0) {
            return;
        }

        MediaAlbumItem item = new MediaAlbumItem();
        item.setAlbumId(albumId);
        item.setFileId(fileId);
        item.setAddedAt(LocalDateTime.now());
        albumItemDataService.save(item);

        albumDataService.lambdaUpdate()
                .eq(MediaAlbum::getId, albumId)
                .setSql("photo_count = COALESCE(photo_count, 0) + 1")
                .update();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removePictureFromAlbum(Long albumId, Long fileId) {
        boolean removed = albumItemDataService.lambdaUpdate()
                .eq(MediaAlbumItem::getAlbumId, albumId)
                .eq(MediaAlbumItem::getFileId, fileId)
                .remove();

        if (removed) {
            albumDataService.lambdaUpdate()
                    .eq(MediaAlbum::getId, albumId)
                    .setSql("photo_count = GREATEST(COALESCE(photo_count, 0) - 1, 0)")
                    .update();
        }
    }

    @Override
    public PageResponseDTO<PictureListDTO> getAlbumPictures(Long albumId, Integer page, Integer pageSize) {
        int current = page != null && page > 0 ? page : 1;
        int size = pageSize != null && pageSize > 0 ? pageSize : 40;

        long total = albumItemDataService.lambdaQuery()
                .eq(MediaAlbumItem::getAlbumId, albumId)
                .count();

        if (total == 0) {
            return PageResponseDTO.of(new ArrayList<>(), 0L, current, size);
        }

        Page<MediaAlbumItem> itemPage = albumItemDataService.lambdaQuery()
                .eq(MediaAlbumItem::getAlbumId, albumId)
                .orderByAsc(MediaAlbumItem::getAddedAt)
                .page(new Page<>(current, size));

        List<Long> fileIds = itemPage.getRecords().stream()
                .map(MediaAlbumItem::getFileId)
                .collect(Collectors.toList());

        List<File> files = fileDataService.listByIds(fileIds);
        Map<Long, MediaPictureMetadata> metadataMap = batchLoadPictureMetadata(files);
        List<PictureListDTO> list = files.stream().map(f -> convertToPictureListDTO(f, metadataMap)).collect(Collectors.toList());
        return PageResponseDTO.of(list, total, current, size);
    }

    private Map<Long, MediaPictureMetadata> batchLoadPictureMetadata(List<File> files) {
        List<Long> fileIds = files.stream()
                .map(File::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (fileIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return pictureMetadataDataService.lambdaQuery()
                .in(MediaPictureMetadata::getFileId, fileIds)
                .list().stream()
                .collect(Collectors.toMap(MediaPictureMetadata::getFileId, m -> m, (a, b) -> a));
    }

    private PictureListDTO convertToPictureListDTO(File file, Map<Long, MediaPictureMetadata> metadataMap) {
        MediaPictureMetadata metadata = metadataMap.get(file.getId());

        PictureListDTO.PictureListDTOBuilder builder = PictureListDTO.builder()
                .fileId(file.getId())
                .resourceId(file.getResourceId())
                .fileName(file.getFileName())
                .size(file.getSize())
                .thumbnailUrls(getThumbnailUrls(file));

        if (metadata != null) {
            builder.width(metadata.getWidth())
                    .height(metadata.getHeight())
                    .takenAt(metadata.getTakenAt())
                    .locationName(metadata.getLocationName())
                    .cameraModel(metadata.getCameraModel());
        }

        return builder.build();
    }

    private AlbumDTO convertToAlbumDTO(MediaAlbum album) {
        File coverFile = album.getCoverFileId() != null ? fileDataService.getById(album.getCoverFileId()) : null;
        return AlbumDTO.builder()
                .albumId(album.getId())
                .albumName(album.getAlbumName())
                .albumType(album.getAlbumType())
                .coverFileId(album.getCoverFileId())
                .coverUrl(coverFile != null ? buildPictureThumbnailUrl(coverFile.getResourceId()) : null)
                .description(album.getDescription())
                .photoCount(album.getPhotoCount())
                .createTime(album.getCreateTime())
                .build();
    }

    private Map<String, String> getThumbnailUrls(File file) {
        Map<String, String> urls = new HashMap<>();
        urls.put("small", buildPictureThumbnailUrl(file.getResourceId()));
        urls.put("medium", buildPictureThumbnailUrl(file.getResourceId()));
        urls.put("large", buildPictureThumbnailUrl(file.getResourceId()));
        urls.put("original", buildPictureOriginalUrl(file.getResourceId()));
        return urls;
    }

    private LambdaQueryWrapper<File> buildPictureQuery(Integer year, Integer month, Long albumId, String directory) {
        LambdaQueryWrapper<File> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(File::getType, FileTypeEnum.PICTURE.toString());

        if (albumId != null) {
            List<Long> albumFileIds = albumItemDataService.lambdaQuery()
                    .eq(MediaAlbumItem::getAlbumId, albumId)
                    .list().stream()
                    .map(MediaAlbumItem::getFileId)
                    .collect(Collectors.toList());
            if (albumFileIds.isEmpty()) {
                wrapper.eq(File::getId, -1L);
                return wrapper;
            }
            wrapper.in(File::getId, albumFileIds);
        }

        if (year != null || month != null) {
            List<Long> metadataMatchedIds = resolvePictureIdsByTakenAt(year, month);
            if (metadataMatchedIds.isEmpty()) {
                wrapper.eq(File::getId, -1L);
                return wrapper;
            }
            wrapper.in(File::getId, metadataMatchedIds);
        }

        if (StringUtils.hasText(directory)) {
            Long folderId = resolveDirectoryFolderId(directory);
            if (folderId == null) {
                wrapper.eq(File::getId, -1L);
                return wrapper;
            }
            wrapper.eq(File::getParentId, folderId);
        }

        return wrapper;
    }

    private void applyPictureSort(LambdaQueryWrapper<File> wrapper, String sortBy, String sortOrder) {
        String sortField = sortBy != null ? sortBy : "createTime";
        boolean isAsc = "asc".equalsIgnoreCase(sortOrder);
        wrapper.orderBy(true, isAsc, getSortColumn(sortField));
    }

    private List<Long> resolvePictureIdsByTakenAt(Integer year, Integer month) {
        LambdaQueryWrapper<MediaPictureMetadata> query = new LambdaQueryWrapper<>();
        query.isNotNull(MediaPictureMetadata::getTakenAt);

        if (year != null && month != null) {
            LocalDateTime start = LocalDateTime.of(year, month, 1, 0, 0, 0);
            LocalDateTime end = start.plusMonths(1);
            query.ge(MediaPictureMetadata::getTakenAt, start)
                    .lt(MediaPictureMetadata::getTakenAt, end);
        } else if (year != null) {
            LocalDateTime start = LocalDateTime.of(year, 1, 1, 0, 0, 0);
            LocalDateTime end = LocalDateTime.of(year + 1, 1, 1, 0, 0, 0);
            query.ge(MediaPictureMetadata::getTakenAt, start)
                    .lt(MediaPictureMetadata::getTakenAt, end);
        }

        return pictureMetadataDataService.list(query).stream()
                .map(MediaPictureMetadata::getFileId)
                .distinct()
                .collect(Collectors.toList());
    }

    private Long resolveDirectoryFolderId(String directory) {
        File folder = fileDataService.lambdaQuery()
                .eq(File::getType, FileTypeEnum.FOLDER.toString())
                .eq(File::getFileName, directory.trim())
                .last("LIMIT 1")
                .one();
        return folder != null ? folder.getId() : null;
    }

    private String buildPictureTimelineCover(Long fileId) {
        File file = fileDataService.getById(fileId);
        return file != null ? buildPictureThumbnailUrl(file.getResourceId()) : null;
    }

    private String buildPictureThumbnailUrl(Long resourceId) {
        return resourceId != null ? "/v1/preview/image/" + resourceId + "/thumbnail" : null;
    }

    private String buildPictureOriginalUrl(Long resourceId) {
        return resourceId != null ? "/v1/preview/image/" + resourceId + "/original" : null;
    }

    private com.baomidou.mybatisplus.core.toolkit.support.SFunction<File, ?> getSortColumn(String sortBy) {
        return switch (sortBy != null ? sortBy.toLowerCase() : "createtime") {
            case "filename", "name" -> File::getFileName;
            case "size" -> File::getSize;
            default -> File::getCreateTime;
        };
    }
}
