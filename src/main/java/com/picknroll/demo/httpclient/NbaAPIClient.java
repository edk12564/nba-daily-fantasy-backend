package com.picknroll.demo.httpclient;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "feignClient", url = "https://cdn.nba.com/static/json/liveData")
public interface NbaAPIClient {

    @Cacheable(cacheNames = "livePlayerData", key = "#gameId")
    @GetMapping("/boxscore/boxscore_{gameId}.json")
    String getGamesData(@PathVariable ("gameId") String gameId);

}