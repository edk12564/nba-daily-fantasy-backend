package com.bigschlong.demo.services;

import com.bigschlong.demo.models.dtos.NbaPlayer;
import com.bigschlong.demo.models.joinTables.NbaPlayerTeam;
import com.bigschlong.demo.repositories.NbaPlayerRepository;
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


    @Transactional(readOnly = true)
    public NbaPlayer findNbaPlayerByName(String name) {
        return nbaPlayerRepository.findNbaPlayerByName(name);
    }

    @Transactional(readOnly = true)
    public List<NbaPlayerTeam> getNbaPlayersWithTeam(LocalDate localDate) {
        List<NbaPlayerTeam> nbaPlayersWithTeam = nbaPlayerRepository.getNbaPlayersWithTeam(localDate.toString());
        return nbaPlayersWithTeam;
    }

    // we'll call the db to get todays players
    //
    public List<String> getTodaysNbaPlayersByPosition(String position) {
        return nbaPlayerRepository.getTodaysNbaPlayersByPosition(position).stream()
                .map(player -> player.getName() + "-" + player.getDollar_value().toString())
                .toList();
    }

    public List<NbaPlayer> getAllTodaysNbaPlayers() {
        return nbaPlayerRepository.getAllTodaysNbaPlayers();
    }
}
