package com.picknroll.demo.services;

import com.picknroll.demo.httpclient.NbaAPIClient;
import com.picknroll.demo.utils.Utils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class NbaBoxScoreService {

    private final NbaAPIClient nbaAPIClient;
    private final NbaBoxScoreService self;

    public NbaBoxScoreService(NbaAPIClient nbaAPIClient, @Lazy NbaBoxScoreService self) {
        this.nbaAPIClient = nbaAPIClient;
        this.self = self;
    }

    /**
     * Past Pacific dates use a long TTL (finals rarely change). Today stays on the
     * short live cache so in-progress games keep updating.
     */
    public String getBoxScoreJson(String gameId, LocalDate date) {
        LocalDate day = date != null ? date : Utils.getCaliforniaDate();
        if (day.isBefore(Utils.getCaliforniaDate())) {
            return self.getPastBoxScoreJson(gameId);
        }
        return self.getLiveBoxScoreJson(gameId);
    }

    @Cacheable(cacheNames = "boxScorePast", key = "#gameId")
    public String getPastBoxScoreJson(String gameId) {
        return nbaAPIClient.getGamesData(gameId);
    }

    @Cacheable(cacheNames = "livePlayerData", key = "#gameId")
    public String getLiveBoxScoreJson(String gameId) {
        return nbaAPIClient.getGamesData(gameId);
    }
}
