package com.bigschlong.demo.services;

import com.bigschlong.demo.models.joinTables.DailyRosterPlayer;
import com.bigschlong.demo.repositories.DailyRosterRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class DailyRosterServices {

    private final DailyRosterRepository dailyRosterRepository;

    public DailyRosterServices(DailyRosterRepository dailyRosterRepository) {
        this.dailyRosterRepository = dailyRosterRepository;
    }

    // String version
    public List<String> getPlayerRosterString(String discordId, String guildId) {
        return dailyRosterRepository.getTodaysRosterByDiscordIdAndGuildId(discordId, guildId).stream()
                .map(dailyRosterPlayer -> dailyRosterPlayer.getName() + " " + dailyRosterPlayer.getDollarValue().toString())
                .toList();
    }

    // Entity version
    public List<DailyRosterPlayer> getPlayerRoster(String discordId, String guildId) {
        return dailyRosterRepository.getTodaysRosterByDiscordIdAndGuildId(discordId, guildId);
    }

    public List<String> getGuildRostersString(String guildId) {
        return dailyRosterRepository.getTodaysRostersByGuildId(guildId).stream()
                .map(dailyRosterPlayer -> dailyRosterPlayer.getNickname() + " chose " + dailyRosterPlayer.getPosition() + " " + dailyRosterPlayer.getName() + " " + dailyRosterPlayer.getDollarValue().toString())
                .toList();
    }

    public List<String> getLeaderboard(String guildId) {
        List<String> leaderboard = dailyRosterRepository.getTodaysRostersByGuildIdWithFantasyScore(guildId).stream()
                .map(player -> player.getNickname() + " chose " + player.getName() + " (" + player.getPosition() + ") - " + player.getFantasyScore().toString())
                .toList();
        leaderboard.add(0, "Leaderboard:");

        return leaderboard;
    }

    public void saveRosterChoice(UUID nbaPlayerUid, String discordPlayerId, String guildId, String nickname, String position) {
        dailyRosterRepository.saveRosterChoice(nbaPlayerUid, discordPlayerId, guildId, nickname, position);
    }

    public List<DailyRosterPlayer> getTodaysPlayersOnRosterByPosition(String discordId, String guildId, String position) {
        return dailyRosterRepository.getTodaysRosterByPosition(discordId, guildId, position);
    }

    public Integer getTodaysRosterPrice(String discordId, String guildId) {
        AtomicInteger result = new AtomicInteger();
        dailyRosterRepository.getTodaysRosterPrice(discordId, guildId).forEach(price -> result.addAndGet(price));
        return result.get();
    }

    public Double getTodaysRosterFantasyScore(String discordId, String guildId) {
        Double result = 0.0;
        dailyRosterRepository.getTodaysRosterFantasyScores(discordId, guildId).forEach(score -> result += score);
        return result;
    }

}
