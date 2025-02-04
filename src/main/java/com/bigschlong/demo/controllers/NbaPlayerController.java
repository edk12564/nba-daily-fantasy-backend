package com.bigschlong.demo.controllers;

import com.bigschlong.demo.models.dtos.NbaPlayer;
import com.bigschlong.demo.services.NbaPlayerServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/nba-players")
public class NbaPlayerController {

    @Autowired
    NbaPlayerServices nbaPlayerServices;


    @GetMapping(value = "/todays-players", produces = "application/json")
    public List<NbaPlayer> getPlayers() {
        return nbaPlayerServices.getAllTodaysNbaPlayers();
    }

}




