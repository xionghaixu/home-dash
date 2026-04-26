package com.hd.biz.impl;

import com.hd.biz.AudioMediaBiz;
import com.hd.common.enums.ErrorCode;
import com.hd.common.enums.FileType;
import com.hd.common.exception.BusinessException;
import com.hd.common.exception.DataNotFoundException;
import com.hd.dao.entity.*;
import com.hd.dao.service.*;
import com.hd.model.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 音频媒体业务实现类
 *
 * @author system
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
    public PageResponseDto<AudioListDto> getAudioList(Integer page, Integer pageSize, String sortBy, String sortOrder,
                                                       String album, String artist, String genre) {
        int current = page != null && page > 0 ? page : 1;
        int size = pageSize != null && pageSize > 0 ? pageSize : 30;

        List<File> files = fileDataService.lambdaQuery()
                .eq(File::getType, FileType.AUDIO.toString())
                .orderByDesc(File::getCreateTime)
                .list();

        List<AudioListDto> allAudio = files.stream().map(this::convertToAudioListDto).collect(Collectors.toList());

        if (album != null && !album.isEmpty()) {
            allAudio = allAudio.stream().filter(a -> album.equals(a.getAlbum())).collect(Collectors.toList());
        }
        if (artist != null && !artist.isEmpty()) {
            allAudio = allAudio.stream().filter(a -> artist.equals(a.getArtist())).collect(Collectors.toList());
        }
        if (genre != null && !genre.isEmpty()) {
            allAudio = allAudio.stream().filter(a -> genre.equals(a.getGenre())).collect(Collectors.toList());
        }

        long total = allAudio.size();
        int fromIndex = (current - 1) * size;
        int toIndex = Math.min(fromIndex + size, allAudio.size());
        List<AudioListDto> pageList = fromIndex < allAudio.size() ? allAudio.subList(fromIndex, toIndex) : new ArrayList<>();

        return PageResponseDto.of(pageList, total, current, size);
    }

    @Override
    public AudioDetailDto getAudioDetail(Long fileId) {
        File file = fileDataService.getById(fileId);
        if (file == null || !FileType.AUDIO.toString().equals(file.getType())) {
            throw new DataNotFoundException(String.format("音频不存在 [fileId=%d]", fileId));
        }

        MediaAudioMetadata metadata = audioMetadataDataService.lambdaQuery()
                .eq(MediaAudioMetadata::getFileId, fileId)
                .one();

        AudioDetailDto.AudioDetailDtoBuilder builder = AudioDetailDto.builder()
                .fileId(fileId)
                .fileName(file.getFileName())
                .audioUrl("/v1/resource/" + fileId)
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
                    .coverUrl(metadata.getCoverPath())
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
    public List<AudioAlbumDto> getAlbumList() {
        List<MediaAudioMetadata> metadataList = audioMetadataDataService.lambdaQuery()
                .isNotNull(MediaAudioMetadata::getAlbum)
                .list();

        Map<String, List<MediaAudioMetadata>> albumGroups = metadataList.stream()
                .collect(Collectors.groupingBy(MediaAudioMetadata::getAlbum));

        return albumGroups.entrySet().stream().map(entry -> {
            List<MediaAudioMetadata> list = entry.getValue();
            MediaAudioMetadata first = list.get(0);
            return AudioAlbumDto.builder()
                    .album(entry.getKey())
                    .artist(first.getAlbumArtist() != null ? first.getAlbumArtist() : first.getArtist())
                    .trackCount(list.size())
                    .coverUrl(first.getCoverPath())
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public List<AudioListDto> getAlbumTracks(String album) {
        List<MediaAudioMetadata> metadataList = audioMetadataDataService.lambdaQuery()
                .eq(MediaAudioMetadata::getAlbum, album)
                .orderByAsc(MediaAudioMetadata::getTrackNumber)
                .list();

        List<Long> fileIds = metadataList.stream().map(MediaAudioMetadata::getFileId).collect(Collectors.toList());
        if (fileIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<File> files = fileDataService.listByIds(fileIds);
        return files.stream().map(this::convertToAudioListDto).collect(Collectors.toList());
    }

    @Override
    public List<AudioArtistDto> getArtistList() {
        List<MediaAudioMetadata> metadataList = audioMetadataDataService.lambdaQuery()
                .isNotNull(MediaAudioMetadata::getArtist)
                .list();

        Map<String, List<MediaAudioMetadata>> artistGroups = metadataList.stream()
                .collect(Collectors.groupingBy(MediaAudioMetadata::getArtist));

        return artistGroups.entrySet().stream().map(entry -> {
            List<MediaAudioMetadata> list = entry.getValue();
            return AudioArtistDto.builder()
                    .artist(entry.getKey())
                    .trackCount(list.size())
                    .coverUrl(list.get(0).getCoverPath())
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public List<AudioListDto> getArtistTracks(String artist) {
        List<MediaAudioMetadata> metadataList = audioMetadataDataService.lambdaQuery()
                .eq(MediaAudioMetadata::getArtist, artist)
                .list();

        List<Long> fileIds = metadataList.stream().map(MediaAudioMetadata::getFileId).collect(Collectors.toList());
        if (fileIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<File> files = fileDataService.listByIds(fileIds);
        return files.stream().map(this::convertToAudioListDto).collect(Collectors.toList());
    }

    @Override
    public List<AudioPlaylistDto> getPlaylistList() {
        return playlistDataService.list().stream()
                .map(p -> AudioPlaylistDto.builder()
                        .playlistId(p.getId())
                        .playlistName(p.getPlaylistName())
                        .description(p.getDescription())
                        .coverUrl(p.getCoverPath())
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
    public AudioPlaylistDto createPlaylist(String playlistName, String description) {
        MediaAudioPlaylist playlist = new MediaAudioPlaylist();
        playlist.setPlaylistName(playlistName);
        playlist.setDescription(description);
        playlist.setPlayMode("ORDER");
        playlist.setTotalTracks(0);
        playlist.setTotalDuration(0L);
        playlist.setIsDefault(false);
        playlistDataService.save(playlist);
        return AudioPlaylistDto.builder()
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
            throw new BusinessException(ErrorCode.PLAYLIST_NOT_FOUND);
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
            throw new BusinessException(ErrorCode.PLAYLIST_NOT_FOUND);
        }
        playlistItemDataService.lambdaUpdate().eq(MediaAudioPlaylistItem::getPlaylistId, playlistId).remove();
        playlistDataService.removeById(playlistId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addTrackToPlaylist(Long playlistId, Long fileId) {
        MediaAudioPlaylist playlist = playlistDataService.getById(playlistId);
        if (playlist == null) {
            throw new BusinessException(ErrorCode.PLAYLIST_NOT_FOUND);
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
        for (int i = 0; i < fileIds.size(); i++) {
            Long fileId = fileIds.get(i);
            MediaAudioPlaylistItem item = playlistItemDataService.lambdaQuery()
                    .eq(MediaAudioPlaylistItem::getPlaylistId, playlistId)
                    .eq(MediaAudioPlaylistItem::getFileId, fileId)
                    .one();
            if (item != null) {
                item.setPosition(i + 1);
                playlistItemDataService.updateById(item);
            }
        }
    }

    @Override
    public List<PlaylistItemDto> getPlaylistTracks(Long playlistId) {
        List<MediaAudioPlaylistItem> items = playlistItemDataService.lambdaQuery()
                .eq(MediaAudioPlaylistItem::getPlaylistId, playlistId)
                .orderByAsc(MediaAudioPlaylistItem::getPosition)
                .list();

        List<Long> fileIds = items.stream().map(MediaAudioPlaylistItem::getFileId).collect(Collectors.toList());
        if (fileIds.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, File> fileMap = fileDataService.listByIds(fileIds).stream()
                .collect(Collectors.toMap(File::getId, f -> f));
        Map<Long, MediaAudioMetadata> metadataMap = audioMetadataDataService.lambdaQuery()
                .in(MediaAudioMetadata::getFileId, fileIds)
                .list().stream()
                .collect(Collectors.toMap(MediaAudioMetadata::getFileId, m -> m));

        return items.stream().map(item -> {
            File file = fileMap.get(item.getFileId());
            MediaAudioMetadata metadata = metadataMap.get(item.getFileId());
            return PlaylistItemDto.builder()
                    .fileId(item.getFileId())
                    .fileName(file != null ? file.getFileName() : null)
                    .title(metadata != null ? metadata.getTitle() : null)
                    .artist(metadata != null ? metadata.getArtist() : null)
                    .album(metadata != null ? metadata.getAlbum() : null)
                    .duration(metadata != null ? metadata.getDuration() : null)
                    .position(item.getPosition())
                    .coverUrl(metadata != null ? metadata.getCoverPath() : null)
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public List<AudioListDto> getRecentPlays(Integer limit) {
        List<PlayHistory> histories = playHistoryDataService.lambdaQuery()
                .eq(PlayHistory::getMediaType, "AUDIO")
                .orderByDesc(PlayHistory::getPlayTime)
                .last("LIMIT " + (limit != null ? limit : 20))
                .list();

        List<Long> fileIds = histories.stream().map(PlayHistory::getFileId).distinct().collect(Collectors.toList());
        if (fileIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<File> files = fileDataService.listByIds(fileIds);
        return files.stream().map(this::convertToAudioListDto).collect(Collectors.toList());
    }

    private AudioListDto convertToAudioListDto(File file) {
        MediaAudioMetadata metadata = audioMetadataDataService.lambdaQuery()
                .eq(MediaAudioMetadata::getFileId, file.getId())
                .one();

        AudioListDto.AudioListDtoBuilder builder = AudioListDto.builder()
                .fileId(file.getId())
                .fileName(file.getFileName());

        if (metadata != null) {
            builder.title(metadata.getTitle())
                    .album(metadata.getAlbum())
                    .artist(metadata.getArtist())
                    .duration(metadata.getDuration())
                    .bitrate(metadata.getBitrate())
                    .trackNumber(metadata.getTrackNumber())
                    .year(metadata.getYear())
                    .coverUrl(metadata.getCoverPath());
        }

        return builder.build();
    }
}
