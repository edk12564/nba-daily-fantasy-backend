package com.bigschlong.demo.controllers;

import com.bigschlong.demo.models.DiscordPlayer;
import com.bigschlong.demo.repositories.DiscordPlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("discord-players")
public class DiscordPlayerController {
    
    @Autowired
    DiscordPlayerRepository discordPlayerRepository;
    
    @GetMapping(value = "/{id}", produces = "application/json")
    public Optional<DiscordPlayer> getPlayer(@PathVariable String id)
    {
        var player =  discordPlayerRepository.findById(UUID.fromString(id));
        
        return player;
    }
}
