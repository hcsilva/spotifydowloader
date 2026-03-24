package com.hs.spotifydownloader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
public class SpotifydownloaderApplication {

	private static final Logger log = LoggerFactory.getLogger(SpotifydownloaderApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(SpotifydownloaderApplication.class, args);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void openBrowser() {
		try {
			String url = "http://localhost:8080";
			String os = System.getProperty("os.name").toLowerCase();
			if (os.contains("win")) {
				Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
			} else if (os.contains("mac")) {
				Runtime.getRuntime().exec("open " + url);
			} else if (os.contains("nix") || os.contains("nux")) {
				Runtime.getRuntime().exec("xdg-open " + url);
			}
			log.info("Navegador padrão aberto em {}", url);
		} catch (Exception e) {
			log.error("Não foi possível abrir o navegador automaticamente", e);
		}
	}
}
