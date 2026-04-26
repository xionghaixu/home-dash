package com.hd.biz;

import com.hd.model.dto.*;

import java.util.List;

/**
 * 音频媒体业务接口
 *
 * @author system
 * @since 2026-04-26
 */
public interface AudioMediaBiz {

    PageResponseDto<AudioListDto> getAudioList(Integer page, Integer pageSize, String sortBy, String sortOrder, String album, String artist, String genre);

    AudioDetailDto getAudioDetail(Long fileId);

    List<AudioAlbumDto> getAlbumList();

    List<AudioListDto> getAlbumTracks(String album);

    List<AudioArtistDto> getArtistList();

    List<AudioListDto> getArtistTracks(String artist);

    List<AudioPlaylistDto> getPlaylistList();

    AudioPlaylistDto createPlaylist(String playlistName, String description);

    void updatePlaylist(Long playlistId, String playlistName, String description);

    void deletePlaylist(Long playlistId);

    void addTrackToPlaylist(Long playlistId, Long fileId);

    void removeTrackFromPlaylist(Long playlistId, Long fileId);

    void reorderPlaylistTracks(Long playlistId, List<Long> fileIds);

    List<PlaylistItemDto> getPlaylistTracks(Long playlistId);

    List<AudioListDto> getRecentPlays(Integer limit);
}
