package com.hs.spotifydownloader.dto;

import java.util.List;

public class PlaylistResponseDto {

    private String playlistName;
    private List<MusicDto> musicList;

    public String getPlaylistName() {
        return playlistName;
    }

    public void setPlaylistName(String playlistName) {
        this.playlistName = playlistName;
    }

    public List<MusicDto> getMusicList() {
        return musicList;
    }

    public void setMusicList(List<MusicDto> musicList) {
        this.musicList = musicList;
    }
}
