package com.hs.spotifydownloader.controller;

import com.hs.spotifydownloader.service.TokenStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Controller
@RequestMapping("/auth")
public class SpotifyAuthController {
    private static final Logger log = LoggerFactory.getLogger(SpotifyAuthController.class);

    private final TokenStoreService tokenStore;
    private final RestClient restClient;

    @Value("${spotify.redirect-uri}")
    private String redirectUri;

    public SpotifyAuthController(TokenStoreService tokenStore, RestClient restClient) {
        this.tokenStore = tokenStore;
        this.restClient = restClient;
    }

    @GetMapping("/login")
    public String login() {
        String clientId = tokenStore.getClientId();
        if (clientId == null || clientId.isBlank()) {
            return "redirect:/configuracoes?erro=Configure o Client ID antes de logar.";
        }

        String scope = "playlist-read-private playlist-read-collaborative";
        String authUrl = "https://accounts.spotify.com/authorize?" +
                "response_type=code" +
                "&client_id=" + clientId +
                "&scope=" + URLEncoder.encode(scope, StandardCharsets.UTF_8) +
                "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);

        return "redirect:" + authUrl;
    }

    @GetMapping("/callback")
    public String callback(@RequestParam(required = false) String code,
                           @RequestParam(required = false) String error) {

        if (error != null) {
            log.error("Erro na autorização do Spotify: {}", error);
            return "redirect:/configuracoes?erro=Erro na autorização: " + error;
        }

        if (code == null) {
            return "redirect:/configuracoes?erro=Código de autorização não recebido.";
        }

        try {
            String clientId = tokenStore.getClientId();
            String clientSecret = tokenStore.getClientSecret();
            String credentials = clientId + ":" + clientSecret;
            String basicAuth = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

            String body = "grant_type=authorization_code" +
                    "&code=" + code +
                    "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);

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
            String refreshToken = json.get("refresh_token").asString();
            int expiresIn = json.get("expires_in").asInt();

            tokenStore.setTokens(accessToken, refreshToken, expiresIn);

            log.info("Autenticação via OAuth2 realizada com sucesso.");
            return "redirect:/configuracoes?mensagem=Conectado ao Spotify com sucesso!";

        } catch (Exception e) {
            log.error("Erro ao trocar código por token: {}", e.getMessage(), e);
            return "redirect:/configuracoes?erro=Erro ao autenticar: " + e.getMessage();
        }
    }

    @PostMapping("/logout")
    public String logout() {
        tokenStore.clearTokens();
        return "redirect:/configuracoes?mensagem=Desconectado com sucesso.";
    }
}
