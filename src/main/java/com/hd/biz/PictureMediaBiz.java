package com.hd.biz;

import com.hd.model.dto.*;

import java.util.List;

/**
 * 图片媒体业务接口
 *
 * @author xhx
 * @since 2026-04-26
 */
public interface PictureMediaBiz {

    PageResponseDTO<PictureListDTO> getPictureList(Integer page, Integer pageSize, String sortBy, String sortOrder, Integer year, Integer month, Long albumId, String directory);

    PictureDetailDTO getPictureDetail(Long fileId);

    List<PictureTimelineGroupDTO> getPictureTimeline(String groupBy);

    List<AlbumDTO> getAlbumList(String albumType);

    AlbumDTO createAlbum(String albumName, String description, Long coverFileId);

    void updateAlbum(Long albumId, String albumName, String description, Long coverFileId);

    void deleteAlbum(Long albumId);

    void addPictureToAlbum(Long albumId, Long fileId);

    void removePictureFromAlbum(Long albumId, Long fileId);

    PageResponseDTO<PictureListDTO> getAlbumPictures(Long albumId, Integer page, Integer pageSize);
}
