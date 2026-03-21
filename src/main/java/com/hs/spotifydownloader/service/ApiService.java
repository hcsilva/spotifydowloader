package com.hs.spotifydownloader.service;

import com.hs.spotifydownloader.dto.MusicDto;
import com.hs.spotifydownloader.dto.PlaylistResponseDto;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class ApiService {

    private String clientId;
    private String clientSecret;
    private final RestClient restClient;

    public ApiService(RestClient restClient) {
        this.restClient = restClient;
    }

    public void setCredenciais(String clientId, String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    private String obterAccessToken() {
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalStateException("Credenciais do Spotify não configuradas. Acesse a aba Configurações.");
        }

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
        return json.get("access_token").asString();
    }

    public PlaylistResponseDto carregarPlaylist(String idPlaylist) {
        String accessToken = obterAccessToken();
        return carregarPlaylistComToken(accessToken, idPlaylist);
    }

    private PlaylistResponseDto carregarPlaylistComToken(String token, String idPlaylist) {
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
