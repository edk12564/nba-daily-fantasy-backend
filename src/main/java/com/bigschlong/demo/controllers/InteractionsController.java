package com.bigschlong.demo.controllers;

import com.bigschlong.demo.models.PingModel;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/interactions")
public class InteractionsController {

    @PostMapping(value = "/", produces = "application/json")
    public PingModel ping(@RequestBody PingModel pingModel) {
        System.out.println(pingModel);
        if (pingModel.type == 1) {
            return new PingModel(1);
        }
        return null;
    }

}
