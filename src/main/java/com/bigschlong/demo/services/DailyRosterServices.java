package com.bigschlong.demo.services;

import com.bigschlong.demo.repositories.DailyRosterRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class DailyRosterServices {

    private final DailyRosterRepository dailyRosterRepository;

    public DailyRosterServices(DailyRosterRepository dailyRosterRepository) {
        this.dailyRosterRepository = dailyRosterRepository;
    }

    public List<String> getPlayerRoster(String discordId) {
        return dailyRosterRepository.getRosterByDiscordId(discordId).stream()
                .map(dailyRosterPlayer -> dailyRosterPlayer.getName() + " " + dailyRosterPlayer.getDollarValue().toString())
                .toList();
    }

    public List<String> getGuildRoster(String guildId) {
        return dailyRosterRepository.getRosterByGuildId(guildId).stream()
                .map(dailyRosterPlayer -> dailyRosterPlayer.getName() + " " + dailyRosterPlayer.getDollarValue().toString())
                .toList();
    }

    public void saveRosterChoice(UUID nbaPlayerUid, String discordPlayerId, String guildId, String nickname, String position) {

        dailyRosterRepository.saveRosterChoice(nbaPlayerUid, discordPlayerId, guildId, nickname, position);

    }

}
