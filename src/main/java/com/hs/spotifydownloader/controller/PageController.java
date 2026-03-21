package com.hs.spotifydownloader.controller;

import com.hs.spotifydownloader.dto.MusicDto;
import com.hs.spotifydownloader.dto.PlaylistResponseDto;
import com.hs.spotifydownloader.service.ApiService;
import com.hs.spotifydownloader.service.YoutubeDownloadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
public class PageController {

    private static final Logger log = LoggerFactory.getLogger(PageController.class);

    private static final Pattern PLAYLIST_ID_PATTERN =
            Pattern.compile("playlist/([a-zA-Z0-9]+)");

    private final ApiService apiService;
    private final YoutubeDownloadService youtubeDownloadService;

    public PageController(ApiService apiService, YoutubeDownloadService youtubeDownloadService) {
        this.apiService = apiService;
        this.youtubeDownloadService = youtubeDownloadService;
    }

    // ── Downloader ────────────────────────────────────────────────────────────

    @GetMapping({"/", "/downloader"})
    public String downloaderPage(
            @RequestParam(required = false) String playlistUrl,
            @RequestParam(required = false, defaultValue = "C:\\musicas") String folder,
            Model model) {

        model.addAttribute("activePage", "downloader");
        model.addAttribute("pageTitle", "Download de Músicas");
        model.addAttribute("playlistUrl", playlistUrl);
        model.addAttribute("folder", folder);

        if (playlistUrl != null && !playlistUrl.isBlank()) {
            String playlistId = extrairIdPlaylist(playlistUrl);
            if (playlistId == null) {
                model.addAttribute("erro", "URL de playlist inválida. Certifique-se de usar um link do tipo: https://open.spotify.com/playlist/...");
                return "downloader";
            }
            try {
                PlaylistResponseDto playlist = apiService.carregarPlaylist(playlistId);
                model.addAttribute("playlist", playlist);
                model.addAttribute("selectedAll", false);
            } catch (IllegalStateException e) {
                model.addAttribute("erro", e.getMessage());
            } catch (Exception e) {
                log.error("Erro ao buscar playlist {}: {}", playlistId, e.getMessage(), e);
                model.addAttribute("erro", "Erro ao buscar playlist: " + e.getMessage());
            }
        }

        return "downloader";
    }

    @PostMapping("/downloader/download")
    public String download(
            @RequestParam(required = false) List<String> selectedTracks,
            @RequestParam(required = false, defaultValue = "C:\\musicas") String folder,
            Model model) {

        model.addAttribute("activePage", "downloader");
        model.addAttribute("pageTitle", "Download de Músicas");
        model.addAttribute("folder", folder);

        if (selectedTracks == null || selectedTracks.isEmpty()) {
            model.addAttribute("erro", "Nenhuma música selecionada para download.");
            return "downloader";
        }

        List<MusicDto> tracks = new ArrayList<>();
        for (String value : selectedTracks) {
            String[] parts = value.split("\\|", 2);
            if (parts.length == 2) {
                MusicDto dto = new MusicDto();
                dto.setArtistName(parts[0]);
                dto.setTrackName(parts[1]);
                tracks.add(dto);
            }
        }

        int sucesso = 0;
        int falhas = 0;
        for (MusicDto music : tracks) {
            String query = music.getArtistName() + " " + music.getTrackName();
            boolean ok = youtubeDownloadService.baixarMusica(query, folder);
            if (ok) sucesso++; else falhas++;
        }

        model.addAttribute("mensagem",
                "Download concluído! " + sucesso + " músicas baixadas com sucesso" +
                (falhas > 0 ? ", " + falhas + " com falha." : "."));

        return "downloader";
    }

    // ── Configurações ─────────────────────────────────────────────────────────

    @GetMapping("/configuracoes")
    public String configuracoesPage(Model model) {
        model.addAttribute("activePage", "configuracoes");
        model.addAttribute("pageTitle", "Configurações");
        return "configuracoes";
    }

    @PostMapping("/configuracoes")
    public String salvarConfiguracoes(
            @RequestParam String clientId,
            @RequestParam String clientSecret,
            Model model) {

        model.addAttribute("activePage", "configuracoes");
        model.addAttribute("pageTitle", "Configurações");

        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            model.addAttribute("erro", "Client ID e Client Secret são obrigatórios.");
            return "configuracoes";
        }

        apiService.setCredenciais(clientId.trim(), clientSecret.trim());
        model.addAttribute("mensagem", "Credenciais salvas com sucesso!");
        return "configuracoes";
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String extrairIdPlaylist(String url) {
        if (url == null) return null;
        Matcher matcher = PLAYLIST_ID_PATTERN.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        // If user typed just the ID directly
        if (url.matches("[a-zA-Z0-9]{22}")) {
            return url;
        }
        return null;
    }
}
