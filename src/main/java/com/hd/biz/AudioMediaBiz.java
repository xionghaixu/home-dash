package com.hd.biz;

import com.hd.model.dto.*;

import java.util.List;

/**
 * 音频媒体业务接口
 *
 * @author xhx
 * @since 2026-04-26
 */
public interface AudioMediaBiz {

    PageResponseDTO<AudioListDTO> getAudioList(Integer page, Integer pageSize, String sortBy, String sortOrder, String album, String artist, String genre);

    AudioDetailDTO getAudioDetail(Long fileId);

    List<AudioAlbumDTO> getAlbumList();

    List<AudioListDTO> getAlbumTracks(String album);

    List<AudioArtistDTO> getArtistList();

    List<AudioListDTO> getArtistTracks(String artist);

    List<AudioPlaylistDTO> getPlaylistList();

    AudioPlaylistDTO createPlaylist(String playlistName, String description);

    void updatePlaylist(Long playlistId, String playlistName, String description);

    void deletePlaylist(Long playlistId);

    void addTrackToPlaylist(Long playlistId, Long fileId);

    void removeTrackFromPlaylist(Long playlistId, Long fileId);

    void reorderPlaylistTracks(Long playlistId, List<Long> fileIds);

    List<PlaylistItemDTO> getPlaylistTracks(Long playlistId);

    List<AudioListDTO> getRecentPlays(Integer limit);
}
