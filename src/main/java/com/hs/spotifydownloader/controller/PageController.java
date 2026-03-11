package com.hs.spotifydownloader.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping({ "/", "/downloader" })
    public String downloaderPage(Model model) {
        model.addAttribute("activePage", "downloader");
        model.addAttribute("pageTitle", "Baixar Músicas");
        return "downloader";
    }

    @GetMapping("/configuracoes")
    public String configuracoesPage(Model model) {
        model.addAttribute("activePage", "configuracoes");
        model.addAttribute("pageTitle", "Configurações");
        return "configuracoes";
    }
}
