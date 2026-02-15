package com.hs.spotifydownloader.controller;

import com.hs.spotifydownloader.service.ApiService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api-spotify")
public class ApiController {

    private final ApiService apiService;

    public ApiController(ApiService apiService) {
        this.apiService = apiService;
    }

    @GetMapping(value = "/find/{id}", produces = "application/json")
    public String find(@PathVariable String id) {
        apiService.loadAccessToken(id);
        return "";
    }
}
