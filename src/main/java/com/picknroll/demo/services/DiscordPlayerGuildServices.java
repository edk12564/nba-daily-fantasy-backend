package com.picknroll.demo.services;

import com.picknroll.demo.repositories.DiscordPlayerGuildRepository;
import org.springframework.stereotype.Service;

@Service
public class DiscordPlayerGuildServices {

    private final DiscordPlayerGuildRepository discordPlayerGuildRepository;

    public DiscordPlayerGuildServices(DiscordPlayerGuildRepository discordPlayerGuildRepository) {
        this.discordPlayerGuildRepository = discordPlayerGuildRepository;
    }

    public void insertGuildForPlayerId(String discordPlayerId, String guildId) {
        discordPlayerGuildRepository.insertGuildForPlayerId(discordPlayerId, guildId);
    }

}