package com.hs.spotifydownloader.controller;

import com.hs.spotifydownloader.dto.DownloadRequestDto;
import com.hs.spotifydownloader.dto.DownloadResponseDto;
import com.hs.spotifydownloader.dto.MusicDto;
import com.hs.spotifydownloader.dto.PlaylistResponseDto;
import com.hs.spotifydownloader.service.ApiService;
import com.hs.spotifydownloader.service.YoutubeDownloadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    private static final Logger log = LoggerFactory.getLogger(ApiController.class);

    private final ApiService apiService;
    private final YoutubeDownloadService youtubeDownloadService;

    public ApiController(ApiService apiService, YoutubeDownloadService youtubeDownloadService) {
        this.apiService = apiService;
        this.youtubeDownloadService = youtubeDownloadService;
    }

    /**
     * Busca as músicas de uma playlist do Spotify pelo ID.
     */
    @GetMapping("/playlist/{id}")
    public ResponseEntity<?> buscarPlaylist(@PathVariable String id) {
        try {
            PlaylistResponseDto playlist = apiService.carregarPlaylist(id);
            return ResponseEntity.ok(playlist);
        } catch (Exception e) {
            log.error("Erro ao buscar playlist {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body("Erro ao buscar playlist: " + e.getMessage());
        }
    }

    @PostMapping("/download")
    public ResponseEntity<?> download(@RequestBody DownloadRequestDto request) {
        if (request.getTracks() == null || request.getTracks().isEmpty()) {
            return ResponseEntity.badRequest().body("Nenhuma música selecionada.");
        }

        String pasta = request.getDestinationFolder();
        if (pasta == null || pasta.isBlank()) {
            pasta = "C:\\musicas";
        }

        int downloaded = 0;
        int failed = 0;

        for (MusicDto music : request.getTracks()) {
            String artistAndTrack = music.getArtistName() + " " + music.getTrackName();
            log.info("Iniciando download: {} -> {}", artistAndTrack, pasta);

            boolean success = youtubeDownloadService.baixarMusica(artistAndTrack, pasta);
            if (success) {
                downloaded++;
            } else {
                failed++;
                log.warn("Falha ao baixar: {}", artistAndTrack);
            }
        }

        return ResponseEntity.ok(new DownloadResponseDto(downloaded, failed));
    }

    /**
     * Salva as configurações gerais (persistência em memória por enquanto).
     */
    @PostMapping("/configuracoes")
    public ResponseEntity<?> salvarConfiguracoes(@RequestBody Map<String, Object> config) {
        log.info("Configurações recebidas: {}", config);
        // TODO: persist to file/db
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    /**
     * Salva credenciais (demonstração — em produção use variáveis de ambiente).
     */
    @PostMapping("/credenciais")
    public ResponseEntity<?> salvarCredenciais(@RequestBody Map<String, String> creds) {
        log.info("Credenciais recebidas para atualização.");
        String clientId = creds.get("clientId");
        String clientSecret = creds.get("clientSecret");

        if (clientId != null && clientSecret != null) {
            apiService.setCredenciais(clientId, clientSecret);
            return ResponseEntity.ok(Map.of("status", "ok"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Credenciais incompletas"));
        }
    }
}
