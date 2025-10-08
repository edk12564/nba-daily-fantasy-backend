package com.picknroll.demo.services;

import com.picknroll.demo.models.dtos.NbaPlayer;
import com.picknroll.demo.repositories.DiscordPlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DiscordPlayerServices {

    private final DiscordPlayerRepository discordPlayerRepository;

    public DiscordPlayerServices(DiscordPlayerRepository discordPlayerRepository) {
        this.discordPlayerRepository = discordPlayerRepository;
    }

    @Transactional(readOnly = true)
    public List<NbaPlayer> getRosterByDiscordId(UUID discordId) {
        return discordPlayerRepository.getRosterByDiscordId(discordId);
    }


}

