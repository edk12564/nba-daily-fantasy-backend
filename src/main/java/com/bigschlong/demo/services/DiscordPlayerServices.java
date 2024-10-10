package com.bigschlong.demo.services;

import com.bigschlong.demo.models.DiscordPlayer;
import com.bigschlong.demo.models.NbaPlayer;
import com.bigschlong.demo.repositories.DiscordPlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
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
    
    @Transactional(readOnly = true)
    public Optional<DiscordPlayer> findDiscordPlayerById(UUID id) {
        return discordPlayerRepository.findById(id);
    }
    
    @Transactional(readOnly = true)
    public List<DiscordPlayer> findAllDiscordPlayers() {
        return (List<DiscordPlayer>) discordPlayerRepository.findAll();
    }
    
    @Transactional(readOnly = true)
    public boolean existsById(UUID discordId) {
        return discordPlayerRepository.existsById(discordId);
    }
    
    @Transactional
    public DiscordPlayer saveDiscordPlayer(DiscordPlayer discordPlayer) {
        return discordPlayerRepository.save(discordPlayer);
    }
    
    @Transactional
    public void deleteDiscordPlayerById(UUID discordId) {
        discordPlayerRepository.deleteById(discordId);
    }
    

    
    
    
}

