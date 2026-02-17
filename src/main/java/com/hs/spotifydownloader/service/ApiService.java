package com.hs.spotifydownloader.service;

import com.hs.spotifydownloader.dto.MusicDto;
import com.hs.spotifydownloader.dto.PlaylistResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.util.JSONPObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class ApiService {

    @Value("${api.client-id}")
    private String clientId;
    @Value("${api.client-secret}")
    private String clientSecret;
    private final RestClient restClient;
    private final YoutubeDownloadService youtubeDownloadService;

    public ApiService(RestClient restClient, YoutubeDownloadService youtubeDownloadService) {
        this.restClient = restClient;
        this.youtubeDownloadService = youtubeDownloadService;
    }

    public void loadAccessToken(String idPlaylist) {
        String body = "grant_type=client_credentials";
        String credentials = clientId + ":" + clientSecret;
        String basicAuth = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));


        var result = this.restClient.post()
                .uri("https://accounts.spotify.com/api/token")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + basicAuth)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .body(body)
                .retrieve()
                .toEntity(String.class);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(result.getBody());

        String accessToken = json.get("access_token").asString();
        PlaylistResponseDto playlistResponseDto = carregarPlaylist(accessToken, idPlaylist);

        for (MusicDto musicDto : playlistResponseDto.getMusicList()) {
            System.out.println(musicDto.getArtistName() + " "+ musicDto.getTrackName());
        }

         youtubeDownloadService.baixarMusica("Mr. Big To Be With You");
    }

    private PlaylistResponseDto carregarPlaylist(String token, String idPlaylist) {
        ObjectMapper mapper = new ObjectMapper();
        PlaylistResponseDto playlist = new PlaylistResponseDto();
        List<MusicDto> musicList = new ArrayList<>();

        var result = this.restClient.get()
                .uri("https://api.spotify.com/v1/playlists/" + idPlaylist)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .toEntity(String.class);

        JsonNode jsonNode = mapper.readTree(result.getBody());
        playlist.setPlaylistName(jsonNode.get("name").asString());

        JsonNode tracksNode = jsonNode.path("tracks");
        processarTracks(tracksNode.path("items"), musicList);

        String nextUrl = tracksNode.path("next").asString(null);

        while (nextUrl != null && !nextUrl.isEmpty()) {
            var nextResult = this.restClient.get()
                    .uri(nextUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .toEntity(String.class);

            JsonNode nextPage = mapper.readTree(nextResult.getBody());
            processarTracks(nextPage.path("items"), musicList);

            nextUrl = nextPage.path("next").asString(null);
            System.out.println(nextUrl);
        }

        playlist.setMusicList(musicList);

        return playlist;
    }

    private void processarTracks(JsonNode items, List<MusicDto> musicList) {
        for (JsonNode item : items) {
            JsonNode track = item.path("track");

            if (track.isMissingNode() || track.isNull()) {
                continue;
            }

            MusicDto musicDto = new MusicDto();
            musicDto.setTrackName(track.get("name").asString());

            JsonNode artists = track.path("artists");
            if (artists.isArray() && !artists.isEmpty()) {
                musicDto.setArtistName(artists.get(0).path("name").asString());
            }

            musicList.add(musicDto);
        }
    }
}
