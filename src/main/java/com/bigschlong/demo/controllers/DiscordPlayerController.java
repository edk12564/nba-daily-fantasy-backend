package com.bigschlong.demo.controllers;

import com.bigschlong.demo.models.dtos.NbaPlayer;
import com.bigschlong.demo.services.DiscordPlayerServices;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("discord-players")
public class DiscordPlayerController {
    
    private final
    DiscordPlayerServices services;

    public DiscordPlayerController(DiscordPlayerServices services) {
        this.services = services;
    }

    @GetMapping(value="/{id}/roster", produces = "application/json")
    public List<NbaPlayer> getRosterByDiscordId(@PathVariable String id) {
        return services.getRosterByDiscordId(UUID.fromString(id));
    }
}
