package com.picknroll.demo.services;

import com.picknroll.demo.models.joinTables.DailyRosterPlayer;
import com.picknroll.demo.repositories.DiscordPlayerGuildRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
public class DiscordPlayerGuildServices {
    
    private final DiscordPlayerGuildRepository discordPlayerGuildRepository;

    public DiscordPlayerGuildServices(DiscordPlayerGuildRepository discordPlayerGuildRepository) {
        this.discordPlayerGuildRepository = discordPlayerGuildRepository;
    }

    public void insertGuildForPlayerId(String guildId, String discordPlayerId) {
        discordPlayerGuildRepository.insertGuildForPlayerId(discordPlayerId, guildId);
    }

}