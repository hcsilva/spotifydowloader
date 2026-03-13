package com.hs.spotifydownloader.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class YoutubeDownloadService {

    private static final Logger log = LoggerFactory.getLogger(YoutubeDownloadService.class);

    private String ytDlpPath;

    public boolean baixarMusica(String artistAndTrack) {
        return baixarMusica(artistAndTrack, "C:\\musicas");
    }

    public boolean baixarMusica(String artistAndTrack, String destinationFolder) {
        try {
            if (ytDlpPath == null) {
                ytDlpPath = findYtDlp();

                if (ytDlpPath == null) {
                    log.info("yt-dlp não encontrado. Tentando instalar...");
                    instalarYtdlp();
                    ytDlpPath = findYtDlp();

                    if (ytDlpPath == null) {
                        log.error("Não foi possível instalar ou encontrar yt-dlp");
                        return false;
                    }
                }
            }

            log.info("Usando yt-dlp em: {}", ytDlpPath);
            return downloadTrack(artistAndTrack, destinationFolder);

        } catch (Exception e) {
            log.error("Erro ao baixar música: {}", e.getMessage(), e);
            return false;
        }
    }

    private String findYtDlp() {
        String[] possiblePaths = {
                "yt-dlp", // In PATH
                "C:\\tools\\yt-dlp.exe", // Instalação customizada
                "/usr/local/bin/yt-dlp", // Linux/Mac
                "/usr/bin/yt-dlp", // Linux
                "C:\\Program Files\\yt-dlp\\yt-dlp.exe", // Windows default
                System.getProperty("user.home") + "/bin/yt-dlp"
        };

        for (String path : possiblePaths) {
            if (isExecutableAvailable(path)) {
                log.info("yt-dlp encontrado em: {}", path);
                return path;
            }
        }

        log.warn("yt-dlp não encontrado em nenhum caminho padrão");
        return null;
    }

    private boolean isExecutableAvailable(String command) {
        try {
            Process process = new ProcessBuilder(command, "--version")
                    .redirectErrorStream(true)
                    .start();

            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean downloadTrack(String artistAndTrack, String destinationFolder) {
        try {
            Path musicasDir = Paths.get(destinationFolder);
            Files.createDirectories(musicasDir);

            String fileName = artistAndTrack
                    .replaceAll("[/\\\\:*?\"<>|]", "-")
                    .trim();

            log.info("Baixando: {}", artistAndTrack);

            ProcessBuilder processBuilder = new ProcessBuilder(
                    ytDlpPath,
                    "ytsearch:" + artistAndTrack,
                    "-x", // Extrair apenas o áudio
                    "--audio-format", "mp3", // Forçar formato MP3
                    "--audio-quality", "0", // 0 é a melhor qualidade no VBR do ffmpeg (geralmente ~320kbps)
                    // Garantir que baixe o mlehor áudio disponível do youtube antes de converter
                    "-f", "bestaudio/best",
                    "-o", musicasDir.resolve(fileName + ".%(ext)s").toString(),
                    "--add-metadata", // Adiciona artista/título no MP3
                    "--no-playlist", // Don't download playlists
                    "--progress", // Show progress
                    "--newline" // New line per progress update
            );

            processBuilder.directory(musicasDir.toFile());
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            // Lê a saída do processo
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("yt-dlp: {}", line);
                }
            }

            int exitCode = process.waitFor();
            log.info("Exit code: {}", exitCode);

            boolean arquivoExiste = Files.exists(musicasDir);
            boolean success = (exitCode == 0 || exitCode == 1) && arquivoExiste;

            if (success) {
                log.info("✓ Download concluído: {}", musicasDir);
            } else {
                log.error("✗ Falha no download. Exit code: {}, Arquivo existe: {}", exitCode, arquivoExiste);
            }

            return success;

        } catch (IOException | InterruptedException e) {
            log.error("Erro durante o download: {}", e.getMessage(), e);
            Thread.currentThread().interrupt(); // Restaura status de interrupção
            return false;
        }
    }

    private void instalarYtdlp() throws IOException, InterruptedException {
        log.info("Instalando yt-dlp em C:\\tools...");

        Path toolsDir = Path.of("C:/tools");
        Files.createDirectories(toolsDir);

        Path ytDlpExe = toolsDir.resolve("yt-dlp.exe");

        if (Files.exists(ytDlpExe)) {
            log.info("yt-dlp.exe já existe em C:\\tools");
            return;
        }

        ProcessBuilder pb = new ProcessBuilder(
                "powershell",
                "-Command",
                "Invoke-WebRequest https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp.exe -OutFile C:\\tools\\yt-dlp.exe");

        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("PowerShell: {}", line);
            }
        }

        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new IllegalStateException("Falha ao instalar yt-dlp. Exit code: " + exitCode);
        }

        log.info("yt-dlp instalado com sucesso em C:\\tools\\yt-dlp.exe");
    }
}
