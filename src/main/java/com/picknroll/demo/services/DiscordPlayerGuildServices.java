package com.picknroll.demo.services;

import com.picknroll.demo.repositories.DiscordChannelRepository;
import com.picknroll.demo.repositories.DiscordPlayerGuildRepository;
import com.picknroll.demo.utils.Utils;
import org.springframework.stereotype.Service;

@Service
public class DiscordPlayerGuildServices {

    private final DiscordPlayerGuildRepository discordPlayerGuildRepository;
    DiscordChannelRepository discordChannelRepository;

    public DiscordPlayerGuildServices(DiscordPlayerGuildRepository discordPlayerGuildRepository, DiscordChannelRepository discordChannelRepository) {
        this.discordPlayerGuildRepository = discordPlayerGuildRepository;
        this.discordChannelRepository = discordChannelRepository;
    }

    public void insertGuildForPlayerId(String discordPlayerId, String guildId) {
        discordPlayerGuildRepository.insertGuildForPlayerId(discordPlayerId, guildId);
    }

    public void insertChannelForDate(String channelId, String guildId) {
        discordChannelRepository.insertChannelForDate(channelId, Utils.getCaliforniaDate(), guildId);
    }
}