package com.bigschlong.demo.services;

import com.bigschlong.demo.models.NbaPlayer;
import com.bigschlong.demo.models.Team;
import com.bigschlong.demo.repositories.NbaPlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class NbaPlayerServices {
    
    @Autowired
    private NbaPlayerRepository nbaPlayerRepository;
    
    public List<Team> getTeamByPlayerId(UUID discordId) {
        return nbaPlayerRepository.getTeamByPlayerId(discordId);
    }
    
    public Optional<NbaPlayer> findById(UUID id) {
        return nbaPlayerRepository.findById(id);
    
    }
}
