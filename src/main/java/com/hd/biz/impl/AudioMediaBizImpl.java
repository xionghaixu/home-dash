package com.hd.biz.impl;

import com.hd.biz.AudioMediaBiz;
import com.hd.common.enums.ErrorCodeEnum;
import com.hd.common.enums.FileTypeEnum;
import com.hd.common.exception.BusinessException;
import com.hd.common.exception.DataNotFoundException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hd.dao.entity.*;
import com.hd.dao.service.*;
import com.hd.model.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 音频媒体业务实现类
 *
 * @author xhx
 * @since 2026-04-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AudioMediaBizImpl implements AudioMediaBiz {

    private final FileDataService fileDataService;
    private final MediaAudioMetadataDataService audioMetadataDataService;
    private final MediaAudioPlaylistDataService playlistDataService;
    private final MediaAudioPlaylistItemDataService playlistItemDataService;
    private final PlayHistoryDataService playHistoryDataService;

    @Override
    public PageResponseDTO<AudioListDTO> getAudioList(Integer page, Integer pageSize, String sortBy, String sortOrder,
                                                       String album, String artist, String genre) {
         int current = page != null && page > 0 ? page : 1;
         int size = pageSize != null && pageSize > 0 ? pageSize : 30;

         LambdaQueryWrapper<File> query = new LambdaQueryWrapper<>();
         query.eq(File::getType, FileTypeEnum.AUDIO.toString())
                 .eq(File::getIsDeleted, 0);

         if (StringUtils.hasText(album) || StringUtils.hasText(artist) || StringUtils.hasText(genre)) {
             LambdaQueryWrapper<MediaAudioMetadata> metaQuery = new LambdaQueryWrapper<>();
             if (StringUtils.hasText(album)) {
                 metaQuery.eq(MediaAudioMetadata::getAlbum, album.trim());
             }
             if (StringUtils.hasText(artist)) {
                 metaQuery.eq(MediaAudioMetadata::getArtist, artist.trim());
             }
             if (StringUtils.hasText(genre)) {
                 metaQuery.eq(MediaAudioMetadata::getGenre, genre.trim());
             }
             List<Long> matchedFileIds = audioMetadataDataService.list(metaQuery).stream()
                     .map(MediaAudioMetadata::getFileId)
                     .collect(Collectors.toList());
             if (matchedFileIds.isEmpty()) {
                 return PageResponseDTO.of(new ArrayList<>(), 0L, current, size);
             }
             query.in(File::getId, matchedFileIds);
         }

         long total = fileDataService.count(query);
         if (total == 0) {
             return PageResponseDTO.of(new ArrayList<>(), 0L, current, size);
         }

         String normalizedSortBy = StringUtils.hasText(sortBy) ? sortBy.trim().toLowerCase() : "createtime";
         boolean isAsc = "asc".equalsIgnoreCase(sortOrder);
         
         switch (normalizedSortBy) {
             case "name", "filename" -> {
                 if (isAsc) query.orderByAsc(File::getFileName);
                 else query.orderByDesc(File::getFileName);
             }
             case "size" -> {
                 if (isAsc) query.orderByAsc(File::getSize);
                 else query.orderByDesc(File::getSize);
             }
             case "title" -> {
                 String direction = isAsc ? "ASC" : "DESC";
                 query.last("ORDER BY (SELECT title FROM media_audio_metadata WHERE file_id = id) " + direction);
             }
             case "artist" -> {
                 String direction = isAsc ? "ASC" : "DESC";
                 query.last("ORDER BY (SELECT artist FROM media_audio_metadata WHERE file_id = id) " + direction);
             }
             case "album" -> {
                 String direction = isAsc ? "ASC" : "DESC";
                 query.last("ORDER BY (SELECT album FROM media_audio_metadata WHERE file_id = id) " + direction);
             }
             case "duration" -> {
                 String direction = isAsc ? "ASC" : "DESC";
                 query.last("ORDER BY (SELECT duration FROM media_audio_metadata WHERE file_id = id) " + direction);
             }
             default -> {
                 if (isAsc) query.orderByAsc(File::getCreateTime);
                 else query.orderByDesc(File::getCreateTime);
             }
         }

         Page<File> filePage = fileDataService.page(new Page<>(current, size), query);
         List<File> pageFiles = filePage.getRecords();

         List<Long> fileIds = pageFiles.stream().map(File::getId).collect(Collectors.toList());
         Map<Long, MediaAudioMetadata> metadataMap = audioMetadataDataService.lambdaQuery()
                 .in(MediaAudioMetadata::getFileId, fileIds)
                 .list().stream()
                 .collect(Collectors.toMap(MediaAudioMetadata::getFileId, m -> m, (l, r) -> l));

         List<AudioListDTO> pageList = pageFiles.stream()
                 .map(file -> convertToAudioListDTO(file, metadataMap.get(file.getId())))
                 .collect(Collectors.toList());

         return PageResponseDTO.of(pageList, total, current, size);
     }

    @Override
    public AudioDetailDTO getAudioDetail(Long fileId) {
        File file = fileDataService.getById(fileId);
        if (file == null || !FileTypeEnum.AUDIO.toString().equals(file.getType())) {
            throw new DataNotFoundException(String.format("音频不存在 [fileId=%d]", fileId));
        }

        MediaAudioMetadata metadata = audioMetadataDataService.lambdaQuery()
                .eq(MediaAudioMetadata::getFileId, fileId)
                .one();

        AudioDetailDTO.AudioDetailDTOBuilder builder = AudioDetailDTO.builder()
                .fileId(fileId)
                .fileName(file.getFileName())
                .audioUrl(buildAudioStreamUrl(file.getResourceId()))
                .favorite(false);

        if (metadata != null) {
            builder.title(metadata.getTitle())
                    .artist(metadata.getArtist())
                    .album(metadata.getAlbum())
                    .albumArtist(metadata.getAlbumArtist())
                    .genre(metadata.getGenre())
                    .trackNumber(metadata.getTrackNumber())
                    .discNumber(metadata.getDiscNumber())
                    .year(metadata.getYear())
                    .duration(metadata.getDuration())
                    .bitrate(metadata.getBitrate())
                    .sampleRate(metadata.getSampleRate())
                    .coverUrl(normalizeAccessibleUrl(metadata.getCoverPath()))
                    .lyrics(metadata.getLyrics());
        }

        List<Long> playlistIds = playlistItemDataService.lambdaQuery()
                .eq(MediaAudioPlaylistItem::getFileId, fileId)
                .list().stream()
                .map(MediaAudioPlaylistItem::getPlaylistId)
                .collect(Collectors.toList());
        builder.playlistIds(playlistIds);

        return builder.build();
    }

    @Override
    public List<AudioAlbumDTO> getAlbumList() {
        List<MediaAudioMetadata> metadataList = audioMetadataDataService.lambdaQuery()
                .isNotNull(MediaAudioMetadata::getAlbum)
                .list();

        Map<String, List<MediaAudioMetadata>> albumGroups = metadataList.stream()
                .collect(Collectors.groupingBy(MediaAudioMetadata::getAlbum));

        return albumGroups.entrySet().stream().map(entry -> {
            List<MediaAudioMetadata> list = entry.getValue();
            MediaAudioMetadata first = list.get(0);
            return AudioAlbumDTO.builder()
                    .album(entry.getKey())
                    .artist(first.getAlbumArtist() != null ? first.getAlbumArtist() : first.getArtist())
                    .trackCount(list.size())
                    .coverUrl(normalizeAccessibleUrl(first.getCoverPath()))
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public List<AudioListDTO> getAlbumTracks(String album) {
        List<MediaAudioMetadata> metadataList = audioMetadataDataService.lambdaQuery()
                .eq(MediaAudioMetadata::getAlbum, album)
                .orderByAsc(MediaAudioMetadata::getTrackNumber)
                .list();

        List<Long> fileIds = metadataList.stream().map(MediaAudioMetadata::getFileId).collect(Collectors.toList());
        if (fileIds.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, File> fileMap = fileDataService.listByIds(fileIds).stream()
                .collect(Collectors.toMap(File::getId, file -> file));
        return metadataList.stream()
                .map(metadata -> {
                    File file = fileMap.get(metadata.getFileId());
                    return file != null ? convertToAudioListDTO(file, metadata) : null;
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<AudioArtistDTO> getArtistList() {
        List<MediaAudioMetadata> metadataList = audioMetadataDataService.lambdaQuery()
                .isNotNull(MediaAudioMetadata::getArtist)
                .list();

        Map<String, List<MediaAudioMetadata>> artistGroups = metadataList.stream()
                .collect(Collectors.groupingBy(MediaAudioMetadata::getArtist));

        return artistGroups.entrySet().stream().map(entry -> {
            List<MediaAudioMetadata> list = entry.getValue();
            return AudioArtistDTO.builder()
                    .artist(entry.getKey())
                    .trackCount(list.size())
                    .coverUrl(normalizeAccessibleUrl(list.get(0).getCoverPath()))
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public List<AudioListDTO> getArtistTracks(String artist) {
        List<MediaAudioMetadata> metadataList = audioMetadataDataService.lambdaQuery()
                .eq(MediaAudioMetadata::getArtist, artist)
                .list();

        List<Long> fileIds = metadataList.stream().map(MediaAudioMetadata::getFileId).collect(Collectors.toList());
        if (fileIds.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, File> fileMap = fileDataService.listByIds(fileIds).stream()
                .collect(Collectors.toMap(File::getId, file -> file));
        return metadataList.stream()
                .map(metadata -> {
                    File file = fileMap.get(metadata.getFileId());
                    return file != null ? convertToAudioListDTO(file, metadata) : null;
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<AudioPlaylistDTO> getPlaylistList() {
        return playlistDataService.list().stream()
                .map(p -> AudioPlaylistDTO.builder()
                        .playlistId(p.getId())
                        .playlistName(p.getPlaylistName())
                        .description(p.getDescription())
                        .coverUrl(normalizeAccessibleUrl(p.getCoverPath()))
                        .playMode(p.getPlayMode())
                        .totalTracks(p.getTotalTracks())
                        .totalDuration(p.getTotalDuration())
                        .isDefault(p.getIsDefault())
                        .createTime(p.getCreateTime())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AudioPlaylistDTO createPlaylist(String playlistName, String description) {
        MediaAudioPlaylist playlist = new MediaAudioPlaylist();
        playlist.setPlaylistName(playlistName);
        playlist.setDescription(description);
        playlist.setPlayMode("ORDER");
        playlist.setTotalTracks(0);
        playlist.setTotalDuration(0L);
        playlist.setIsDefault(false);
        playlistDataService.save(playlist);
        return AudioPlaylistDTO.builder()
                .playlistId(playlist.getId())
                .playlistName(playlist.getPlaylistName())
                .description(playlist.getDescription())
                .playMode(playlist.getPlayMode())
                .totalTracks(0)
                .totalDuration(0L)
                .isDefault(false)
                .createTime(playlist.getCreateTime())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePlaylist(Long playlistId, String playlistName, String description) {
        MediaAudioPlaylist playlist = playlistDataService.getById(playlistId);
        if (playlist == null) {
            throw new BusinessException(ErrorCodeEnum.PLAYLIST_NOT_FOUND);
        }
        playlist.setPlaylistName(playlistName);
        playlist.setDescription(description);
        playlistDataService.updateById(playlist);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePlaylist(Long playlistId) {
        MediaAudioPlaylist playlist = playlistDataService.getById(playlistId);
        if (playlist == null) {
            throw new BusinessException(ErrorCodeEnum.PLAYLIST_NOT_FOUND);
        }
        playlistItemDataService.lambdaUpdate().eq(MediaAudioPlaylistItem::getPlaylistId, playlistId).remove();
        playlistDataService.removeById(playlistId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addTrackToPlaylist(Long playlistId, Long fileId) {
        MediaAudioPlaylist playlist = playlistDataService.getById(playlistId);
        if (playlist == null) {
            throw new BusinessException(ErrorCodeEnum.PLAYLIST_NOT_FOUND);
        }

        long count = playlistItemDataService.lambdaQuery()
                .eq(MediaAudioPlaylistItem::getPlaylistId, playlistId)
                .eq(MediaAudioPlaylistItem::getFileId, fileId)
                .count();
        if (count > 0) {
            return;
        }

        int maxPosition = playlistItemDataService.lambdaQuery()
                .eq(MediaAudioPlaylistItem::getPlaylistId, playlistId)
                .list().stream()
                .mapToInt(MediaAudioPlaylistItem::getPosition)
                .max().orElse(0);

        MediaAudioPlaylistItem item = new MediaAudioPlaylistItem();
        item.setPlaylistId(playlistId);
        item.setFileId(fileId);
        item.setPosition(maxPosition + 1);
        item.setAddedAt(LocalDateTime.now());
        playlistItemDataService.save(item);

        playlist.setTotalTracks(playlist.getTotalTracks() + 1);
        playlistDataService.updateById(playlist);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeTrackFromPlaylist(Long playlistId, Long fileId) {
        playlistItemDataService.lambdaUpdate()
                .eq(MediaAudioPlaylistItem::getPlaylistId, playlistId)
                .eq(MediaAudioPlaylistItem::getFileId, fileId)
                .remove();

        MediaAudioPlaylist playlist = playlistDataService.getById(playlistId);
        if (playlist != null && playlist.getTotalTracks() > 0) {
            playlist.setTotalTracks(playlist.getTotalTracks() - 1);
            playlistDataService.updateById(playlist);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reorderPlaylistTracks(Long playlistId, List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }

        Map<Long, MediaAudioPlaylistItem> itemMap = playlistItemDataService.lambdaQuery()
                .eq(MediaAudioPlaylistItem::getPlaylistId, playlistId)
                .in(MediaAudioPlaylistItem::getFileId, fileIds)
                .list().stream()
                .collect(Collectors.toMap(MediaAudioPlaylistItem::getFileId, i -> i, (a, b) -> a));

        List<MediaAudioPlaylistItem> toUpdate = new ArrayList<>();
        for (int i = 0; i < fileIds.size(); i++) {
            Long fileId = fileIds.get(i);
            MediaAudioPlaylistItem item = itemMap.get(fileId);
            if (item != null) {
                item.setPosition(i + 1);
                toUpdate.add(item);
            }
        }

        if (!toUpdate.isEmpty()) {
            playlistItemDataService.updateBatchById(toUpdate);
        }
    }

    @Override
    public List<PlaylistItemDTO> getPlaylistTracks(Long playlistId) {
        List<MediaAudioPlaylistItem> items = playlistItemDataService.lambdaQuery()
                .eq(MediaAudioPlaylistItem::getPlaylistId, playlistId)
                .orderByAsc(MediaAudioPlaylistItem::getPosition)
                .list();

        List<Long> fileIds = items.stream().map(MediaAudioPlaylistItem::getFileId).collect(Collectors.toList());
        if (fileIds.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, File> fileMap = fileDataService.listByIds(fileIds).stream()
                .collect(Collectors.toMap(File::getId, f -> f, (a, b) -> a));
        Map<Long, MediaAudioMetadata> metadataMap = audioMetadataDataService.lambdaQuery()
                .in(MediaAudioMetadata::getFileId, fileIds)
                .list().stream()
                .collect(Collectors.toMap(MediaAudioMetadata::getFileId, m -> m, (a, b) -> a));

        return items.stream().map(item -> {
            File file = fileMap.get(item.getFileId());
            MediaAudioMetadata metadata = metadataMap.get(item.getFileId());
            return PlaylistItemDTO.builder()
                    .fileId(item.getFileId())
                    .fileName(file != null ? file.getFileName() : null)
                    .title(metadata != null ? metadata.getTitle() : null)
                    .artist(metadata != null ? metadata.getArtist() : null)
                    .album(metadata != null ? metadata.getAlbum() : null)
                    .duration(metadata != null ? metadata.getDuration() : null)
                    .position(item.getPosition())
                    .coverUrl(metadata != null ? normalizeAccessibleUrl(metadata.getCoverPath()) : null)
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public List<AudioListDTO> getRecentPlays(Integer limit) {
        List<PlayHistory> histories = playHistoryDataService.lambdaQuery()
                .eq(PlayHistory::getMediaType, "AUDIO")
                .orderByDesc(PlayHistory::getPlayTime)
                .last("LIMIT " + (limit != null ? limit : 20))
                .list();

        List<Long> fileIds = histories.stream().map(PlayHistory::getFileId).distinct().collect(Collectors.toList());
        if (fileIds.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, File> fileMap = fileDataService.listByIds(fileIds).stream()
                .collect(Collectors.toMap(File::getId, file -> file));
        Map<Long, MediaAudioMetadata> metadataMap = getAudioMetadataMap(fileIds);

        return fileIds.stream()
                .map(fileMap::get)
                .filter(java.util.Objects::nonNull)
                .map(file -> convertToAudioListDTO(file, metadataMap.get(file.getId())))
                .collect(Collectors.toList());
    }

    private AudioListDTO convertToAudioListDTO(File file) {
        return convertToAudioListDTO(file, null);
    }

    private AudioListDTO convertToAudioListDTO(File file, MediaAudioMetadata metadata) {
        MediaAudioMetadata resolvedMetadata = metadata;
        if (resolvedMetadata == null) {
            log.warn("convertToAudioListDTO: metadata not pre-loaded for fileId={}, falling back to per-file query", file.getId());
            resolvedMetadata = audioMetadataDataService.lambdaQuery()
                    .eq(MediaAudioMetadata::getFileId, file.getId())
                    .one();
        }

        AudioListDTO.AudioListDTOBuilder builder = AudioListDTO.builder()
                .fileId(file.getId())
                .resourceId(file.getResourceId())
                .fileName(file.getFileName())
                .audioUrl(buildAudioStreamUrl(file.getResourceId()));

        if (resolvedMetadata != null) {
            builder.title(resolvedMetadata.getTitle())
                    .album(resolvedMetadata.getAlbum())
                    .artist(resolvedMetadata.getArtist())
                    .duration(resolvedMetadata.getDuration())
                    .bitrate(resolvedMetadata.getBitrate())
                    .trackNumber(resolvedMetadata.getTrackNumber())
                    .year(resolvedMetadata.getYear())
                    .genre(resolvedMetadata.getGenre())
                    .coverUrl(normalizeAccessibleUrl(resolvedMetadata.getCoverPath()));
        }

        return builder.build();
    }

    private Map<Long, MediaAudioMetadata> getAudioMetadataMap(List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return Map.of();
        }

        return audioMetadataDataService.lambdaQuery()
                .in(MediaAudioMetadata::getFileId, fileIds)
                .list().stream()
                .collect(Collectors.toMap(MediaAudioMetadata::getFileId, metadata -> metadata, (left, right) -> left));
    }

    private boolean matchesAudioFilter(MediaAudioMetadata metadata, String album, String artist, String genre) {
        return matchesTextFilter(metadata != null ? metadata.getAlbum() : null, album)
                && matchesTextFilter(metadata != null ? metadata.getArtist() : null, artist)
                && matchesTextFilter(metadata != null ? metadata.getGenre() : null, genre);
    }

    private boolean matchesTextFilter(String actualValue, String expectedValue) {
        return !StringUtils.hasText(expectedValue) || expectedValue.equals(actualValue);
    }

    private void applyAudioMetadataSorting(LambdaQueryWrapper<MediaAudioMetadata> query, String sortBy, boolean isAsc) {
        switch (sortBy) {
            case "title", "name", "filename" -> {
                if (isAsc) query.orderByAsc(MediaAudioMetadata::getTitle);
                else query.orderByDesc(MediaAudioMetadata::getTitle);
            }
            case "artist" -> {
                if (isAsc) query.orderByAsc(MediaAudioMetadata::getArtist);
                else query.orderByDesc(MediaAudioMetadata::getArtist);
            }
            case "album" -> {
                if (isAsc) query.orderByAsc(MediaAudioMetadata::getAlbum);
                else query.orderByDesc(MediaAudioMetadata::getAlbum);
            }
            case "duration" -> {
                if (isAsc) query.orderByAsc(MediaAudioMetadata::getDuration);
                else query.orderByDesc(MediaAudioMetadata::getDuration);
            }
            case "size" -> {
                if (isAsc) query.orderByAsc(MediaAudioMetadata::getFileId);
                else query.orderByDesc(MediaAudioMetadata::getFileId);
            }
            default -> {
                if (isAsc) query.orderByAsc(MediaAudioMetadata::getCreateTime);
                else query.orderByDesc(MediaAudioMetadata::getCreateTime);
            }
        }
    }

    private String resolveAudioDisplayName(File file, MediaAudioMetadata metadata) {
        if (metadata != null && StringUtils.hasText(metadata.getTitle())) {
            return metadata.getTitle();
        }
        return file != null ? file.getFileName() : null;
    }

    private String safeLowerCase(String value) {
        return value != null ? value.toLowerCase() : null;
    }

    private String buildAudioStreamUrl(Long resourceId) {
        return resourceId != null ? "/v1/preview/audio/" + resourceId + "/stream" : null;
    }

    private String normalizeAccessibleUrl(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        if (path.startsWith("http://") || path.startsWith("https://") || path.startsWith("/")) {
            return path;
        }
        return null;
    }
}
