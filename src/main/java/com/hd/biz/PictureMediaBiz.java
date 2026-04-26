package com.hd.biz;

import com.hd.model.dto.*;

import java.util.List;

/**
 * 图片媒体业务接口
 *
 * @author system
 * @since 2026-04-26
 */
public interface PictureMediaBiz {

    PageResponseDto<PictureListDto> getPictureList(Integer page, Integer pageSize, String sortBy, String sortOrder, Integer year, Integer month, Long albumId, String directory);

    PictureDetailDto getPictureDetail(Long fileId);

    List<PictureTimelineGroupDto> getPictureTimeline(String groupBy);

    List<AlbumDto> getAlbumList(String albumType);

    AlbumDto createAlbum(String albumName, String description, Long coverFileId);

    void updateAlbum(Long albumId, String albumName, String description, Long coverFileId);

    void deleteAlbum(Long albumId);

    void addPictureToAlbum(Long albumId, Long fileId);

    void removePictureFromAlbum(Long albumId, Long fileId);

    PageResponseDto<PictureListDto> getAlbumPictures(Long albumId, Integer page, Integer pageSize);
}
