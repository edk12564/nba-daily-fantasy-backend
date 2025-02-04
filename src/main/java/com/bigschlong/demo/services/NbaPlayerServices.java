package com.bigschlong.demo.services;

import com.bigschlong.demo.models.dtos.NbaPlayer;
import com.bigschlong.demo.models.dtos.Team;
import com.bigschlong.demo.models.joinTables.NbaPlayerTeam;
import com.bigschlong.demo.repositories.NbaPlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class NbaPlayerServices {
    
    private final NbaPlayerRepository nbaPlayerRepository;

    public NbaPlayerServices(NbaPlayerRepository nbaPlayerRepository) {
        this.nbaPlayerRepository = nbaPlayerRepository;
    }

    @Transactional(readOnly = true)
    public List<Team> getTeamByPlayerUid(UUID playerUid) {
        return nbaPlayerRepository.getTeamByPlayerUid(playerUid);
    }

    @Transactional(readOnly = true)
    public NbaPlayer findNbaPlayerByUid(UUID uid) {
        return nbaPlayerRepository.findNbaPlayerByUid(uid);
    }

    @Transactional(readOnly = true)
    public NbaPlayer findNbaPlayerByName(String name) {
        return nbaPlayerRepository.findNbaPlayerByName(name);
    }

    @Transactional(readOnly = true)
    public List<NbaPlayerTeam> getNbaPlayersByPositionWithTeam(String position) {
        return getNbaPlayersByPositionWithTeam(position);
    }

//
//    @Transactional(readOnly = true)
//    public List<NbaPlayer> findAllNbaPlayers() {
//        return (List<NbaPlayer>) nbaPlayerRepository.findAll();
//    }
//
//    @Transactional(readOnly = true)
//    public boolean existsById(UUID playerId) {
//        return nbaPlayerRepository.existsById(playerId);
//    }
//
//    @Transactional
//    public NbaPlayer saveNbaPlayer(NbaPlayer nbaPlayer) {
//        return nbaPlayerRepository.save(nbaPlayer);
//    }
//
//    @Transactional
//    public void deleteNbaPlayerById(UUID playerId) {
//        nbaPlayerRepository.deleteById(playerId);
//    }

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
