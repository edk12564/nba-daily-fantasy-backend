package com.bigschlong.demo.controllers;

import com.bigschlong.demo.models.NbaPlayer;
import com.bigschlong.demo.models.PingModel;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/interactions")
public class InteractionsController {

    @PostMapping(value = "/", produces = "application/json")
    public PingModel ping(PingModel pingModel) {
        System.out.println(pingModel);
        if (pingModel.type == 1) {
            return new PingModel(1);
        }
        return null;
    }

}
