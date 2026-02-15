package com.hs.spotifydownloader.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

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

        String accessToken = json.get("access_token").asText();
        carregarPlaylist(accessToken, idPlaylist);
        youtubeDownloadService.baixarMusica("Break on Through (To the Other Side) The Doors");
    }

    private void carregarPlaylist(String token, String idPlaylist) {

        var result = this.restClient.get()
                .uri("https://api.spotify.com/v1/playlists/" + idPlaylist)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .toEntity(String.class);

        String responsyBody = result.getBody();
      //  System.out.println(responsyBody);
    }

}
