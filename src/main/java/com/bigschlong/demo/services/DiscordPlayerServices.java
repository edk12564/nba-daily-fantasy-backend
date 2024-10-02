package com.bigschlong.demo.services;

import com.bigschlong.demo.models.DiscordPlayer;
import com.bigschlong.demo.models.NbaPlayer;
import com.bigschlong.demo.repositories.DiscordPlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DiscordPlayerServices {

    @Autowired
    private DiscordPlayerRepository discordPlayerRepository;
    
    public List<NbaPlayer> getRosterByDiscordId(UUID discordId) {
        return discordPlayerRepository.getRosterByDiscordId(discordId);
    }
    
    public Optional<DiscordPlayer> findById(UUID id) {
        return discordPlayerRepository.findById(id);
    }
    
//    public DiscordPlayerServices(DiscordPlayerRepository discordPlayerRepository) {
//        this.discordPlayerRepository = discordPlayerRepository;
//    }
//
//    public List<NbaPlayer> getDailyRoster(UUID id) {
//        return discordPlayerRepository.getRosterByDiscordId(id);
//    }
    
    
}

