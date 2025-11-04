package com.picknroll.demo.controllers;
import com.picknroll.demo.httpclient.NbaAPIClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/livedata")
public class NbaAPIClientController {

    private final NbaAPIClient nbaAPIClient;

    @Autowired
    public NbaAPIClientController(NbaAPIClient nbaAPIClient) {

        this.nbaAPIClient = nbaAPIClient;
    }

    @GetMapping(value = "/test/{gameId}")
    public String testGetPost(@PathVariable String gameId) {
        System.out.println(nbaAPIClient.getGamesData(gameId));
        return "worked";
    }

}
