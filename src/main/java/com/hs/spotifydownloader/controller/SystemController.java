package com.hs.spotifydownloader.controller;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class SystemController {

    private final ApplicationContext applicationContext;

    public SystemController(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @PostMapping("/shutdown")
    public String shutdown(Model model) {
        Thread shutdownThread = new Thread(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            int exitCode = SpringApplication.exit(applicationContext, () -> 0);
            System.exit(exitCode);
        });
        shutdownThread.setDaemon(false);
        shutdownThread.start();
        
        return "shutdown";
    }
}
