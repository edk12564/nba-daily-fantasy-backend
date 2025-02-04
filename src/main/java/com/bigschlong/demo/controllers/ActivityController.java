package com.bigschlong.demo.controllers;

import com.bigschlong.demo.models.joinTables.NbaPlayerTeam;
import com.bigschlong.demo.services.NbaPlayerServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/nba-players")
public class ActivityController {

    @Autowired
    NbaPlayerServices nbaPlayerServices;


    @GetMapping(value = "/todays-players", produces = "application/json")
    public List<NbaPlayerTeam> getPlayers() {

        return nbaPlayerServices.getNbaPlayersWithTeam();

    }

}




