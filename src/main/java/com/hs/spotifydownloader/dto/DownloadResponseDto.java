package com.hs.spotifydownloader.dto;

public class DownloadResponseDto {

    private int downloaded;
    private int failed;
    private String message;

    public DownloadResponseDto(int downloaded, int failed) {
        this.downloaded = downloaded;
        this.failed = failed;
        this.message = downloaded + " música(s) baixada(s) com sucesso" +
                (failed > 0 ? ", " + failed + " falha(s)" : "") + ".";
    }

    public int getDownloaded() {
        return downloaded;
    }

    public void setDownloaded(int downloaded) {
        this.downloaded = downloaded;
    }

    public int getFailed() {
        return failed;
    }

    public void setFailed(int failed) {
        this.failed = failed;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
