package com.hs.spotifydownloader.service;

import org.springframework.stereotype.Service;
import java.time.Instant;

@Service
public class TokenStoreService {

    private String accessToken;
    private String refreshToken;
    private Instant expiresAt;
    private String clientId;
    private String clientSecret;

    public synchronized void setTokens(String accessToken, String refreshToken, int expiresIn) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresAt = Instant.now().plusSeconds(expiresIn - 60); // 1-minute buffer
    }

    public synchronized boolean isAuthenticated() {
        return accessToken != null;
    }

    public synchronized String getAccessToken() {
        return accessToken;
    }

    public synchronized String getRefreshToken() {
        return refreshToken;
    }

    public synchronized boolean isTokenExpired() {
        return expiresAt == null || Instant.now().isAfter(expiresAt);
    }

    public synchronized void clearTokens() {
        this.accessToken = null;
        this.refreshToken = null;
        this.expiresAt = null;
    }

    public void setCredenciais(String clientId, String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }
}
