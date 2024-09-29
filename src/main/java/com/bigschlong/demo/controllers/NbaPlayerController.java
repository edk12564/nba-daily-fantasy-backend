package com.bigschlong.demo.controllers;

import com.bigschlong.demo.models.NbaPlayer;
import com.bigschlong.demo.repositories.NbaPlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("nba-players")
public class NbaPlayerController {

    @Autowired
    NbaPlayerRepository nbaPlayerRepository;

    @GetMapping(value = "/{id}", produces = "application/json")
    public Optional<NbaPlayer> getPlayer(@PathVariable String id) {
        var player =  nbaPlayerRepository.findById(UUID.fromString(id));
//        if(player.isPresent()) {
//           player.get().setDollarValue(0);
//           nbaPlayerRepository.save(player.get());
//        }

        return player;
    }

//    @GetMapping(value = "/", produces = "application/json")
//    public List<NbaPlayer> getPlayers() {
////        return nbaPlayerRepository.findAllByAgainstTeamAndDollarValueLessThan();
//    }

}




