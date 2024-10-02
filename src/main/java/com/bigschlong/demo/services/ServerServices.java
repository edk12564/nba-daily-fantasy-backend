package com.bigschlong.demo.services;

import com.bigschlong.demo.models.DiscordPlayer;
import com.bigschlong.demo.models.Server;
import com.bigschlong.demo.repositories.ServerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ServerServices {
    
    @Autowired
    private ServerRepository serverRepository;
    
    public List<DiscordPlayer> getDiscordPlayersByServerId(UUID discordId) {
        return serverRepository.getDiscordPlayersByServerId(discordId);
    }
    
    public Optional<Server> findById(UUID id)
    {
        
        return serverRepository.findById(id);
    
    }
}