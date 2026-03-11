package com.hs.spotifydownloader.dto;

import java.util.List;

public class DownloadRequestDto {

    private List<MusicDto> tracks;
    private String destinationFolder;

    public List<MusicDto> getTracks() {
        return tracks;
    }

    public void setTracks(List<MusicDto> tracks) {
        this.tracks = tracks;
    }

    public String getDestinationFolder() {
        return destinationFolder;
    }

    public void setDestinationFolder(String destinationFolder) {
        this.destinationFolder = destinationFolder;
    }
}
