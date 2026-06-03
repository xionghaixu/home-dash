package com.hd.controller;

import com.hd.biz.AudioMediaBiz;
import com.hd.model.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 音频媒体控制器
 *
 * @author xhx
 * @since 2026-04-26
 */
@RestController
@RequestMapping("/v1/media")
@RequiredArgsConstructor
public class AudioMediaController {

    private final AudioMediaBiz audioMediaBiz;

    @GetMapping("/audio")
    public ResponseEntity<ResponseDTO> getAudioList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "30") Integer pageSize,
            @RequestParam(defaultValue = "title") String sortBy,
            @RequestParam(defaultValue = "asc") String sortOrder,
            @RequestParam(required = false) String album,
            @RequestParam(required = false) String artist,
            @RequestParam(required = false) String genre) {
        PageResponseDTO<AudioListDTO> result = audioMediaBiz.getAudioList(page, pageSize, sortBy, sortOrder, album, artist, genre);
        return ResponseEntity.ok(ResponseDTO.success(result));
    }

    @GetMapping("/audio/{fileId}")
    public ResponseEntity<ResponseDTO> getAudioDetail(@PathVariable Long fileId) {
        AudioDetailDTO detail = audioMediaBiz.getAudioDetail(fileId);
        return ResponseEntity.ok(ResponseDTO.success(detail));
    }

    @GetMapping("/albums/audio")
    public ResponseEntity<ResponseDTO> getAudioAlbumList() {
        List<AudioAlbumDTO> albums = audioMediaBiz.getAlbumList();
        return ResponseEntity.ok(ResponseDTO.success(albums));
    }

    @GetMapping("/albums/audio/{album}/tracks")
    public ResponseEntity<ResponseDTO> getAlbumTracks(@PathVariable String album) {
        List<AudioListDTO> tracks = audioMediaBiz.getAlbumTracks(album);
        return ResponseEntity.ok(ResponseDTO.success(tracks));
    }

    @GetMapping("/artists")
    public ResponseEntity<ResponseDTO> getArtistList() {
        List<AudioArtistDTO> artists = audioMediaBiz.getArtistList();
        return ResponseEntity.ok(ResponseDTO.success(artists));
    }

    @GetMapping("/artists/{artist}/tracks")
    public ResponseEntity<ResponseDTO> getArtistTracks(@PathVariable String artist) {
        List<AudioListDTO> tracks = audioMediaBiz.getArtistTracks(artist);
        return ResponseEntity.ok(ResponseDTO.success(tracks));
    }

    @GetMapping("/playlists")
    public ResponseEntity<ResponseDTO> getPlaylistList() {
        List<AudioPlaylistDTO> playlists = audioMediaBiz.getPlaylistList();
        return ResponseEntity.ok(ResponseDTO.success(playlists));
    }

    @PostMapping("/playlists")
    public ResponseEntity<ResponseDTO> createPlaylist(@RequestBody AudioPlaylistDTO playlistDto) {
        AudioPlaylistDTO playlist = audioMediaBiz.createPlaylist(playlistDto.getPlaylistName(), playlistDto.getDescription());
        return ResponseEntity.ok(ResponseDTO.success(playlist));
    }

    @PostMapping("/playlists/{playlistId}/update")
    public ResponseEntity<ResponseDTO> updatePlaylist(@PathVariable Long playlistId, @RequestBody AudioPlaylistDTO playlistDto) {
        audioMediaBiz.updatePlaylist(playlistId, playlistDto.getPlaylistName(), playlistDto.getDescription());
        return ResponseEntity.ok(ResponseDTO.success());
    }

    @PostMapping("/playlists/{playlistId}/delete")
    public ResponseEntity<ResponseDTO> deletePlaylist(@PathVariable Long playlistId) {
        audioMediaBiz.deletePlaylist(playlistId);
        return ResponseEntity.ok(ResponseDTO.success());
    }

    @PostMapping("/playlists/{playlistId}/tracks")
    public ResponseEntity<ResponseDTO> addTrackToPlaylist(@PathVariable Long playlistId, @RequestParam Long fileId) {
        audioMediaBiz.addTrackToPlaylist(playlistId, fileId);
        return ResponseEntity.ok(ResponseDTO.success());
    }

    @PostMapping("/playlists/{playlistId}/tracks/{fileId}")
    public ResponseEntity<ResponseDTO> removeTrackFromPlaylist(@PathVariable Long playlistId, @PathVariable Long fileId) {
        audioMediaBiz.removeTrackFromPlaylist(playlistId, fileId);
        return ResponseEntity.ok(ResponseDTO.success());
    }

    @PostMapping("/playlists/{playlistId}/tracks/reorder")
    public ResponseEntity<ResponseDTO> reorderPlaylistTracks(@PathVariable Long playlistId, @RequestBody List<Long> fileIds) {
        audioMediaBiz.reorderPlaylistTracks(playlistId, fileIds);
        return ResponseEntity.ok(ResponseDTO.success());
    }

    @GetMapping("/playlists/{playlistId}/tracks")
    public ResponseEntity<ResponseDTO> getPlaylistTracks(@PathVariable Long playlistId) {
        List<PlaylistItemDTO> tracks = audioMediaBiz.getPlaylistTracks(playlistId);
        return ResponseEntity.ok(ResponseDTO.success(tracks));
    }

    @GetMapping("/audio/recent-plays")
    public ResponseEntity<ResponseDTO> getRecentPlays(@RequestParam(defaultValue = "20") Integer limit) {
        List<AudioListDTO> tracks = audioMediaBiz.getRecentPlays(limit);
        return ResponseEntity.ok(ResponseDTO.success(tracks));
    }
}
