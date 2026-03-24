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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class YoutubeDownloadService {

    private static final Logger log = LoggerFactory.getLogger(YoutubeDownloadService.class);
    private static final String TOOLS_DIR = "C:\\tools";
    private static final String FFMPEG_RELATIVE_PATH = "target/ffmpeg";

    private String ytDlpPath;

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
                "yt-dlp",
                TOOLS_DIR + "\\yt-dlp.exe",
                "/usr/local/bin/yt-dlp",
                "/usr/bin/yt-dlp",
                "C:\\Program Files\\yt-dlp\\yt-dlp.exe",
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
            return process.waitFor() == 0;
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

            String ffmpegPath = Paths.get(FFMPEG_RELATIVE_PATH).toAbsolutePath().toString();

            List<String> command = new ArrayList<>(Arrays.asList(
                    ytDlpPath,
                    "ytsearch1:" + artistAndTrack,
                    "--extract-audio",
                    "--audio-format", "mp3",
                    "--audio-quality", "0",
                    "-o", musicasDir.resolve(fileName + ".%(ext)s").toString(),
                    "--ffmpeg-location", ffmpegPath,
                    "--embed-metadata",
                    "--no-playlist",
                    "--progress",
                    "--newline"
            ));

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(musicasDir.toFile());
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("yt-dlp: {}", line);
                }
            }

            int exitCode = process.waitFor();
            log.info("Exit code: {}", exitCode);

            boolean success = (exitCode == 0 || exitCode == 1) && Files.exists(musicasDir);

            if (success) {
                log.info("✓ Download concluído: {}", musicasDir);
            } else {
                log.error("✗ Falha no download. Exit code: {}", exitCode);
            }

            return success;

        } catch (IOException | InterruptedException e) {
            log.error("Erro durante o download: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void instalarYtdlp() throws IOException, InterruptedException {
        log.info("Instalando yt-dlp em {}...", TOOLS_DIR);

        Path toolsDir = Path.of(TOOLS_DIR);
        Files.createDirectories(toolsDir);

        Path ytDlpExe = toolsDir.resolve("yt-dlp.exe");

        if (Files.exists(ytDlpExe)) {
            log.info("yt-dlp.exe já existe em {}", TOOLS_DIR);
            return;
        }

        ProcessBuilder pb = new ProcessBuilder(
                "powershell", "-Command",
                "Invoke-WebRequest https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp.exe " +
                        "-OutFile '" + ytDlpExe + "'"
        );
        executarComando(pb, "PowerShell (yt-dlp)");

        log.info("✓ yt-dlp instalado com sucesso em {}", ytDlpExe);
    }

    private void executarComando(ProcessBuilder pb, String label) throws IOException, InterruptedException {
        pb.redirectErrorStream(false);

        Process process = pb.start();

        Thread stdoutThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("{}: {}", label, line);
                }
            } catch (IOException e) {
                log.warn("Erro ao ler stdout de {}: {}", label, e.getMessage());
            }
        });

        Thread stderrThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.error("{} [STDERR]: {}", label, line);
                }
            } catch (IOException e) {
                log.warn("Erro ao ler stderr de {}: {}", label, e.getMessage());
            }
        });

        stdoutThread.start();
        stderrThread.start();

        int exitCode = process.waitFor();

        stdoutThread.join();
        stderrThread.join();

        if (exitCode != 0) {
            throw new IllegalStateException("Comando falhou [" + label + "]. Exit code: " + exitCode);
        }
    }
}