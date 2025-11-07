package com.picknroll.demo.controllers;

import com.picknroll.demo.models.dtos.DiscordPlayerGuild;
import com.picknroll.demo.services.DiscordPlayerGuildServices;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("servers")
public class DiscordPlayerGuildController {
    
    private final
    DiscordPlayerGuildServices discordPlayerGuildServices;

    public DiscordPlayerGuildController(DiscordPlayerGuildServices discordPlayerGuildServices) {
        this.discordPlayerGuildServices = discordPlayerGuildServices;
    }
    
    @GetMapping(value = "/{id}/players", produces = "application/json")
    public List<String> getDiscordPlayersByServerId(@PathVariable String id)
    {
       return discordPlayerGuildServices.getDiscordPlayersByServerId(id);
    }
}