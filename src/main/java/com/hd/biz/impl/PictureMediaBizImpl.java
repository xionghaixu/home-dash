package com.hd.biz.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hd.biz.PictureMediaBiz;
import com.hd.common.enums.ErrorCode;
import com.hd.common.enums.FileType;
import com.hd.common.exception.BusinessException;
import com.hd.common.exception.DataNotFoundException;
import com.hd.common.util.FileSizeFormatter;
import com.hd.dao.entity.*;
import com.hd.dao.service.*;
import com.hd.model.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 图片媒体业务实现类
 *
 * @author system
 * @since 2026-04-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PictureMediaBizImpl implements PictureMediaBiz {

    private final FileDataService fileDataService;
    private final MediaPictureMetadataDataService pictureMetadataDataService;
    private final MediaThumbnailDataService thumbnailDataService;
    private final MediaAlbumDataService albumDataService;
    private final MediaAlbumItemDataService albumItemDataService;

    @Override
    public PageResponseDto<PictureListDto> getPictureList(Integer page, Integer pageSize, String sortBy, String sortOrder,
                                                           Integer year, Integer month, Long albumId, String directory) {
        int current = page != null && page > 0 ? page : 1;
        int size = pageSize != null && pageSize > 0 ? pageSize : 40;

        LambdaQueryWrapper<File> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(File::getType, FileType.PICTURE.toString());

        if (albumId != null) {
            List<Long> fileIds = albumItemDataService.lambdaQuery()
                    .eq(MediaAlbumItem::getAlbumId, albumId)
                    .list().stream()
                    .map(MediaAlbumItem::getFileId)
                    .collect(Collectors.toList());
            if (fileIds.isEmpty()) {
                return PageResponseDto.of(new ArrayList<>(), 0L, current, size);
            }
            wrapper.in(File::getId, fileIds);
        }

        long total = fileDataService.count(wrapper);

        String sortField = sortBy != null ? sortBy : "createTime";
        boolean isAsc = "asc".equalsIgnoreCase(sortOrder);

        List<File> files = fileDataService.lambdaQuery()
                .eq(File::getType, FileType.PICTURE.toString())
                .orderBy(true, isAsc, getSortColumn(sortField))
                .last("LIMIT " + size + " OFFSET " + (current - 1) * size)
                .list();

        List<PictureListDto> list = files.stream().map(this::convertToPictureListDto).collect(Collectors.toList());
        return PageResponseDto.of(list, total, current, size);
    }

    @Override
    public PictureDetailDto getPictureDetail(Long fileId) {
        File file = fileDataService.getById(fileId);
        if (file == null || !FileType.PICTURE.toString().equals(file.getType())) {
            throw new DataNotFoundException(String.format("图片不存在 [fileId=%d]", fileId));
        }

        MediaPictureMetadata metadata = pictureMetadataDataService.lambdaQuery()
                .eq(MediaPictureMetadata::getFileId, fileId)
                .one();

        PictureDetailDto.PictureDetailDtoBuilder builder = PictureDetailDto.builder()
                .fileId(fileId)
                .fileName(file.getFileName())
                .size(file.getSize())
                .thumbnailUrls(getThumbnailUrls(fileId))
                .originalUrl("/v1/resource/" + fileId);

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
    public List<PictureTimelineGroupDto> getPictureTimeline(String groupBy) {
        List<MediaPictureMetadata> metadataList = pictureMetadataDataService.lambdaQuery()
                .isNotNull(MediaPictureMetadata::getTakenAt)
                .orderByDesc(MediaPictureMetadata::getTakenAt)
                .list();

        Map<String, List<MediaPictureMetadata>> groups;
        switch (groupBy != null ? groupBy.toLowerCase() : "month") {
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
            return PictureTimelineGroupDto.builder()
                    .year(first.getTakenAt().getYear())
                    .month("month".equals(groupBy) || "day".equals(groupBy) ? first.getTakenAt().getMonthValue() : null)
                    .day("day".equals(groupBy) ? first.getTakenAt().getDayOfMonth() : null)
                    .count((long) list.size())
                    .coverFileId(first.getFileId())
                    .coverThumbnail("/thumb/" + first.getFileId() + "_small.jpg")
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public List<AlbumDto> getAlbumList(String albumType) {
        LambdaQueryWrapper<MediaAlbum> wrapper = new LambdaQueryWrapper<>();
        if (albumType != null && !albumType.isEmpty()) {
            wrapper.eq(MediaAlbum::getAlbumType, albumType.toUpperCase());
        }
        return albumDataService.list(wrapper).stream().map(this::convertToAlbumDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AlbumDto createAlbum(String albumName, String description, Long coverFileId) {
        MediaAlbum album = new MediaAlbum();
        album.setAlbumName(albumName);
        album.setDescription(description);
        album.setCoverFileId(coverFileId);
        album.setAlbumType("MANUAL");
        album.setPhotoCount(0);
        albumDataService.save(album);
        return convertToAlbumDto(album);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAlbum(Long albumId, String albumName, String description, Long coverFileId) {
        MediaAlbum album = albumDataService.getById(albumId);
        if (album == null) {
            throw new BusinessException(ErrorCode.ALBUM_NOT_FOUND);
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
            throw new BusinessException(ErrorCode.ALBUM_NOT_FOUND);
        }
        albumItemDataService.lambdaUpdate().eq(MediaAlbumItem::getAlbumId, albumId).remove();
        albumDataService.removeById(albumId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addPictureToAlbum(Long albumId, Long fileId) {
        MediaAlbum album = albumDataService.getById(albumId);
        if (album == null) {
            throw new BusinessException(ErrorCode.ALBUM_NOT_FOUND);
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

        album.setPhotoCount(album.getPhotoCount() + 1);
        albumDataService.updateById(album);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removePictureFromAlbum(Long albumId, Long fileId) {
        albumItemDataService.lambdaUpdate()
                .eq(MediaAlbumItem::getAlbumId, albumId)
                .eq(MediaAlbumItem::getFileId, fileId)
                .remove();

        MediaAlbum album = albumDataService.getById(albumId);
        if (album != null && album.getPhotoCount() > 0) {
            album.setPhotoCount(album.getPhotoCount() - 1);
            albumDataService.updateById(album);
        }
    }

    @Override
    public PageResponseDto<PictureListDto> getAlbumPictures(Long albumId, Integer page, Integer pageSize) {
        int current = page != null && page > 0 ? page : 1;
        int size = pageSize != null && pageSize > 0 ? pageSize : 40;

        List<Long> fileIds = albumItemDataService.lambdaQuery()
                .eq(MediaAlbumItem::getAlbumId, albumId)
                .list().stream()
                .map(MediaAlbumItem::getFileId)
                .collect(Collectors.toList());

        if (fileIds.isEmpty()) {
            return PageResponseDto.of(new ArrayList<>(), 0L, current, size);
        }

        List<File> files = fileDataService.listByIds(fileIds);
        List<PictureListDto> list = files.stream().map(this::convertToPictureListDto).collect(Collectors.toList());
        return PageResponseDto.of(list, (long) files.size(), current, size);
    }

    private PictureListDto convertToPictureListDto(File file) {
        MediaPictureMetadata metadata = pictureMetadataDataService.lambdaQuery()
                .eq(MediaPictureMetadata::getFileId, file.getId())
                .one();

        PictureListDto.PictureListDtoBuilder builder = PictureListDto.builder()
                .fileId(file.getId())
                .fileName(file.getFileName())
                .size(file.getSize())
                .thumbnailUrls(getThumbnailUrls(file.getId()));

        if (metadata != null) {
            builder.width(metadata.getWidth())
                    .height(metadata.getHeight())
                    .takenAt(metadata.getTakenAt())
                    .locationName(metadata.getLocationName())
                    .cameraModel(metadata.getCameraModel());
        }

        return builder.build();
    }

    private AlbumDto convertToAlbumDto(MediaAlbum album) {
        return AlbumDto.builder()
                .albumId(album.getId())
                .albumName(album.getAlbumName())
                .albumType(album.getAlbumType())
                .coverFileId(album.getCoverFileId())
                .coverUrl(album.getCoverFileId() != null ? "/thumb/" + album.getCoverFileId() + "_medium.jpg" : null)
                .description(album.getDescription())
                .photoCount(album.getPhotoCount())
                .createTime(album.getCreateTime())
                .build();
    }

    private Map<String, String> getThumbnailUrls(Long fileId) {
        Map<String, String> urls = new HashMap<>();
        urls.put("small", "/thumb/" + fileId + "_320.jpg");
        urls.put("medium", "/thumb/" + fileId + "_800.jpg");
        urls.put("large", "/thumb/" + fileId + "_1920.jpg");
        urls.put("original", "/thumb/" + fileId + "_origin.jpg");
        return urls;
    }

    private com.baomidou.mybatisplus.core.toolkit.support.SFunction<File, ?> getSortColumn(String sortBy) {
        return switch (sortBy != null ? sortBy.toLowerCase() : "createtime") {
            case "filename", "name" -> File::getFileName;
            case "size" -> File::getSize;
            default -> File::getCreateTime;
        };
    }
}
