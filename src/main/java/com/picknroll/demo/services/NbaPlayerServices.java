package com.picknroll.demo.services;

import com.picknroll.demo.models.dtos.NbaPlayer;
import com.picknroll.demo.models.joinTables.NbaPlayerTeam;
import com.picknroll.demo.repositories.NbaPlayerRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class NbaPlayerServices {

    private final NbaPlayerRepository nbaPlayerRepository;

    public NbaPlayerServices(NbaPlayerRepository nbaPlayerRepository) {
        this.nbaPlayerRepository = nbaPlayerRepository;
    }

    /* Get NBA Players */
    @Transactional(readOnly = true)
    public NbaPlayer findNbaPlayerByName(String name) {
        return nbaPlayerRepository.findNbaPlayerByName(name);
    }


    // With Team Today
    @Cacheable(cacheNames = "todaysPlayers", key = "#localDate.toString()")
    @Transactional(readOnly = true)
    public List<NbaPlayerTeam> getNbaPlayersWithTeam(LocalDate localDate) {
        return nbaPlayerRepository.getNbaPlayersWithTeam(localDate.toString());
    }

    // All NBA Players Today
    public List<NbaPlayer> getAllTodaysNbaPlayers() {
        return nbaPlayerRepository.getAllTodaysNbaPlayers();
    }
}
