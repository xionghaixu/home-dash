package com.hd.controller;

import com.hd.biz.AudioMediaBiz;
import com.hd.model.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 音频媒体控制器
 *
 * @author system
 * @since 2026-04-26
 */
@RestController
@RequestMapping("/v1/media")
@RequiredArgsConstructor
public class AudioMediaController {

    private final AudioMediaBiz audioMediaBiz;

    @GetMapping("/audio")
    public ResponseDto getAudioList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "30") Integer pageSize,
            @RequestParam(defaultValue = "title") String sortBy,
            @RequestParam(defaultValue = "asc") String sortOrder,
            @RequestParam(required = false) String album,
            @RequestParam(required = false) String artist,
            @RequestParam(required = false) String genre) {
        PageResponseDto<AudioListDto> result = audioMediaBiz.getAudioList(page, pageSize, sortBy, sortOrder, album, artist, genre);
        return ResponseDto.success(result);
    }

    @GetMapping("/audio/{fileId}")
    public ResponseDto getAudioDetail(@PathVariable Long fileId) {
        AudioDetailDto detail = audioMediaBiz.getAudioDetail(fileId);
        return ResponseDto.success(detail);
    }

    @GetMapping("/albums/audio")
    public ResponseDto getAudioAlbumList() {
        List<AudioAlbumDto> albums = audioMediaBiz.getAlbumList();
        return ResponseDto.success(albums);
    }

    @GetMapping("/albums/audio/{album}/tracks")
    public ResponseDto getAlbumTracks(@PathVariable String album) {
        List<AudioListDto> tracks = audioMediaBiz.getAlbumTracks(album);
        return ResponseDto.success(tracks);
    }

    @GetMapping("/artists")
    public ResponseDto getArtistList() {
        List<AudioArtistDto> artists = audioMediaBiz.getArtistList();
        return ResponseDto.success(artists);
    }

    @GetMapping("/artists/{artist}/tracks")
    public ResponseDto getArtistTracks(@PathVariable String artist) {
        List<AudioListDto> tracks = audioMediaBiz.getArtistTracks(artist);
        return ResponseDto.success(tracks);
    }

    @GetMapping("/playlists")
    public ResponseDto getPlaylistList() {
        List<AudioPlaylistDto> playlists = audioMediaBiz.getPlaylistList();
        return ResponseDto.success(playlists);
    }

    @PostMapping("/playlists")
    public ResponseDto createPlaylist(@RequestBody AudioPlaylistDto playlistDto) {
        AudioPlaylistDto playlist = audioMediaBiz.createPlaylist(playlistDto.getPlaylistName(), playlistDto.getDescription());
        return ResponseDto.success(playlist);
    }

    @PutMapping("/playlists/{playlistId}")
    public ResponseDto updatePlaylist(@PathVariable Long playlistId, @RequestBody AudioPlaylistDto playlistDto) {
        audioMediaBiz.updatePlaylist(playlistId, playlistDto.getPlaylistName(), playlistDto.getDescription());
        return ResponseDto.success();
    }

    @DeleteMapping("/playlists/{playlistId}")
    public ResponseDto deletePlaylist(@PathVariable Long playlistId) {
        audioMediaBiz.deletePlaylist(playlistId);
        return ResponseDto.success();
    }

    @PostMapping("/playlists/{playlistId}/tracks")
    public ResponseDto addTrackToPlaylist(@PathVariable Long playlistId, @RequestParam Long fileId) {
        audioMediaBiz.addTrackToPlaylist(playlistId, fileId);
        return ResponseDto.success();
    }

    @DeleteMapping("/playlists/{playlistId}/tracks/{fileId}")
    public ResponseDto removeTrackFromPlaylist(@PathVariable Long playlistId, @PathVariable Long fileId) {
        audioMediaBiz.removeTrackFromPlaylist(playlistId, fileId);
        return ResponseDto.success();
    }

    @PutMapping("/playlists/{playlistId}/tracks/reorder")
    public ResponseDto reorderPlaylistTracks(@PathVariable Long playlistId, @RequestBody List<Long> fileIds) {
        audioMediaBiz.reorderPlaylistTracks(playlistId, fileIds);
        return ResponseDto.success();
    }

    @GetMapping("/playlists/{playlistId}/tracks")
    public ResponseDto getPlaylistTracks(@PathVariable Long playlistId) {
        List<PlaylistItemDto> tracks = audioMediaBiz.getPlaylistTracks(playlistId);
        return ResponseDto.success(tracks);
    }

    @GetMapping("/audio/recent-plays")
    public ResponseDto getRecentPlays(@RequestParam(defaultValue = "20") Integer limit) {
        List<AudioListDto> tracks = audioMediaBiz.getRecentPlays(limit);
        return ResponseDto.success(tracks);
    }
}
