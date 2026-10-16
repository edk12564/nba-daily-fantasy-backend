package com.picknroll.demo.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.picknroll.demo.httpclient.NbaScheduleClient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class NbaScheduleService {

    private final NbaScheduleClient nbaScheduleClient;
    private final ObjectMapper objectMapper;

    public NbaScheduleService(NbaScheduleClient nbaScheduleClient, ObjectMapper objectMapper) {
        this.nbaScheduleClient = nbaScheduleClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Returns YYYY-MM-DD → game IDs from the NBA static schedule feed.
     * Cached for 24h via the {@code nbaSchedule} Caffeine cache.
     */
    @Cacheable(cacheNames = "nbaSchedule")
    public Map<String, List<String>> getGamesByDate() {
        try {
            String raw = nbaScheduleClient.getLeagueSchedule();
            return parseGamesByDate(objectMapper.readTree(raw));
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load NBA schedule", e);
        }
    }

    static Map<String, List<String>> parseGamesByDate(JsonNode root) {
        JsonNode leagueSchedule = root.path("leagueSchedule");
        JsonNode gameDates = leagueSchedule.path("gameDates");
        if (!leagueSchedule.isObject() || !gameDates.isArray()) {
            throw new IllegalStateException("NBA schedule response is missing leagueSchedule.gameDates");
        }

        Map<String, List<String>> result = new LinkedHashMap<>();
        for (JsonNode dateEntry : gameDates) {
            JsonNode games = dateEntry.path("games");
            if (!games.isArray()) {
                throw new IllegalStateException("NBA schedule contains an invalid date entry");
            }
            for (JsonNode game : games) {
                JsonNode gameIdNode = game.get("gameId");
                JsonNode gameDateEst = game.get("gameDateEst");
                if (gameIdNode == null || !gameIdNode.isTextual()
                        || gameDateEst == null || !gameDateEst.isTextual()
                        || gameDateEst.asText().length() < 10) {
                    throw new IllegalStateException("NBA schedule contains an invalid game");
                }
                String date = gameDateEst.asText().substring(0, 10);
                result.computeIfAbsent(date, k -> new ArrayList<>()).add(gameIdNode.asText());
            }
        }
        return result;
    }
}
