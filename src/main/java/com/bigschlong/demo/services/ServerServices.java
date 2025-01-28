package com.bigschlong.demo.services;

import com.bigschlong.demo.models.dtos.DiscordPlayer;
import com.bigschlong.demo.models.dtos.Server;
import com.bigschlong.demo.repositories.ServerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ServerServices {
    
    private final ServerRepository serverRepository;

    public ServerServices(ServerRepository serverRepository) {
        this.serverRepository = serverRepository;
    }
    
    @Transactional(readOnly = true)
    public List<DiscordPlayer> getDiscordPlayersByServerId(UUID discordId) {
        return serverRepository.getDiscordPlayersByServerId(discordId);
    }
    
    @Transactional(readOnly = true)
    public Optional<Server> findServerById(UUID id)
    {
        return serverRepository.findById(id);
    }
    
    @Transactional(readOnly = true)
    public List<Server> findAllServers() {
        return (List<Server>) serverRepository.findAll();
    }
    
    @Transactional(readOnly = true)
    public boolean existsById(UUID serverId) {
        return serverRepository.existsById(serverId);
    }
    
    @Transactional
    public Server saveServer(Server server) {
        return serverRepository.save(server);
    }
    
    @Transactional
    public void deleteServerById(UUID serverId) {
        serverRepository.deleteById(serverId);
    }
    
}