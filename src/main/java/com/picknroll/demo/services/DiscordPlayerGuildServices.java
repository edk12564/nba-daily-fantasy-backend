package com.picknroll.demo.services;

import com.picknroll.demo.models.dtos.DiscordPlayerGuild;
import com.picknroll.demo.repositories.DiscordPlayerGuildRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DiscordPlayerGuildServices {
    
    private final DiscordPlayerGuildRepository discordPlayerGuildRepository;

    public DiscordPlayerGuildServices(DiscordPlayerGuildRepository discordPlayerGuildRepository) {
        this.discordPlayerGuildRepository = discordPlayerGuildRepository;
    }
    
    @Transactional(readOnly = true)
    public List<String> getDiscordPlayersByServerId(String discordId) {
        return discordPlayerGuildRepository.getDiscordPlayersByGuildId(discordId);
    }
    
    @Transactional(readOnly = true)
    public List<DiscordPlayerGuild> findAllServers() {
        return (List<DiscordPlayerGuild>) discordPlayerGuildRepository.findAll();
    }
    
    @Transactional(readOnly = true)
    public boolean existsById(String serverId) {
        return discordPlayerGuildRepository.existsById(serverId);
    }
    
    @Transactional
    public DiscordPlayerGuild saveServer(DiscordPlayerGuild discordPlayerGuild) {
        return discordPlayerGuildRepository.save(discordPlayerGuild);
    }
    
    @Transactional
    public void deleteServerById(String serverId) {
        discordPlayerGuildRepository.deleteById(serverId);
    }
    
}