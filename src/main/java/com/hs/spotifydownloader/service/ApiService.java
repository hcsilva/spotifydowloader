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

    private final RestClient restClient;
    private final TokenStoreService tokenStore;

    public ApiService(RestClient restClient, TokenStoreService tokenStore) {
        this.restClient = restClient;
        this.tokenStore = tokenStore;
    }

    public void setCredenciais(String clientId, String clientSecret) {
        tokenStore.setCredenciais(clientId, clientSecret);
    }

    private String obterAccessToken() {
        if (!tokenStore.isAuthenticated()) {
            throw new IllegalStateException("Você precisa se autenticar no Spotify. Vá em Configurações e clique em Login.");
        }

        if (tokenStore.isTokenExpired()) {
            refreshAccessToken();
        }

        return tokenStore.getAccessToken();
    }

    private synchronized void refreshAccessToken() {
        String clientId = tokenStore.getClientId();
        String clientSecret = tokenStore.getClientSecret();
        String credentials = clientId + ":" + clientSecret;
        String basicAuth = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        String body = "grant_type=refresh_token" +
                "&refresh_token=" + tokenStore.getRefreshToken();

        try {
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
            int expiresIn = json.get("expires_in").asInt();

            String newRefreshToken = json.has("refresh_token") ? json.get("refresh_token").asString() : tokenStore.getRefreshToken();

            tokenStore.setTokens(accessToken, newRefreshToken, expiresIn);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao renovar token do Spotify: " + e.getMessage());
        }
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

        JsonNode tracksNode = jsonNode.path("items");
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
        for (JsonNode itemNode : items) {

            JsonNode track = itemNode.has("track") ? itemNode.path("track") : itemNode.path("item");

            if (track.isMissingNode() || track.isNull()) {
                continue;
            }

            MusicDto dto = new MusicDto();
            dto.setTrackName(track.path("name").asText());

            JsonNode artists = track.path("artists");
            if (artists.isArray() && !artists.isEmpty()) {
                dto.setArtistName(artists.get(0).path("name").asText());
            }

            musicList.add(dto);
        }
    }
}
