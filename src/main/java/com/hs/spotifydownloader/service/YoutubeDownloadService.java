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

    /**
     * Resolve o diretório onde o .jar está localizado em disco.
     *
     * Quando rodando dentro de um .jar empacotado (jpackage), o classloader
     * retorna uma URI do tipo "jar:file:/C:/.../" que não pode ser convertida
     * diretamente para File. Por isso usamos a propriedade de sistema
     * "java.class.path" como fallback, que aponta para o .jar real no disco.
     */
    private Path resolveAppDirectory() {
        // 1. Tenta via java.class.path — mais confiável dentro de .jar empacotado
        String classpath = System.getProperty("java.class.path");
        if (classpath != null && !classpath.isEmpty()) {
            // classpath pode ter múltiplas entradas separadas por ";"
            String firstEntry = classpath.split(";")[0].trim();
            Path cpPath = Paths.get(firstEntry).toAbsolutePath();
            Path cpDir = Files.isRegularFile(cpPath) ? cpPath.getParent() : cpPath;
            if (cpDir != null && Files.exists(cpDir)) {
                log.info("Diretório da aplicação resolvido via classpath: {}", cpDir);
                return cpDir;
            }
        }

        // 2. Fallback: tenta via ProtectionDomain (funciona na IDE / classes explodidas)
        try {
            java.net.URL location = YoutubeDownloadService.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation();

            // Converte "jar:file:/C:/..." ou "file:/C:/..." para Path
            String urlStr = location.toString();
            if (urlStr.startsWith("jar:")) {
                urlStr = urlStr.substring(4); // remove "jar:"
            }
            if (urlStr.contains("!")) {
                urlStr = urlStr.substring(0, urlStr.indexOf("!")); // remove "!/BOOT-INF/..."
            }

            Path locPath = Paths.get(new java.net.URI(urlStr)).toAbsolutePath();
            Path locDir = Files.isRegularFile(locPath) ? locPath.getParent() : locPath;
            log.info("Diretório da aplicação resolvido via ProtectionDomain: {}", locDir);
            return locDir;

        } catch (Exception e) {
            log.warn("Não foi possível resolver diretório via ProtectionDomain: {}", e.getMessage());
        }

        // 3. Último recurso: diretório de trabalho atual
        Path fallback = Paths.get("").toAbsolutePath();
        log.warn("Usando diretório de trabalho como fallback: {}", fallback);
        return fallback;
    }

    /**
     * Procura o ffmpeg.exe em múltiplos locais, em ordem de prioridade:
     *
     * Estrutura jpackage instalado no cliente:
     *   C:\Program Files\SpotifyDownloader\
     *     ├── SpotifyDownloader.exe
     *     ├── ffmpeg\          ← --app-content coloca aqui (pasta pai de app\)
     *     ├── app\             ← .jar roda daqui (appDir)
     *     └── runtime\
     *
     * 1. Pasta pai de appDir + ffmpeg\  (instalação jpackage - caso do cliente)
     * 2. appDir + ffmpeg\               (caso alternativo)
     * 3. appDir + ffmpeg\bin\           (ffmpeg com subpasta bin)
     * 4. target\ffmpeg\                 (desenvolvimento na IDE)
     */
    private String findFfmpeg() {
        Path appDir = resolveAppDirectory();
        Path parentDir = appDir.getParent(); // sobe um nível: app\ -> SpotifyDownloader\

        List<Path> candidates = new ArrayList<>();

        // 1. Pasta pai + ffmpeg (instalação jpackage: C:\Program Files\SpotifyDownloader\ffmpeg\)
        if (parentDir != null) {
            candidates.add(parentDir.resolve("ffmpeg").resolve("ffmpeg.exe"));
            candidates.add(parentDir.resolve("ffmpeg").resolve("bin").resolve("ffmpeg.exe"));
        }

        // 2. Ao lado do .jar (app\ffmpeg\)
        candidates.add(appDir.resolve("ffmpeg").resolve("ffmpeg.exe"));
        candidates.add(appDir.resolve("ffmpeg").resolve("bin").resolve("ffmpeg.exe"));

        // 3. ffmpeg.exe diretamente ao lado do .jar
        candidates.add(appDir.resolve("ffmpeg.exe"));

        // 4. Desenvolvimento na IDE (target\ffmpeg\)
        candidates.add(Paths.get("target", "ffmpeg", "ffmpeg.exe").toAbsolutePath());

        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                // yt-dlp espera o diretório onde está o ffmpeg.exe, não o .exe diretamente
                String ffmpegDir = candidate.getParent().toString();
                log.info("ffmpeg encontrado em: {} -> usando diretório: {}", candidate, ffmpegDir);
                return ffmpegDir;
            }
        }

        log.warn("ffmpeg não encontrado em nenhum caminho. Caminhos verificados:");
        candidates.forEach(c -> log.warn("  - {}", c));
        log.warn("O yt-dlp tentará usar o ffmpeg do PATH do sistema.");
        return null;
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

            // Resolve o ffmpeg dinamicamente em runtime
            String ffmpegDir = findFfmpeg();

            List<String> command = new ArrayList<>(Arrays.asList(
                    ytDlpPath,
                    "ytsearch1:" + artistAndTrack,
                    "--extract-audio",
                    "--audio-format", "mp3",
                    "--audio-quality", "0",
                    "-o", musicasDir.resolve(fileName + ".%(ext)s").toString(),
                    "--embed-metadata",
                    "--no-playlist",
                    "--progress",
                    "--newline"
            ));

            // Só adiciona --ffmpeg-location se encontrou o ffmpeg
            if (ffmpegDir != null) {
                command.add("--ffmpeg-location");
                command.add(ffmpegDir);
                log.info("Usando ffmpeg em: {}", ffmpegDir);
            } else {
                log.warn("Continuando sem --ffmpeg-location. A conversão para MP3 pode falhar.");
            }

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