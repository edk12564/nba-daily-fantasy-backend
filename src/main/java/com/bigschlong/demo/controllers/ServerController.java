package com.bigschlong.demo.controllers;

import com.bigschlong.demo.models.DiscordPlayer;
import com.bigschlong.demo.models.Server;
import com.bigschlong.demo.services.ServerServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("servers")
public class ServerController {
    
    @Autowired
    ServerServices serverServices;
    
    @GetMapping(value = "/{id}", produces = "application/json")
    public Optional<Server> getServer(@PathVariable String id)
    {
        
        return serverServices.findById(UUID.fromString(id));
    
    }
    
    @GetMapping(value = "/{id}/players", produces = "application/json")
    public List<DiscordPlayer> getDiscordPlayersByServerId(@PathVariable String id)
    {
       
       return serverServices.getDiscordPlayersByServerId(UUID.fromString(id));
       
    }
}