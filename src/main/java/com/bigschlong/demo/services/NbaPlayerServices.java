package com.bigschlong.demo.services;

import com.bigschlong.demo.models.NbaPlayer;
import com.bigschlong.demo.models.Team;
import com.bigschlong.demo.repositories.NbaPlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class NbaPlayerServices {
    
    private final NbaPlayerRepository nbaPlayerRepository;

    public NbaPlayerServices(NbaPlayerRepository nbaPlayerRepository) {
        this.nbaPlayerRepository = nbaPlayerRepository;
    }

    @Transactional(readOnly = true)
    public List<Team> getTeamByPlayerId(UUID discordId) {
        return nbaPlayerRepository.getTeamByPlayerId(discordId);
    }
    
    @Transactional(readOnly = true)
    public Optional<NbaPlayer> findNbaPlayerById(UUID id) {
        return nbaPlayerRepository.findById(id);
    }
    
    @Transactional(readOnly = true)
    public List<NbaPlayer> findAllNbaPlayers() {
        return (List<NbaPlayer>) nbaPlayerRepository.findAll();
    }
    
    @Transactional(readOnly = true)
    public boolean existsById(UUID playerId) {
        return nbaPlayerRepository.existsById(playerId);
    }
    
    @Transactional
    public NbaPlayer saveNbaPlayer(NbaPlayer nbaPlayer) {
        return nbaPlayerRepository.save(nbaPlayer);
    }
    
    @Transactional
    public void deleteNbaPlayerById(UUID playerId) {
        nbaPlayerRepository.deleteById(playerId);
    }

    // we'll call the db to get todays players
    //
    public getTodaysNbaPlayersByPosition(String value) {

    }
}
