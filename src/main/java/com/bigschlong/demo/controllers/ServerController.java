package com.bigschlong.demo.controllers;

import com.bigschlong.demo.models.Server;
import com.bigschlong.demo.repositories.ServerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("servers")
public class ServerController {
    
    @Autowired
    ServerRepository serverRepository;
    
    @GetMapping(value = "/{id}", produces = "application/json")
    public Optional<Server> getServer(@PathVariable String id)
    {
        var server =  serverRepository.findById(UUID.fromString(id));
        
        return server;
    }
}